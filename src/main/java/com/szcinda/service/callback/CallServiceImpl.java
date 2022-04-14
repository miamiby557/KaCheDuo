package com.szcinda.service.callback;

import com.szcinda.controller.Result;
import com.szcinda.repository.*;
import com.szcinda.service.SnowFlakeFactory;
import com.szcinda.service.TypeStringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.*;
import java.util.Arrays;
import java.util.Collections;

@Service
@Transactional
public class CallServiceImpl implements CallService {

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

    @Override
    public void call(CallParams params) {
        PhoneBill phoneBill = new PhoneBill();
        phoneBill.setId(snowFlakeFactory.nextId("PB"));
        // 与处置记录关联
        phoneBill.setFxId(params.getFxId());
        params.setDataId(phoneBill.getId());
        Result<String> result = VoiceApi.sendVoiceNotification(params);
        phoneBill.setCalled(params.getPhone());
        if (params.getParams() != null) {
            phoneBill.setParams(String.join(",", params.getParams()));
        }
        phoneBill.setTemplateId(params.getTemplateId());
        if (result.isSuccess()) {
            phoneBill.setStatus(TypeStringUtils.phone_status2);
        } else {
            phoneBill.setStatus(TypeStringUtils.phone_status1);
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
            fengXian.setCallTime(phoneBill.getCallCreateTime().toString().replaceAll("T", " "));
            fengXian.setCalled(phoneBill.getStatus());
            fengXian.setHangUpTime(phoneBill.getAnswerTime().toString().replaceAll("T", " "));
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
