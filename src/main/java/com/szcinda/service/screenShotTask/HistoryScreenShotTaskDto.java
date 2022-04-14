package com.szcinda.service.screenShotTask;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class HistoryScreenShotTaskDto implements Serializable {
    private String id;
    private String fxId;
    private String wechat;
    private String wxid;
    private String vehicleNo;
    private String ownerWechat;
    private String type;
    private String content;
    private String message;
    private String owner;// 所属账户
    private String fileBase64;
    private String filePath;
    private LocalDateTime createTime;
}
