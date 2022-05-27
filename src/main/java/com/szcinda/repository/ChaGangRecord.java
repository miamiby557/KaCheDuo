package com.szcinda.repository;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.Entity;
import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
@Entity
public class ChaGangRecord extends BaseEntity {
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
