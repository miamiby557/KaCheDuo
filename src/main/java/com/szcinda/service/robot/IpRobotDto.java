package com.szcinda.service.robot;

import lombok.Data;

import java.io.Serializable;

@Data
public class IpRobotDto implements Serializable {
    private String ip;
    private String account;

    private String id;
    private String phone;
}
