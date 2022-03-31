package com.szcinda.service.driver;

import lombok.Data;

import java.io.Serializable;

@Data
public class DriverConnectDto implements Serializable {
    private String vehicleNo;
    private String wechat;
    private String ownerWechat;
}
