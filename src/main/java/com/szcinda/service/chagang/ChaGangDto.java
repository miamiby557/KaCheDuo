package com.szcinda.service.chagang;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class ChaGangDto implements Serializable {
    private String id;
    private String company;
    private String account;
    private String pwd;

    private LocalDateTime lastTime;
    private boolean alive;
}
