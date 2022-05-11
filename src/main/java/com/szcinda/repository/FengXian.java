package com.szcinda.repository;

import com.szcinda.service.TypeStringUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.Entity;
import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
@Entity
public class FengXian extends BaseEntity{
    public String vehicleNo;//车牌号
    public String vehicleColor;//车牌颜色
    public String area;// 所属地区
    public String thirdOrg;//第三方监控机构
    public String currentDriver;//当前驾驶员
    public String businessScope;// 经营范围
    public String dangerType;// 风险类型
    public String dangerLevel;// 风险等级
    public String speed;// 行车速度km/h
    public String happenTime;// 发生时间
    public String happenPlace;//发生位置
    public LocalDateTime gdCreateTime;// 两客创建时间
    public String owner;// 所属账户
    public String company;
    //
    public LocalDateTime disposeTime;// 处置时间
    public String messageSendTime;// 微信通知时间
    public String messageReceiveTime;// 微信回复时间
    public String chuLiType = TypeStringUtils.fxHandleStatus1; // 待处理、处理完成、处理失败
    public LocalDateTime chuLiTime;// 处理时间
    public String filePath;//处理图片相对路径
    public String callTime;//外呼时间
    public String called;//是否接通   是、否
    public String phone;
    public String hangUpTime;//接通时间
    public int seconds;//接通时长
    public double cost;// 接通成本

}
