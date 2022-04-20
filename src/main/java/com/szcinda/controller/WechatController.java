package com.szcinda.controller;

import com.szcinda.repository.Wechat;
import com.szcinda.service.ScheduleService;
import com.szcinda.service.wechat.WechatService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("wechat")
public class WechatController {
    private final WechatService wechatService;
    private final ScheduleService scheduleService;

    public WechatController(WechatService wechatService, ScheduleService scheduleService) {
        this.wechatService = wechatService;
        this.scheduleService = scheduleService;
    }


    @GetMapping("query")
    public List<Wechat> findAll() {
        return wechatService.query();
    }

    @GetMapping("create/{wechat}")
    public Result<String> create(@PathVariable String wechat) {
        wechatService.create(wechat);
        return Result.success();
    }

    @GetMapping("delete/{id}")
    public Result<String> delete(@PathVariable String id) {
        wechatService.delete(id);
        return Result.success();
    }

    @GetMapping("sync/{wechat}")
    public Result<String> sync(@PathVariable String wechat) {
        wechatService.sync(wechat);
        return Result.success();
    }

    @GetMapping("getFriends/{wechat}")
    public Result<String> getFriends(@PathVariable String wechat) {
        boolean canSync = scheduleService.canRunSyncFriends(wechat);
        if (canSync) {
            return Result.success();
        }
        return Result.fail("暂不同步");
    }
}
