package com.szcinda.controller;

import com.alibaba.excel.EasyExcelFactory;
import com.alibaba.excel.metadata.Sheet;
import com.szcinda.service.PageResult;
import com.szcinda.service.driver.*;
import com.szcinda.service.wechat.WechatService;
import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.lang.reflect.Field;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("driver")
public class DriverController {

    private final DriverService driverService;
    private final WechatService wechatService;

    @Value("${file.save.path}")
    private String savePath;

    public DriverController(DriverService driverService, WechatService wechatService) {
        this.driverService = driverService;
        this.wechatService = wechatService;
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

    @GetMapping("downloadNoWechat/{owner}")
    public void downloadNoWechat(@PathVariable String owner, HttpServletResponse response) {
        OutputStream out = null;
        try {
            out = response.getOutputStream();
            List<DriverDto> driverList = driverService.queryNoWechat(owner);
            Map<String, List<String>> map = new HashMap<>();
            HSSFWorkbook wb = new HSSFWorkbook();
            HSSFSheet sheet = wb.createSheet("sheet1");
            sheet.setDefaultColumnWidth(20);// 默认列宽
            HSSFRow row = sheet.createRow(0);
            HSSFCellStyle style = wb.createCellStyle();
            style.setAlignment(HorizontalAlignment.CENTER);
            HSSFCell cell;
            Map<String, String> fieldMap = DriverDto.getFieldMap();
            List<String> columnList = DriverDto.getFieldList();
            // 生成标题
            int size = columnList.size();
            for (int i = 0; i < size; i++) {
                cell = row.createCell((short) i);
                cell.setCellValue(columnList.get(i));
                cell.setCellStyle(style);
            }
            for (DriverDto dto : driverList) {
                String id = dto.getId();
                List<String> list = new ArrayList<>();
                columnList.forEach(column -> {
                    if (fieldMap.containsKey(column)) {
                        String value;
                        try {
                            Field field = DriverDto.class.getDeclaredField(fieldMap.get(column));
                            if (field.getType() == String.class) {
                                value = (String) field.get(dto);
                            } else if (field.getType() == Integer.class || field.getGenericType().getTypeName().equals("int")) {
                                value = ((Integer) field.get(dto)).toString();
                            } else if (field.getType() == Double.class || field.getGenericType().getTypeName().equals("double")) {
                                value = field.get(dto).toString();
                            } else if (field.getType() == LocalDateTime.class) {
                                value = ((LocalDateTime) field.get(dto)).toString().replace("T", " ");
                            } else if (field.getType() == LocalDate.class) {
                                value = ((LocalDate) field.get(dto)).toString();
                            } else {
                                value = "";
                            }
                        } catch (Exception e) {
                            value = "";
                        }
                        list.add(value);
                    }
                });
                map.put(id, list);
            }
            int i = 0;
            for (String str : map.keySet()) {
                row = sheet.createRow(i + 1);
                List<String> list = map.get(str);
                for (int j = 0; j < size; j++) {
                    row.createCell((short) j).setCellValue(list.get(j));
                }
                i++;
            }
            // 下载EXCEL
            String fName = URLEncoder.encode("司机信息表", "UTF-8");
            response.setHeader("Content-disposition", "attachment;filename=" + fName + ".xls");
            wb.write(out);
            out.flush();
        } catch (Exception exception) {
            exception.printStackTrace();
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    @GetMapping("delete/{id}")
    public Result<String> delete(@PathVariable String id) {
        driverService.delete(id);
        return Result.success();
    }

    @PostMapping("query")
    public PageResult<DriverDto> query(@RequestBody DriverQuery query) {
        return driverService.query(query);
    }

    @PostMapping("update")
    public Result<String> update(@RequestBody DriverUpdateDto updateDto) {
        driverService.update(updateDto);
        return Result.success();
    }

    @PostMapping("updateInfo")
    public Result<String> updateInfo(@RequestBody UpdateDriverInfo info) {
        driverService.updateInfo(info);
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

    @GetMapping("sync/{wechat}")
    public Result<String> sync(@PathVariable String wechat) {
        wechatService.sync(wechat);
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
