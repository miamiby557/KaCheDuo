package com.szcinda.repository;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.Entity;

@EqualsAndHashCode(callSuper = true)
@Data
@Entity
public class HistoryScreenShotTask extends BaseEntity{
    private String fxId;
    private String wechat;
    private String wxid;
    private String vehicleNo;
    private String ownerWechat;
    private String type;
    private String content;
    private String message;
    private String owner;// 所属账户
}
