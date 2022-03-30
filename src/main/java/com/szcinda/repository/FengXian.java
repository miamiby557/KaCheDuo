package com.szcinda.repository;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.Entity;
import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
@Entity
public class FengXian extends BaseEntity{
    private String vehicleNo;//车牌号
    private String vehicleColor;//车牌颜色
    private String area;// 所属地区
    private String thirdOrg;//第三方监控机构
    private String currentDriver;//当前驾驶员
    private String businessScope;// 经营范围
    private String dangerType;// 风险类型
    private String dangerLevel;// 风险等级
    private String speed;// 行车速度km/h
    private String happenTime;// 发生时间
    private String happenPlace;//发生位置
    private LocalDateTime gdCreateTime;// 两客创建时间
    private String owner;// 所属账户
    private String company;
    //
    private LocalDateTime disposeTime;// 处置时间
    private String messageSendTime;// 微信通知时间
    private String messageReceiveTime;// 微信回复时间
    private String chuLiType; // 待处理、处理完成、处理失败
    private LocalDateTime chuLiTime;// 处理时间
    private String filePath;//处理图片相对路径
    private String callTime;//外呼时间
    private String called;//是否接通   是、否
    private String hangUpTime;//接通时间
    private int seconds;//接通时长
    private double cost;// 接通成本

}
