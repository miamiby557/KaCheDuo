package com.szcinda.controller;

import com.szcinda.repository.HistoryTask;
import com.szcinda.repository.RobotTask;
import com.szcinda.service.PageResult;
import com.szcinda.service.robotTask.RobotTaskDto;
import com.szcinda.service.robotTask.RobotTaskQuery;
import com.szcinda.service.robotTask.RobotTaskService;
import com.szcinda.service.robotTask.TaskErrorDto;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("task")
public class RobotTaskController {

    private final RobotTaskService robotTaskService;

    public RobotTaskController(RobotTaskService robotTaskService) {
        this.robotTaskService = robotTaskService;
    }

    @GetMapping("getList")
    public Result<List<RobotTaskDto>> getList() {
        return Result.success(robotTaskService.getStandByList());
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

    @GetMapping("reRunHistoryTask/{id}")
    public Result<String> reRunHistoryTask(@PathVariable String id) {
        robotTaskService.reRunHistoryTask(id);
        return Result.success();
    }

    @PostMapping("error")
    public Result<String> error(@RequestBody TaskErrorDto errorDto) {
        robotTaskService.error(errorDto);
        return Result.success();
    }

    @GetMapping("finish/{id}")
    public Result<String> finish(@PathVariable String id) {
        robotTaskService.finish(id);
        return Result.success();
    }
}
