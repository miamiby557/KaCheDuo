package com.szcinda.repository;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.Entity;
import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
@Entity
public class RobotTask  extends BaseEntity{
    private String userName;
    private String company;
    private String pwd;
    private String taskType;  // 处置、处理、位置监控
    private String taskStatus; // 待运行、运行中、已完成、运行失败
    private String message;
    private LocalDateTime finishTime;
    private String fxId;
    private String vehicleNo;
    private String happenTime;
    private String filePath;
}
