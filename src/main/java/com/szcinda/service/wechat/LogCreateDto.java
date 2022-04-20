package com.szcinda.service.wechat;

import lombok.Data;

import java.io.Serializable;

@Data
public class LogCreateDto implements Serializable {
    private String wxid; // 发送人的微信ID
    private String content; // 发送内容
    private String selfWxid;// 接收消息的微信
}
