package com.szcinda.controller;

import com.szcinda.service.PageResult;
import com.szcinda.service.ScheduleService;
import com.szcinda.service.fengxian.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("fx")
public class FengXianController {


    private final FengXianService fengXianService;
    private final ScheduleService scheduleService;

    public FengXianController(FengXianService fengXianService, ScheduleService scheduleService) {
        this.fengXianService = fengXianService;
        this.scheduleService = scheduleService;
    }

    @PostMapping("apiCreate")
    public Result<String> apiCreate(@RequestBody List<CreateFengXianDto> dtos) {
        fengXianService.batchCreate(dtos);
        return Result.success();
    }

    @PostMapping("query")
    public PageResult<ChuZhiDetailDto> query(@RequestBody ChuZhiQuery query) {
        return fengXianService.query(query);
    }

    @PostMapping("chuZhi")
    public Result<String> chuZhi(@RequestBody ChuZhiDto dto) {
        fengXianService.chuZhi(dto);
        return Result.success();
    }

    @GetMapping("canRun/{phone}")
    public Result<String> canRun(@PathVariable String phone) {
        boolean canRun = scheduleService.canRunFX(phone);
        if (canRun) {
            String pwd = scheduleService.getPwd(phone);
            return Result.success(pwd);
        }
        return Result.fail("暂停执行处置");
    }

    @GetMapping("canRunChuLiAndLocation/{phone}")
    public Result<String> canRunChuLiAndLocation(@PathVariable String phone) {
        boolean canRun = scheduleService.canRunChuLiAndLocation(phone);
        if (canRun) {
            String pwd = scheduleService.getPwd(phone);
            return Result.success(pwd);
        }
        return Result.fail("暂停执行处置");
    }

    @GetMapping("canWatch/{id}")
    public Result<String> canWatch(@PathVariable String id) {
        boolean canWatch = scheduleService.canWatch(id);
        if (canWatch) {
            return Result.success();
        }
        return Result.fail("暂停监控");
    }
}
