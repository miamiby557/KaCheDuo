package com.szcinda.service;

import lombok.Data;

@Data
public class TypeStringUtils {
    // 机器人帐号类型
    public static final String robotType1 = "监控";
    public static final String robotType2 = "处置";
    public static final String robotType3 = "处理-位置监控";

    // 任务状态
    public static final String taskStatus1 = "待运行";
    public static final String taskStatus2 = "运行中";
    public static final String taskStatus3 = "已完成";
    public static final String taskStatus4 = "运行失败";

    // 风险处理状态
    public static final String fxHandleStatus1 = "待处理";
    public static final String fxHandleStatus2 = "已处理";
    public static final String fxHandleStatus3 = "处理失败";

    // 报警类型
    public static final String tired_status = "生理疲劳报警";
    public static final String over_status = "超速报警";
    // 微信截图状态
    public static final String screen_status1 = "已截图";
    public static final String screen_status2 = "截图失败";

    public static final String wechat_status1 = "待回复";
    public static final String wechat_status2 = "已回复";
    public static final String wechat_status3 = "待发送";


    public static String getWechatContent(String type) {
        switch (type) {
            case tired_status:
                return "卡车多物流科技提醒您，请勿疲劳驾驶，请注意行驶安全，杜绝此类行为再次发生。请您收到回复”确认“！";
            case over_status:
                return "卡车多物流科技提醒您，请勿超速，请注意行驶安全，杜绝此类行为再次发生。请您收到回复”确认“！";
            default:
                return "卡车多物流科技提醒您，请您停止非规范驾驶行为，请注意行车安全，杜绝此类行为再次发生。请您收到回复”确认“！";
        }
    }
}
