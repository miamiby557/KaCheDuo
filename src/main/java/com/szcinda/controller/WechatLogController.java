package com.szcinda.controller;

import com.szcinda.repository.WechatLog;
import com.szcinda.service.PageResult;
import com.szcinda.service.wechat.LogCreateDto;
import com.szcinda.service.wechat.WechatLogQueryDto;
import com.szcinda.service.wechat.WechatLogService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("wechatLog")
public class WechatLogController {

    private final WechatLogService wechatLogService;

    public WechatLogController(WechatLogService wechatLogService) {
        this.wechatLogService = wechatLogService;
    }

    @PostMapping("create")
    public Result<String> create(@RequestBody LogCreateDto createDto) {
        wechatLogService.create(createDto);
        return Result.success();
    }

    @PostMapping("query")
    public PageResult<WechatLog> query(@RequestBody WechatLogQueryDto queryDto) {
        return wechatLogService.query(queryDto);
    }
}
