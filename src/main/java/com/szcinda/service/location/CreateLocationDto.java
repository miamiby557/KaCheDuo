package com.szcinda.service.location;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class CreateLocationDto implements Serializable {
    private String vehicleNo;
    private String vehicleColor;
    private String sim;
    private String currentDriver;
    private String company;
    private String businessScope;
    private String speed;
    private String happenPlace;
    private LocalDateTime happenTime;
    private String owner;// 所属账户
    private String userCompany;
}
