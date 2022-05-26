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

    // 处置
    public static final String chuZhiStatus1 = "待处置";
    public static final String chuZhiStatus2 = "已处置";

    // 报警类型
    public static final String tired_status = "生理疲劳报警";
    public static final String over_status = "超速报警";
    public static final String smoke_status = "抽烟报警";
    public static final String hangup_phone_status = "接打手机报警";
    public static final String play_phone_status = "玩手机报警";
    public static final String untied_status = "未系安全带报警";
    public static final String over_hand_status = "双脱把报警";

    public static boolean canSendToApp(String type){
        return tired_status.equals(type) || over_status.equals(type) || smoke_status.equals(type) || hangup_phone_status.equals(type)
                || play_phone_status.equals(type) || untied_status.equals(type) || over_hand_status.equals(type);
    }

    // 微信截图状态
    public static final String screen_status1 = "已截图";
    public static final String screen_status2 = "截图失败";
    public static final String screen_status3 = "告警失败";
    public static final String screen_status4 = "告警成功";

    public static final String wechat_status1 = "待回复";
    public static final String wechat_status2 = "已回复";
    public static final String wechat_status3 = "待发送";
    public static final String wechat_status4 = "超时未回复";
    public static final String wechat_status5 = "告警";


    public static final String phone_status1 = "未拨打";
    public static final String phone_status2 = "已拨打";
    public static final String phone_status3 = "未接通";
    public static final String phone_status4 = "已接通";


    public static final String messageStop = "通知司机停车休息";
    public static final String messageSlow = "通知司机降低车速";
    public static final String messageSafe = "通知司机注意安全驾驶";


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
