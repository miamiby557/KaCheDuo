package com.szcinda.service.driver;

import lombok.Data;

import java.io.Serializable;

@Data
public class DriverUpdateDto implements Serializable {
    private String id;
    private String name;
    private String phone;
    private String vehicleNo;
    private String company;
    private String wechat;
    private String wxid;
    private String owner;
    private String ownerWechat;// 来源微信号，就是哪个微信下的好友
}
