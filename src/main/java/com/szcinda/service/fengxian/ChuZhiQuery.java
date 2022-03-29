package com.szcinda.service.fengxian;

import com.szcinda.service.PageParams;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ChuZhiQuery extends PageParams {
    private String vehicleNo;//车牌号
    private String happenTime;// 发生时间
    private String owner;
}
