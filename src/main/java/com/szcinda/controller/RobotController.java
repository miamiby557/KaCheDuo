package com.szcinda.controller;

import com.szcinda.service.ScheduleService;
import com.szcinda.service.robot.CreateRobotDto;
import com.szcinda.service.robot.RobotDto;
import com.szcinda.service.robot.RobotService;
import com.szcinda.service.robot.UpdateRobotDto;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("robot")
public class RobotController {

    private final ScheduleService scheduleService;
    private final RobotService robotService;

    public RobotController(ScheduleService scheduleService, RobotService robotService) {
        this.scheduleService = scheduleService;
        this.robotService = robotService;
    }


    @GetMapping("query/{owner}")
    public Result<List<RobotDto>> query(@PathVariable String owner) {
        return Result.success(robotService.query(owner));
    }

    @GetMapping("alive/{id}/{phone}")
    public Result<String> alive(@PathVariable String id, @PathVariable String phone) {
        scheduleService.alive(id, phone);
        return Result.success();
    }

    @PostMapping("create")
    public Result<String> create(@RequestBody CreateRobotDto robotDto) {
        robotService.create(robotDto);
        return Result.success();
    }

    @PostMapping("update")
    public Result<String> update(@RequestBody UpdateRobotDto robotDto) {
        robotService.update(robotDto);
        return Result.success();
    }

    @GetMapping("del/{id}")
    public Result<String> delete(@PathVariable String id) {
        robotService.delete(id);
        return Result.success();
    }

    @GetMapping("start/{id}")
    public Result<String> start(@PathVariable String id) {
        robotService.start(id);
        return Result.success();
    }

    @GetMapping("stop/{id}")
    public Result<String> stop(@PathVariable String id) {
        robotService.stop(id);
        return Result.success();
    }

    // 获取10个可以运行监控的主账号
    @GetMapping("getMainAccount")
    public Result<List<RobotDto>> getMainAccount() {
        return Result.success(robotService.find10Account());
    }

    @GetMapping("getLocationRobots/{owner}")
    public Result<List<String>> getLocationRobots(@PathVariable String owner){
        return Result.success(robotService.getLocationRobots(owner));
    }
}
