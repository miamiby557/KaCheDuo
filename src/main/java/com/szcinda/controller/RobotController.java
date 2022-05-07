package com.szcinda.controller;

import com.szcinda.service.PageResult;
import com.szcinda.service.ScheduleService;
import com.szcinda.service.robot.*;
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


    @PostMapping("query")
    public PageResult<RobotDto> query(@RequestBody QueryRobotParams params) {
        return robotService.query(params);
    }


    @GetMapping("alive/{id}/{phone}")
    public Result<String> alive(@PathVariable String id, @PathVariable String phone) {
        scheduleService.alive(id, phone);
        return Result.success();
    }


    @GetMapping("aliveIp/{id}/{phone}/{ip}")
    public Result<String> aliveIp(@PathVariable String id, @PathVariable String phone, @PathVariable String ip) {
        scheduleService.alive(id, phone);
        scheduleService.aliveIp(ip, phone);
        return Result.success();
    }

    @PostMapping("checkCanReboot")
    public Result<String> checkCanReboot(@RequestBody IpRobotDto ip) {
        boolean needReboot = scheduleService.needReboot(ip.getIp());
        if (needReboot) {
            return Result.success();
        }
        return Result.fail("不需要重启");
    }

    @PostMapping("rebootSuccess")
    public Result<String> rebootSuccess(@PathVariable IpRobotDto ip) {
        scheduleService.rebootSuccess(ip.getIp());
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

    @GetMapping("startLocation/{id}")
    public Result<String> startLocation(@PathVariable String id) {
        robotService.startLocation(id);
        return Result.success();
    }

    @GetMapping("stopLocation/{id}")
    public Result<String> stopLocation(@PathVariable String id) {
        robotService.stopLocation(id);
        return Result.success();
    }

    @GetMapping("batchRunOnceLocation")
    public Result<String> batchRunOnceLocation() {
        robotService.batchRunOnceLocation();
        return Result.success();
    }

    // 获取10个可以运行监控的主账号
    @GetMapping("getMainAccount")
    public Result<List<RobotDto>> getMainAccount() {
        return Result.success(robotService.find10Account());
    }

    @GetMapping("getLocationRobots/{owner}")
    public Result<List<String>> getLocationRobots(@PathVariable String owner) {
        return Result.success(robotService.getLocationRobots(owner));
    }
}
