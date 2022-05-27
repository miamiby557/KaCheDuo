package com.szcinda.service.chagang;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class ChaGangRecordCreateDto implements Serializable {
    private String mangerName;
    private String ownerName;
    private String stationType;
    private String inquireUser;
    private String answerUser;
    private String inquireQuestion;
    private String answerContent;
    private LocalDateTime inquireTime;
    private String answerDuration;
    private LocalDateTime answerTime;
}
