package com.szcinda.service.fengxian;

import lombok.Data;

import java.io.Serializable;

@Data
public class ChuZhiDto implements Serializable {
    private String vehicleNo;//车牌号
    private String happenTime;// 发生时间
}
