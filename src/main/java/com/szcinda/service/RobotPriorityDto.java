package com.szcinda.service;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class RobotPriorityDto implements Serializable{
    private String phone;
    private int priority;
    private LocalDateTime lastTime;

    public RobotPriorityDto(String phone) {
        this.priority = 99;
        this.phone = phone;
    }
}
