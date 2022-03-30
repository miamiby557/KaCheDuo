package com.szcinda.controller;

import com.szcinda.repository.RobotTask;
import com.szcinda.service.robotTask.RobotTaskService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("handle")
@RestController
public class HandleController {

    private final RobotTaskService robotTaskService;

    public HandleController(RobotTaskService robotTaskService) {
        this.robotTaskService = robotTaskService;
    }

    @GetMapping("getList")
    public Result<List<RobotTask>> getList() {
        return Result.success(robotTaskService.getHandleList());
    }


    // 释放帐号
    @GetMapping("release/{userName}")
    public Result<String> release(@PathVariable String userName) {
        robotTaskService.release(userName);
        return Result.success();
    }

}
