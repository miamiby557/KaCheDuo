package com.szcinda.service.callback;

import com.szcinda.controller.Result;
import com.szcinda.repository.*;
import com.szcinda.service.SnowFlakeFactory;
import com.szcinda.service.TypeStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Transactional
public class CallServiceImpl implements CallService {

    private final static Logger logger = LoggerFactory.getLogger(CallService.class);

    //记录上次拨打的时间，在30分钟内不打
    private static final ConcurrentHashMap<String, LocalDateTime> callMap = new ConcurrentHashMap<>();

    private final PhoneBillRepository phoneBillRepository;
    private final SnowFlakeFactory snowFlakeFactory;
    private final DriverRepository driverRepository;
    private final RobotRepository robotRepository;
    private final FengXianRepository fengXianRepository;

    public CallServiceImpl(PhoneBillRepository phoneBillRepository, DriverRepository driverRepository,
                           RobotRepository robotRepository, FengXianRepository fengXianRepository) {
        this.phoneBillRepository = phoneBillRepository;
        this.driverRepository = driverRepository;
        this.robotRepository = robotRepository;
        this.fengXianRepository = fengXianRepository;
        this.snowFlakeFactory = SnowFlakeFactory.getInstance();
    }

    @Value("${charge.msg.id}")
    private String chargeMsgId;


    // 每天凌晨1点清空一下，避免内存过大
    @Scheduled(cron = "0 0 1 * * ?")
    public void deleteCallMap() {
        try {
            callMap.clear();
        } catch (Exception ignored) {

        }
    }

    @Override
    public void call(CallParams params) {
        PhoneBill phoneBill = new PhoneBill();
        phoneBill.setId(snowFlakeFactory.nextId("PB"));
        // 与处置记录关联
        phoneBill.setFxId(params.getFxId());

        phoneBill.setVehicleNo(params.getVehicleNo());
        phoneBill.setAccount(params.getAccount());
        phoneBill.setCompany(params.getCompany());

        params.setDataId(phoneBill.getId());
        logger.info("判断当前手机号上次拨打时间是否超过30分钟");
        boolean canCall = true;
        LocalDateTime now = LocalDateTime.now();
        if (callMap.containsKey(params.getPhone())) {
            logger.info("上次拨打时间：" + callMap.get(params.getPhone()));
            LocalDateTime lastCall = callMap.get(params.getPhone());
            Duration duration = Duration.between(now, lastCall);
            long minutes = Math.abs(duration.toMinutes());//相差的分钟数
            if (minutes <= 30) {
                canCall = false;
            }
        } else {
            logger.info("没有上次拨打记录，可以拨打电话");
        }
        if (!canCall) {
            logger.info("30分钟内不允许拨打电话");
            phoneBill.setMessage("30分钟内不允许拨打电话");
        } else {
            logger.info(String.format("正在外呼：【%s】", params.toString()));
            Result<String> result = VoiceApi.sendVoiceNotification(params);
            logger.info(String.format("外呼返回结果：【%s】", result.toString()));
            phoneBill.setCalled(params.getPhone());
            if (params.getParams() != null) {
                phoneBill.setParams(String.join(",", params.getParams()));
            }
            phoneBill.setTemplateId(params.getTemplateId());
            if (result.isSuccess()) {
                phoneBill.setStatus(TypeStringUtils.phone_status2);
                phoneBill.setMessage("成功调用外呼接口");
                // 记录当前拨打时间
                callMap.put(params.getPhone(), LocalDateTime.now());
            } else {
                phoneBill.setStatus(TypeStringUtils.phone_status1);
                phoneBill.setMessage(result.getMessage());
            }
        }
        phoneBillRepository.save(phoneBill);
    }

