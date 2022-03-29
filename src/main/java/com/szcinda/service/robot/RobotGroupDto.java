package com.szcinda.service.robot;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class RobotGroupDto implements Serializable {
    private String company;
    private String owner;

    private List<RobotDto> subRobots = new ArrayList<>();
}
