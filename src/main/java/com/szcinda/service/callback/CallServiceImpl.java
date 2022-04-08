package com.szcinda.service.callback;

import com.szcinda.controller.Result;
import com.szcinda.repository.PhoneBill;
import com.szcinda.repository.PhoneBillRepository;
import com.szcinda.service.SnowFlakeFactory;
import com.szcinda.service.TypeStringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@Transactional
public class CallServiceImpl implements CallService {

    private final PhoneBillRepository phoneBillRepository;
    private final SnowFlakeFactory snowFlakeFactory;

    public CallServiceImpl(PhoneBillRepository phoneBillRepository) {
        this.phoneBillRepository = phoneBillRepository;
        this.snowFlakeFactory = SnowFlakeFactory.getInstance();
    }

    @Override
    public void call(CallParams params) {
        PhoneBill phoneBill = new PhoneBill();
        phoneBill.setId(snowFlakeFactory.nextId("PB"));
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
        BeanUtils.copyProperties(callbackData.getSubject(), phoneBill, "id", "caller");
        if (StringUtils.hasText(callbackData.getSubject().getCreateTime())) {
            phoneBill.setCallCreateTime(LocalDateTime.ofInstant(Instant.ofEpochMilli(Long.getLong(callbackData.getSubject().getCreateTime())), ZoneId.of("+8")));
        }
        if (StringUtils.hasText(callbackData.getSubject().getAnswerTime())) {
            phoneBill.setAnswerTime(LocalDateTime.ofInstant(Instant.ofEpochMilli(Long.getLong(callbackData.getSubject().getAnswerTime())), ZoneId.of("+8")));
        }
        if (StringUtils.hasText(callbackData.getSubject().getReleaseTime())) {
            phoneBill.setReleaseTime(LocalDateTime.ofInstant(Instant.ofEpochMilli(Long.getLong(callbackData.getSubject().getReleaseTime())), ZoneId.of("+8")));
        }
        // 如果总通话时长，包含ivr时间（duration）和 Ivr播放总时长（ivrTime）播放时间都是0，代表没有拨通
        if (callbackData.getSubject().getDirection() == 0 && callbackData.getSubject().getIvrTime() == 0) {
            phoneBill.setStatus(TypeStringUtils.phone_status1);
        } else if (callbackData.getSubject().getDirection() > 0 && callbackData.getSubject().getIvrTime() == 0) {
            phoneBill.setStatus(TypeStringUtils.phone_status3);
        } else if (callbackData.getSubject().getDirection() > 0 && callbackData.getSubject().getIvrTime() > 0) {
            phoneBill.setStatus(TypeStringUtils.phone_status4);
        }
        phoneBillRepository.save(phoneBill);
    }
}