    @Override
    public void create(CallbackData callbackData) {
        PhoneBill phoneBill = phoneBillRepository.findOne(callbackData.getData());
        if (phoneBill == null) {
            return;
        }
        phoneBill.setCallTime(phoneBill.getCallTime() + 1);
        phoneBill.setCaller(callbackData.getSubject().getCaller());
        phoneBill.setBusiness(callbackData.getSubject().getBusiness());
        phoneBill.setTtsCount(callbackData.getSubject().getTtsCount());
        phoneBill.setTtsLength(callbackData.getSubject().getTtsLength());
        phoneBill.setIvrCount(callbackData.getSubject().getIvrCount());
        phoneBill.setIvrTime(callbackData.getSubject().getIvrTime());
        phoneBill.setCost(callbackData.getSubject().getCost());
        phoneBill.setRecordFilename(callbackData.getSubject().getRecordFilename());
        phoneBill.setRecordSize(callbackData.getSubject().getRecordSize());
        phoneBill.setDtmf(callbackData.getSubject().getDtmf());
        phoneBill.setDirection(callbackData.getSubject().getDirection());
        phoneBill.setDuration(callbackData.getSubject().getDuration());
        phoneBill.setCallout(callbackData.getSubject().getCallout());
        if (StringUtils.hasText(callbackData.getSubject().getCreateTime())) {
            phoneBill.setCallCreateTime(LocalDateTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(callbackData.getSubject().getCreateTime())), ZoneId.of("+8")));
        }
        if (StringUtils.hasText(callbackData.getSubject().getAnswerTime())) {
            phoneBill.setAnswerTime(LocalDateTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(callbackData.getSubject().getAnswerTime())), ZoneId.of("+8")));
        }
        if (StringUtils.hasText(callbackData.getSubject().getReleaseTime())) {
            phoneBill.setReleaseTime(LocalDateTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(callbackData.getSubject().getReleaseTime())), ZoneId.of("+8")));
        }
        // 如果总通话时长，包含ivr时间（duration）和 Ivr播放总时长（ivrTime）播放时间都是0，代表没有拨通
        if (callbackData.getSubject().getDuration() == 0 && callbackData.getSubject().getIvrTime() == 0) {
            phoneBill.setStatus(TypeStringUtils.phone_status1);
        } else if (callbackData.getSubject().getDuration() > 0 && callbackData.getSubject().getIvrTime() == 0) {
            phoneBill.setStatus(TypeStringUtils.phone_status3);
            // 判断未接通，再拨打一次
            if (phoneBill.getCallTime() < 2) {
                CallParams params = new CallParams();
                params.setTemplateId(phoneBill.getTemplateId());
                if (phoneBill.getParams() != null) {
                    params.setParams(Arrays.asList(phoneBill.getParams().split(",")));
                }
                params.setPhone(phoneBill.getCalled());
                this.call(params);
            } else if (phoneBill.getCallTime() >= 2 && !chargeMsgId.equals(phoneBill.getTemplateId())) {
                // 已经打了2次电话，并且不是打给负责人，则需要打给负责人
                // 通过司机的公司名找到登录账号的负责人电话
                Driver driver = driverRepository.findByPhone(phoneBill.getCalled());
                if (driver != null) {
                    String company = driver.getCompany();
                    Robot robot = robotRepository.findByCompanyAndParentIdIsNull(company);
                    if (robot != null && StringUtils.hasText(robot.getChargePhone())) {
                        CallParams params = new CallParams();
                        params.setTemplateId(chargeMsgId);
                        params.setPhone(robot.getChargePhone());
                        params.setParams(Collections.singletonList(driver.getVehicleNo()));
                        this.call(params);
                    }
                }
            }
        } else if (callbackData.getSubject().getDuration() > 0 && callbackData.getSubject().getIvrTime() > 0) {
            phoneBill.setStatus(TypeStringUtils.phone_status4);
        }
        phoneBillRepository.save(phoneBill);
        // 更新处置的通话记录
        FengXian fengXian = fengXianRepository.findOne(phoneBill.getFxId());
        if (fengXian != null) {
            if (phoneBill.getCallCreateTime() != null) {
                fengXian.setCallTime(phoneBill.getCallCreateTime().toString().replaceAll("T", " "));
            }
            fengXian.setCalled(phoneBill.getStatus());
            if (phoneBill.getAnswerTime() != null) {
                fengXian.setHangUpTime(phoneBill.getAnswerTime().toString().replaceAll("T", " "));
            }
            fengXian.setSeconds(phoneBill.getIvrTime());
            fengXianRepository.save(fengXian);
        }
    }

    public static void main(String[] args) {
        String text = "1649415711206";
        Long aLong = Long.parseLong(text);
        ZonedDateTime zonedDateTime = Instant.ofEpochMilli(aLong).atZone(ZoneOffset.ofHours(8));
        LocalDateTime localDateTime = zonedDateTime.toLocalDateTime();
        System.out.println(localDateTime.toString());
    }
}
