package com.szcinda.service.location;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class LocationDto implements Serializable {
    private String id;
    private String vehicleNo;
    private String vehicleColor;
    private String sim;
    private String currentDriver;
    private String company;
    private String businessScope;
    private String speed;
    private String happenPlace;
    private LocalDateTime happenTime;
    private String owner;
    private LocalDateTime createTime;
    private String userCompany;
}
