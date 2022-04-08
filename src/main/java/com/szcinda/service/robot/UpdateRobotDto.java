package com.szcinda.service.robot;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class UpdateRobotDto implements Serializable {
    private String id;
    private String company;
    private String email;
    private String phone;
    private String pwd;
    private String account2;//处理、位置监控 用到此账号
    private String pwd2;
    private String owner;
    private boolean run;
    private String parentId;

    List<UpdateRobotDto> subRobotList = new ArrayList<>();
}
