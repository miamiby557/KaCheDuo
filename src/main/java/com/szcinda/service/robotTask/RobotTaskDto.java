package com.szcinda.service.robotTask;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class RobotTaskDto implements Serializable {
    private String id;
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

    private List<RobotTaskDto> subTasks = new ArrayList<>();
}
