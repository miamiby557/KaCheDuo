package com.szcinda.service.robot;

import com.szcinda.controller.util.CarCountDto;
import com.szcinda.service.PageResult;

import java.util.List;

public interface RobotService {
    void create(CreateRobotDto dto);

    void update(UpdateRobotDto dto);

    PageResult<RobotDto> query(QueryRobotParams params);

    List<RobotGroupDto> querySelf(String owner);

    void delete(String id);

    void stop(String id);

    void start(String id);

    List<RobotGroupDto> group();

    List<RobotDto> find10Account();

    List<String> getLocationRobots(String owner);

    void startLocation(String id);

    void stopLocation(String id);

    void batchRunOnceLocation();

    void runOnceLocation(String id);

    void  updateCarCount(CarCountDto carCountDto);
}
