package com.szcinda.service.wechat;

import com.szcinda.service.PageParams;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@EqualsAndHashCode(callSuper = true)
@Data
public class WechatLogQueryDto extends PageParams {
    private String wxid; // 发送人的微信ID
    private String wechat; // 发送人的微信ID
    private String selfWxid;// 接收消息的微信
    private LocalDate createTimeStart;
    private LocalDate createTimeEnd;
    private String content;
}
