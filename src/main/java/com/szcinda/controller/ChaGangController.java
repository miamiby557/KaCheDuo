package com.szcinda.controller;

import com.szcinda.repository.ChaGangRecord;
import com.szcinda.service.PageResult;
import com.szcinda.service.chagang.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("chaGang")
public class ChaGangController {

    private final static Logger logger = LoggerFactory.getLogger(ChaGangController.class);

    private final ChaGangService chaGangService;

    public ChaGangController(ChaGangService chaGangService) {
        this.chaGangService = chaGangService;
    }

    @PostMapping("create")
    public Result<String> create(@RequestBody ChaGangCreateDto createDto) {
        chaGangService.create(createDto);
        return Result.success();
    }

    @GetMapping("query")
    public Result<List<ChaGangDto>> query() {
        return Result.success(chaGangService.query());
    }

    @GetMapping("alive/{account}")
    public Result<String> alive(@PathVariable String account) {
        chaGangService.alive(account);
        return Result.success();
    }

    @GetMapping("delete/{id}")
    public Result<String> delete(@PathVariable String id) {
        chaGangService.delete(id);
        return Result.success();
    }

    @PostMapping("createRecord")
    public Result<String> createRecord(@RequestBody List<ChaGangRecordCreateDto> createDtos) {
        logger.info(String.format("创建查岗记录：【%s】", createDtos));
        for (ChaGangRecordCreateDto createDto : createDtos) {
            chaGangService.createRecord(createDto);
        }
        return Result.success();
    }

    @PostMapping("queryRecord")
    public PageResult<ChaGangRecord> queryRecord(@RequestBody RecordQueryDto queryDto) {
        return chaGangService.query(queryDto);
    }
}
