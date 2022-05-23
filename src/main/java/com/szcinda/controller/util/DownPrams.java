package com.szcinda.controller.util;

import lombok.Data;

import java.io.Serializable;

@Data
public class DownPrams implements Serializable {
    private String fileName;
    private String vehicleNo;


    // 微信字段
    private String message;
    private String account;
    private boolean error;   // 是否需要加入提醒机制， 机制：累计3次都失败则微信提醒
    private String type;// 处置，处理， 位置监控
}
