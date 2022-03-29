package com.szcinda.controller;

import com.szcinda.repository.RobotLog;
import com.szcinda.service.PageResult;
import com.szcinda.service.robotLog.CreateLogDto;
import com.szcinda.service.robotLog.LogService;
import com.szcinda.service.robotLog.QueryLog;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("log")
public class LogController {

    private final LogService logService;

    public LogController(LogService logService) {
        this.logService = logService;
    }

    @PostMapping("create")
    public Result<String> create(@RequestBody CreateLogDto dto) {
        logService.create(dto);
        return Result.success();
    }

    @PostMapping("query")
    public PageResult<RobotLog> query(@RequestBody QueryLog queryLog) {
        return logService.query(queryLog);
    }
}
