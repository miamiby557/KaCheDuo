package com.szcinda.repository;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.Entity;
import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
@Entity
public class Location extends BaseEntity{
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
