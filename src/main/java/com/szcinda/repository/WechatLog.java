package com.szcinda.repository;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.Entity;

@EqualsAndHashCode(callSuper = true)
@Data
@Entity
public class WechatLog extends BaseEntity {
    private String wxid; // 发送人的微信ID
    private String wechat; // 发送人的微信号
    private String content; // 发送内容
    private String selfWxid;// 接收消息的微信
}
