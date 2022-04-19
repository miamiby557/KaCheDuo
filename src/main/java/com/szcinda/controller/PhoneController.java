package com.szcinda.controller;

import com.szcinda.repository.Phone;
import com.szcinda.service.phone.PhoneDto;
import com.szcinda.service.phone.PhoneService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("phone")
public class PhoneController {
    private final PhoneService phoneService;

    public PhoneController(PhoneService phoneService) {
        this.phoneService = phoneService;
    }

    @GetMapping("query")
    public List<PhoneDto> findAll() {
        return phoneService.getAll();
    }

    @GetMapping("create/{phone}")
    public Result<String> create(@PathVariable String phone) {
        phoneService.create(phone);
        return Result.success();
    }

    @GetMapping("delete/{id}")
    public Result<String> delete(@PathVariable String id) {
        phoneService.delete(id);
        return Result.success();
    }

    @GetMapping("alive/{phone}")
    public Result<String> alive(@PathVariable String phone){
        phoneService.alive(phone);
        return Result.success();
    }
}
