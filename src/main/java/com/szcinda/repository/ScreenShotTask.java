package com.szcinda.repository;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.Entity;

@EqualsAndHashCode(callSuper = true)
@Data
@Entity
public class ScreenShotTask extends BaseEntity{
    private String fxId;
    private String wechat;
    private String wxid;
    private String ownerWechat;
    private String vehicleNo;
    private String content;
    private String status; // 待回复
    private String owner;// 所属账户
}
