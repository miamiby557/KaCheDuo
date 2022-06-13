package com.szcinda.controller;

import com.szcinda.repository.HistoryTask;
import com.szcinda.repository.RobotTask;
import com.szcinda.service.PageResult;
import com.szcinda.service.RobotPriorityDto;
import com.szcinda.service.RobotScheduleService;
import com.szcinda.service.robotTask.RobotTaskDto;
import com.szcinda.service.robotTask.RobotTaskQuery;
import com.szcinda.service.robotTask.RobotTaskService;
import com.szcinda.service.robotTask.TaskErrorDto;
import com.szcinda.service.wechat.WechatAlarmService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("task")
public class RobotTaskController {

    private final RobotTaskService robotTaskService;
    private final WechatAlarmService wechatAlarmService;
    private final RobotScheduleService robotScheduleService;

    public RobotTaskController(RobotTaskService robotTaskService, WechatAlarmService wechatAlarmService, RobotScheduleService robotScheduleService) {
        this.robotTaskService = robotTaskService;
        this.wechatAlarmService = wechatAlarmService;
        this.robotScheduleService = robotScheduleService;
    }

    @GetMapping("getList")
    public Result<List<RobotTaskDto>> getList() {
        return Result.success(robotTaskService.getStandByList());
    }

    @GetMapping("getLocationMission")
    public Result<List<RobotTaskDto>> getLocationMission() {
        return Result.success(robotTaskService.getOneLocationMission());
    }

    @GetMapping("checkIsStandBy/{id}")
    public Result<String> checkIsStandBy(@PathVariable String id) {
        boolean standby = robotTaskService.checkIsStandby(id);
        if (standby) {
            return Result.success();
        }
        return Result.fail("不是待运行状态");
    }

    @PostMapping("query")
    public PageResult<HistoryTask> query(@RequestBody RobotTaskQuery params) {
        return robotTaskService.query(params);
    }

    @PostMapping("queryRunningTask")
    public Result<List<RobotTask>> queryRunningTask(@RequestBody RobotTaskQuery params) {
        return Result.success(robotTaskService.queryRunningTask(params));
    }

    @GetMapping("create/{userName}")
    public Result<String> create(@PathVariable String userName) {
        robotTaskService.createTask(userName);
        return Result.success();
    }

    @GetMapping("testCreateAllTask")
    public Result<String> testCreateAllTask() {
        robotTaskService.createAllUserNameTask();
        return Result.success();
    }

    @GetMapping("run/{id}")
    public Result<String> run(@PathVariable String id) {
        robotTaskService.run(id);
        return Result.success();
    }

    @GetMapping("reRun/{id}")
    public Result<String> reRun(@PathVariable String id) {
        robotTaskService.reRun(id);
        return Result.success();
    }

    @GetMapping("delete/{id}")
    public Result<String> delete(@PathVariable String id) {
        robotTaskService.delete(id);
        return Result.success();
    }

    @GetMapping("reRunHistoryTask/{id}")
    public Result<String> reRunHistoryTask(@PathVariable String id) {
        robotTaskService.reRunHistoryTask(id);
        return Result.success();
    }

    @PostMapping("error")
    public Result<String> error(@RequestBody TaskErrorDto errorDto) {
        robotTaskService.error(errorDto);
        wechatAlarmService.plusError(errorDto.getUserName());
        return Result.success();
    }

    @GetMapping("finish/{id}/{account}")
    public Result<String> finish(@PathVariable String id, @PathVariable String account) {
        robotTaskService.finish(id);
        wechatAlarmService.minusError(account);
        return Result.success();
    }

    @GetMapping("getOneWatchRobot")
    public Result<RobotPriorityDto> getOneWatchRobot() {
        return Result.success(robotScheduleService.getOneWatchRobot());
    }

    @GetMapping("getOneChaGangRobot")
    public Result<RobotPriorityDto> getOneChaGangRobot() {
        return Result.success(robotScheduleService.getOneChaGangRobot());
    }

    @GetMapping("getRobotPwd/{account}")
    public Result<String> getRobotPwd(@PathVariable String account) {
        return Result.success(robotScheduleService.getPwdByAccount(account));
    }

    @GetMapping("getTopWatchRobotList")
    public Result<List<RobotPriorityDto>> getTopWatchRobotList() {
        return Result.success(robotScheduleService.getTop20FromWatchRobotList());
    }

    @GetMapping("getTopChaGangRobotList")
    public Result<List<RobotPriorityDto>> getTopChaGangRobotList() {
        return Result.success(robotScheduleService.getTop20FromChaGangRobotList());
    }
}
