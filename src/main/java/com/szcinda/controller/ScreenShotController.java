package com.szcinda.controller;

import com.szcinda.repository.HistoryScreenShotTask;
import com.szcinda.repository.ScreenShotTask;
import com.szcinda.service.PageResult;
import com.szcinda.service.screenShotTask.ScreenShotTaskParams;
import com.szcinda.service.screenShotTask.ScreenShotTaskService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
