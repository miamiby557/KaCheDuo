package com.szcinda.controller.util;

import lombok.Data;

import java.io.Serializable;

@Data
public class DownPrams implements Serializable {
    private String fileName;
    private String vehicleNo;


    // 微信字段
    private String message;
}
