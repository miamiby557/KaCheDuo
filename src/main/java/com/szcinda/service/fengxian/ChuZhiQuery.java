package com.szcinda.service.fengxian;

import com.szcinda.service.PageParams;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ChuZhiQuery extends PageParams {
    private String vehicleNo;//车牌号
    private String happenTime;// 发生时间
    private String userName;// 属于那个账号的风险处置
    private String owner;
    private String company;
}
