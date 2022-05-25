package com.szcinda.controller;

import com.szcinda.controller.util.DownPrams;
import com.szcinda.controller.util.FieldMapUtil;
import com.szcinda.repository.FengXian;
import com.szcinda.service.PageResult;
import com.szcinda.service.ScheduleService;
import com.szcinda.service.fengxian.*;
import com.szcinda.service.robotTask.RobotTaskServiceImpl;
import com.szcinda.service.wechat.WechatAlarmService;
import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;
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
@RequestMapping("fx")
public class FengXianController {

    private final static Logger logger = LoggerFactory.getLogger(FengXianController.class);

    private final FengXianService fengXianService;
    private final ScheduleService scheduleService;
    private final WechatAlarmService wechatAlarmService;

    public FengXianController(FengXianService fengXianService, ScheduleService scheduleService, WechatAlarmService wechatAlarmService) {
        this.fengXianService = fengXianService;
        this.scheduleService = scheduleService;
        this.wechatAlarmService = wechatAlarmService;
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

    @GetMapping("generateScreenShotMissions")
    public Result<String> generateScreenShotMissions() {
        fengXianService.generateScreenShotMissions();
        return Result.success();
    }

    static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @PostMapping("export")
    public void export(@RequestParam(required = false) String createTimeStart, @RequestParam(required = false) String createTimeEnd,
                       @RequestParam(required = false) String userName, @RequestParam(required = false) String happenTime,
                       @RequestParam(required = false) String vehicleNo, @RequestParam(required = false) String company,
                       @RequestParam(required = false) String owner,
                       HttpServletResponse response) throws Exception {
        ChuZhiQuery params = new ChuZhiQuery();
        if (StringUtils.hasText(createTimeStart)) {
            params.setCreateTimeStart(LocalDate.parse(createTimeStart, dateTimeFormatter));
        }
        if (StringUtils.hasText(createTimeEnd)) {
            params.setCreateTimeEnd(LocalDate.parse(createTimeEnd, dateTimeFormatter));
        }
        params.setCompany(company);
        params.setVehicleNo(vehicleNo);
        params.setHappenTime(happenTime);
        params.setUserName(userName);
        params.setOwner(owner);
        List<FengXian> fengXianList = fengXianService.queryAll(params);
        OutputStream out = response.getOutputStream();
        try {
            Map<String, List<String>> map = new HashMap<>();
            HSSFWorkbook wb = new HSSFWorkbook();
            HSSFSheet sheet = wb.createSheet("sheet1");
            sheet.setDefaultColumnWidth(20);// 默认列宽
            HSSFRow row = sheet.createRow(0);
            HSSFCellStyle style = wb.createCellStyle();
            style.setAlignment(HorizontalAlignment.CENTER);
            HSSFCell cell;
            Map<String, String> fieldMap = FieldMapUtil.fxMap();
            List<String> columnList = FieldMapUtil.fxColumns();
            // 生成标题
            int size = columnList.size();
            for (int i = 0; i < size; i++) {
                cell = row.createCell((short) i);
                cell.setCellValue(columnList.get(i));
                cell.setCellStyle(style);
            }
            for (FengXian dto : fengXianList) {
                String id = dto.getId();
                List<String> list = new ArrayList<>();
                columnList.forEach(column -> {
                    if (fieldMap.containsKey(column)) {
                        String value;
                        try {
                            Field field = FengXian.class.getDeclaredField(fieldMap.get(column));
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
            String fName = URLEncoder.encode("处置账单", "UTF-8");
            response.setHeader("Content-disposition", "attachment;filename=" + fName + ".xls");
            wb.write(out);
            out.flush();
        } catch (Exception ignored) {

        } finally {
            if (out != null) {
                out.close();
            }
        }
    }


    @PostMapping("sendMsg")
    public Result<String> sendMsg(@RequestBody DownPrams downPrams) {
        if (downPrams.isError()) {
            // 如果是错误的提示，需要统计次数，累计3次才提醒
            int count = wechatAlarmService.geErrorCount(downPrams.getAccount());
            if (count >= 3) {
                // 累计3次发送提醒并将历史错误记录清空
                wechatAlarmService.sendMsg(String.format("【%s】账号【%s】已经累计3次操作失败，最后一次失败原因：【%s】", downPrams.getType(), downPrams.getAccount(), downPrams.getMessage()));
                wechatAlarmService.minusError(downPrams.getAccount());
            } else {
                // 先累计次数，不通知
                wechatAlarmService.plusError(downPrams.getAccount());
            }
        } else {
            wechatAlarmService.sendMsg(downPrams.getMessage());
        }
        return Result.success();
    }


    @GetMapping("watchFinish/{account}")
    public Result<String> watchFinish(@PathVariable String account) {
        wechatAlarmService.minusError(account);
        return Result.success();
    }
}
