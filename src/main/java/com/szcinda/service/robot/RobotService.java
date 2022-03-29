package com.szcinda.service.robot;

import java.util.List;

public interface RobotService {
    void create(CreateRobotDto dto);

    void update(UpdateRobotDto dto);

    List<RobotDto> query(String owner);

    List<RobotGroupDto> querySelf(String owner);

    void delete(String id);

    void stop(String id);

    void start(String id);

    List<RobotGroupDto> group();

    List<RobotDto> find10Account();

    List<String> getLocationRobots(String owner);
}
