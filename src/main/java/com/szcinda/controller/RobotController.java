package com.szcinda.controller;

import com.szcinda.service.PageResult;
import com.szcinda.service.ScheduleService;
import com.szcinda.service.robot.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("robot")
public class RobotController {

    private final static Logger logger = LoggerFactory.getLogger(RobotController.class);

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
        logger.info(String.format("机器人发送心跳包：%s,%s", id, phone));
        scheduleService.alive(id, phone);
        return Result.success();
    }


    @PostMapping("aliveIp")
    public Result<String> aliveIp(@RequestBody IpRobotDto ipRobotDto) {
        logger.info(String.format("机器人发送心跳包：%s,%s,%s", ipRobotDto.getId(), ipRobotDto.getIp(), ipRobotDto.getAccount()));
        scheduleService.alive(ipRobotDto.getId(), ipRobotDto.getAccount());
        scheduleService.aliveIp(ipRobotDto.getIp(), ipRobotDto.getAccount());
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

    // 获取需要重启的IP列表
    @GetMapping("getNeedRebootList")
    public Result<Set<String>> getNeedRebootList() {
        return Result.success(ScheduleService.ipList);
    }

    // 手动重启
    @PostMapping("manualReboot")
    public Result<String> manualReboot(@RequestBody IpRobotDto ip) {
        ScheduleService.ipList.add(ip.getIp());
        return Result.success();
    }

    @PostMapping("rebootSuccess")
    public Result<String> rebootSuccess(@RequestBody IpRobotDto ip) {
        scheduleService.rebootSuccess(ip.getIp());
        return Result.success();
    }


    @PostMapping("create")
    public Result<String> create(@RequestBody CreateRobotDto robotDto) {
        logger.info(String.format("创建机器人账号：%s", robotDto.toString()));
        robotService.create(robotDto);
        return Result.success();
    }

    @PostMapping("update")
    public Result<String> update(@RequestBody UpdateRobotDto robotDto) {
        logger.info(String.format("更新机器人账号：%s", robotDto.toString()));
        robotService.update(robotDto);
        return Result.success();
    }

    @GetMapping("del/{id}")
    public Result<String> delete(@PathVariable String id) {
        logger.info(String.format("删除机器人账号：%s", id));
        robotService.delete(id);
        return Result.success();
    }

    @GetMapping("start/{id}")
    public Result<String> start(@PathVariable String id) {
        logger.info(String.format("启动机器人账号：%s", id));
        robotService.start(id);
        return Result.success();
    }

    @GetMapping("stop/{id}")
    public Result<String> stop(@PathVariable String id) {
        logger.info(String.format("停止机器人账号：%s", id));
        robotService.stop(id);
        return Result.success();
    }

    @GetMapping("startLocation/{id}")
    public Result<String> startLocation(@PathVariable String id) {
        logger.info(String.format("开启机器人位置监控功能：%s", id));
        robotService.startLocation(id);
        return Result.success();
    }

    @GetMapping("stopLocation/{id}")
    public Result<String> stopLocation(@PathVariable String id) {
        logger.info(String.format("停止机器人位置监控功能：%s", id));
        robotService.stopLocation(id);
        return Result.success();
    }

    @GetMapping("batchRunOnceLocation")
    public Result<String> batchRunOnceLocation() {
        logger.info("批量运行一次机器人位置监控功能");
        robotService.batchRunOnceLocation();
        return Result.success();
    }

    @GetMapping("sendOneCompanyEmail/{id}")
    public Result<String> sendOneCompanyEmail(@PathVariable String id) {
        logger.info(String.format("运行一次发邮件,%s", id));
        robotService.runOnceLocation(id);
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
