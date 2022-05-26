package com.szcinda.controller;

import com.szcinda.service.chagang.ChaGangCreateDto;
import com.szcinda.service.chagang.ChaGangDto;
import com.szcinda.service.chagang.ChaGangService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("chaGang")
public class ChaGangController {

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
    public Result<String> delete(@PathVariable String id){
        chaGangService.delete(id);
        return Result.success();
    }


}
