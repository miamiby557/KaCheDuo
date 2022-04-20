package com.szcinda.repository;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.Entity;

@EqualsAndHashCode(callSuper = true)
@Data
@Entity
public class Driver extends BaseEntity {
    private String name;
    private String phone;
    private String vehicleNo;
    private String company;
    private String wechat; // 微信号
    private String wxid; // 微信ID
    private String ownerWechat;// 来源微信号，就是哪个微信下的好友
    private String owner;
    private boolean friend;
}
