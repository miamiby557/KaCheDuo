package com.szcinda.service.robotTask;

import com.szcinda.service.PageParams;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@EqualsAndHashCode(callSuper = true)
@Data
public class RobotTaskQuery extends PageParams {
    private String userName;
    private String taskType;  // 处置、处理、位置监控
    private String taskStatus; // 待运行、运行中、已完成、运行失败
    private LocalDate createTimeStart;
    private LocalDate createTimeEnd;
    private String owner;
    private String vehicleNo;


    private boolean queryRunning; // 是否查询正在待运行和运行的
}
