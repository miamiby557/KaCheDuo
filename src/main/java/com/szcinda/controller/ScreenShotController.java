package com.szcinda.controller;

import com.szcinda.repository.HistoryScreenShotTask;
import com.szcinda.repository.ScreenShotTask;
import com.szcinda.service.PageResult;
import com.szcinda.service.screenShotTask.ScreenShotTaskErrorDto;
import com.szcinda.service.screenShotTask.ScreenShotTaskParams;
import com.szcinda.service.screenShotTask.ScreenShotTaskService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("screenShot")
public class ScreenShotController {

    private final ScreenShotTaskService screenShotTaskService;

    public ScreenShotController(ScreenShotTaskService screenShotTaskService) {
        this.screenShotTaskService = screenShotTaskService;
    }

    @PostMapping("query")
    public PageResult<HistoryScreenShotTask> query(@RequestBody ScreenShotTaskParams params) {
        return screenShotTaskService.query(params);
    }

    @PostMapping("queryRunningTask")
    public Result<List<ScreenShotTask>> queryRunningTask(@RequestBody ScreenShotTaskParams params) {
        return Result.success(screenShotTaskService.queryRunning(params));
    }

    @GetMapping("getOneJob/{ownerWechat}")
    public Result<ScreenShotTask> getOneJob(@PathVariable String ownerWechat) {
        return Result.success(screenShotTaskService.findOneMission(ownerWechat));
    }

    // 获取一条发送的任务
    @GetMapping("getOneSendJob/{ownerWechat}")
    public Result<ScreenShotTask> getOneSendJob(@PathVariable String ownerWechat) {
        return Result.success(screenShotTaskService.findOneSendMission(ownerWechat));
    }

    @GetMapping("finishSend/{screenShotId}")
    public Result<String> finishSend(@PathVariable String screenShotId){
        screenShotTaskService.finishSend(screenShotId);
        return Result.success();
    }

    @PostMapping("error")
    public Result<String> screenShotError(@RequestBody ScreenShotTaskErrorDto errorDto) {
        screenShotTaskService.error(errorDto);
        return Result.success();
    }
}
