package com.szcinda.repository;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.Entity;
import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
@Entity
public class PhoneBill extends BaseEntity {
    private String caller; // 主叫号
    private String called; // 被叫号
    private int business; // 业务类型
    private int ttsCount; // 使用tts次数
    private int ttsLength; // 总tts长度
    private int ivrCount; //Ivr播放次数
    private int ivrTime; // Ivr播放总时长
    private int duration; //总通话时长，包含ivr时间
    private double cost; // 总费用
    private String recordFilename; // 录音文件名
    private int recordSize; // 录音大小
    private LocalDateTime callCreateTime; // 通话创建时间
    private LocalDateTime answerTime; // 通话开始时间
    private LocalDateTime releaseTime; // 通话结束时间
    private String dtmf; // 按键码
    private int direction; // 呼叫逻辑呼入呼出方向，0呼入，1呼出
    private int callout; // 呼叫实际呼入呼出方向，0呼入，1呼出
    private String params;
    private String templateId;
    private String status;
    private String message;
    private int callTime;// 外呼次数
    private String fxId;
}
