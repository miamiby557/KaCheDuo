package com.szcinda.service.report;

import lombok.Data;

import java.io.Serializable;

@Data
public class ReportDto implements Serializable {
    private String vehicleNo; // 车牌号
    private String vehicleType; // 车型
    private String checkTime; // 抽查时间
    private String deviceStatus = "正常"; // 设备是否正常
    private String speed;// 车速
    private String message;// 报警内容
    private String location;// 车辆所在位置
    private String handleResult;// 处理结果
    private String signName;// 检查人签名
}
