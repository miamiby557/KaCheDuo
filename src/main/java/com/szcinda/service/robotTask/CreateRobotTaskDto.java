package com.szcinda.service.robotTask;

import lombok.Data;

import java.io.Serializable;

@Data
public class CreateRobotTaskDto implements Serializable {
    private String company;
    private String userName;
    private String pwd;
    private String taskType;  // 处置、处理、位置监控
//    private String taskStatus; // 待运行、运行中、已完成、运行失败
}
