package com.szcinda.controller;

import com.alibaba.excel.EasyExcelFactory;
import com.alibaba.excel.metadata.Sheet;
import com.szcinda.service.driver.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("driver")
public class DriverController {

    private final DriverService driverService;

    @Value("${file.save.path}")
    private String savePath;

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

    @PostMapping("update")
    public Result<String> update(@RequestBody DriverUpdateDto updateDto) {
        driverService.update(updateDto);
        return Result.success();
    }


    @PostMapping("connect")
    public Result<String> connect(@RequestBody DriverConnectDto connectDto) {
        driverService.connect(connectDto);
        return Result.success();
    }

    @GetMapping("confirm/{wechat}")
    public Result<String> confirm(@PathVariable String wechat) {
        driverService.confirm(wechat);
        return Result.success();
    }

    @PostMapping("savePic/{fxId}/{screenTaskId}")
    public Result<String> savePic(@RequestParam("file") MultipartFile file, @PathVariable String fxId, @PathVariable String screenTaskId) throws Exception {
        //保存文件
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        String dataStr = LocalDateTime.now().format(dateTimeFormatter);
        File path = new File(savePath, dataStr);
        if (!path.exists()) {
            path.mkdirs();
        }
        String fileName = file.getOriginalFilename();//获取文件名（包括后缀）
        File saveFile = new File(path, fileName);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(saveFile);
            fos.write(file.getBytes()); // 写入文件
            DriverScreenShotDto screenShotDto = new DriverScreenShotDto();
            screenShotDto.setFxId(fxId);
            screenShotDto.setScreenTaskId(screenTaskId);
            screenShotDto.setFilePath(dataStr + File.separator + fileName);
            driverService.savePic(screenShotDto);
            return Result.success();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return Result.fail("保存文件失败");
    }
}
