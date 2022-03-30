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
}
