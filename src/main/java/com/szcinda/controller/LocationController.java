package com.szcinda.controller;

import com.szcinda.controller.util.CarCountDto;
import com.szcinda.service.PageResult;
import com.szcinda.service.ScheduleService;
import com.szcinda.service.location.CreateLocationDto;
import com.szcinda.service.location.LocationDto;
import com.szcinda.service.location.LocationQuery;
import com.szcinda.service.location.LocationService;
import com.szcinda.service.robot.RobotService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("location")
public class LocationController {

    private final LocationService locationService;
    private final RobotService robotService;

    private final ScheduleService scheduleService;

    public LocationController(LocationService locationService, RobotService robotService, ScheduleService scheduleService) {
        this.locationService = locationService;
        this.robotService = robotService;
        this.scheduleService = scheduleService;
    }

    @PostMapping("apiCreate")
    public Result<String> apiCreate(@RequestBody List<CreateLocationDto> dtos) {
        locationService.batchCreate(dtos);
        return Result.success();
    }

    @PostMapping("updateCarCount")
    public Result<String> updateCarCount(@RequestBody CarCountDto carCountDto) {
        robotService.updateCarCount(carCountDto);
        return Result.success();
    }

    @PostMapping("query")
    public PageResult<LocationDto> query(@RequestBody LocationQuery query) {
        return locationService.query(query);
    }

    @GetMapping("canRun/{phone}")
    public Result<String> canRun(@PathVariable String phone) {
        boolean canRun = scheduleService.isInList(phone);
        if (canRun) {
            return Result.success();
        }
        return Result.fail("机器人不在列表中，不需要运行位置监控流程");
    }

    @GetMapping("runOnce")
    public Result<String> runOnce() {
        try {
            scheduleService.run();
        } catch (Exception ignored) {
        }
        return Result.success();
    }

    @GetMapping("runLastDateChuZhi")
    public Result<String> runLastDateChuZhi() {
        try {
            scheduleService.sendMsgToDriver();
        } catch (Exception ignored) {
        }
        return Result.success();
    }
}
