package com.szcinda.controller;

import com.alibaba.excel.EasyExcelFactory;
import com.alibaba.excel.metadata.Sheet;
import com.szcinda.service.driver.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@RestController
@RequestMapping("driver")
public class DriverController {

    private final DriverService driverService;

    public DriverController(DriverService driverService) {
        this.driverService = driverService;
    }

    @PostMapping("/import/{owner}")
    public Result<String> orderImport(@RequestParam("file") MultipartFile file, @PathVariable String owner) throws Exception {
        // 解析每行结果在listener中处理
        InputStream inputStream = file.getInputStream();
        try {
            DriverExcelListener listener = new DriverExcelListener();
            EasyExcelFactory.readBySax(inputStream, new Sheet(1, 1, DriverImportDto.class), listener);
            driverService.importDriver(listener.getImportDatas(), owner);
            return Result.success();
        } catch (Exception e) {
            e.printStackTrace();
            return Result.fail(e.getMessage());
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    @GetMapping("delete/{id}")
    public Result<String> delete(@PathVariable String id) {
        driverService.delete(id);
        return Result.success();
    }

    @PostMapping("query")
    public Result<List<DriverDto>> query(@RequestBody DriverQuery query) {
        return Result.success(driverService.query(query));
    }
}
