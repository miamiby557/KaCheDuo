package com.szcinda.controller;

import com.szcinda.service.PageResult;
import com.szcinda.service.ScheduleService;
import com.szcinda.service.fengxian.*;
import com.szcinda.service.robotTask.RobotTaskServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("fx")
public class FengXianController {

    private final static Logger logger = LoggerFactory.getLogger(FengXianController.class);

    private final FengXianService fengXianService;
    private final ScheduleService scheduleService;

    public FengXianController(FengXianService fengXianService, ScheduleService scheduleService) {
        this.fengXianService = fengXianService;
        this.scheduleService = scheduleService;
    }

    @PostMapping("apiCreate")
    public Result<String> apiCreate(@RequestBody List<CreateFengXianDto> dtos) {
        logger.info(String.format("批量创建风险处置：%s", dtos.toString()));
        fengXianService.batchCreate(dtos);
        return Result.success();
    }

    // 批量插入之前的未处理的数据
    @PostMapping("batchCreateHandleList")
    public Result<String> batchCreateHandleList(@RequestBody List<CreateFengXianDto> dtos) {
        logger.info("批量插入之前的未处理的数据");
        fengXianService.batchCreateHandle(dtos);
        return Result.success();
    }


    @PostMapping("query")
    public PageResult<ChuZhiDetailDto> query(@RequestBody ChuZhiQuery query) {
        return fengXianService.query(query);
    }

    @PostMapping("chuZhi")
    public Result<String> chuZhi(@RequestBody ChuZhiDto dto) {
        logger.info(String.format("机器人上传处置：%s", dto.toString()));
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
            // 再检查一下是否账号正在处理,如果在集合里面，则不允许运行
            canRun = !RobotTaskServiceImpl.handleAccountMap.containsKey(phone);
        }
        if (canRun) {
            String pwd = scheduleService.getPwd(phone);
            return Result.success(pwd);
        }
        return Result.fail("暂停执行处理和位置监控");
    }

    @GetMapping("canWatch/{id}")
    public Result<String> canWatch(@PathVariable String id) {
        boolean canWatch = scheduleService.canWatch(id);
        if (canWatch) {
            return Result.success();
        }
        return Result.fail("暂停监控");
    }

    @GetMapping("generateScreenShotMissions")
    public Result<String> generateScreenShotMissions() {
        fengXianService.generateScreenShotMissions();
        return Result.success();
    }
}
