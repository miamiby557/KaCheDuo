package com.szcinda.service.fengxian;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class CreateFengXianDto implements Serializable {
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

    private String chuLiType;
}
