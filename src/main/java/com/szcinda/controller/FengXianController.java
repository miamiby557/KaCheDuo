package com.szcinda.controller;

import com.szcinda.controller.util.CsvExportUtil;
import com.szcinda.controller.util.DownPrams;
import com.szcinda.controller.util.FieldMapUtl;
import com.szcinda.repository.FengXian;
import com.szcinda.service.PageResult;
import com.szcinda.service.ScheduleService;
import com.szcinda.service.fengxian.*;
import com.szcinda.service.robotTask.RobotTaskServiceImpl;
import com.szcinda.service.wechat.WechatAlarmService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.lang.reflect.Field;
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
        //logger.info(String.format("批量创建风险处置：%s", dtos.toString()));
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
        boolean canRun = scheduleService.canWatch(phone);
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
                       HttpServletResponse response) {
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
        try (OutputStream out = response.getOutputStream()) {
            StringBuilder titles = new StringBuilder();
            List<FieldMapUtl.Item> fxFieldMap = FieldMapUtl.getFXFieldMap();
            List<String> keys = new ArrayList<>();
            for (FieldMapUtl.Item item : fxFieldMap) {
                titles.append(item.getLabel()).append(CsvExportUtil.CSV_COLUMN_SEPARATOR);
                keys.add(item.getField());
            }
            // 构造导出数据
            List<Map<String, String>> datas = new ArrayList<>();
            Map<String, String> mapInfo;
            for (FengXian fengXian : fengXianList) {
                mapInfo = new HashMap<>(keys.size());
                String value;
                for (String key : keys) {
                    try {
                        Field field = FengXian.class.getDeclaredField(key);
                        if (field.getType() == String.class) {
                            value = (String) field.get(fengXian);
                        } else if (field.getType() == Integer.class || field.getGenericType().getTypeName().equals("int")) {
                            value = ((Integer) field.get(fengXian)).toString();
                        } else if (field.getType() == Double.class || field.getGenericType().getTypeName().equals("double")) {
                            value = field.get(fengXian).toString();
                        } else if (field.getType() == LocalDateTime.class) {
                            value = ((LocalDateTime) field.get(fengXian)).toString().replace("T", " ");
                        } else if (field.getType() == LocalDate.class) {
                            value = ((LocalDate) field.get(fengXian)).toString();
                        } else {
                            value = "";
                        }
                    } catch (Exception exception) {
                        value = "";
                    }
                    if(value == null){
                        value = "";
                    }
                    mapInfo.put(key, value);
                }
                datas.add(mapInfo);
            }
            String fName = "处置-";
            CsvExportUtil.responseSetProperties(fName, response);
            CsvExportUtil.doExport(datas, titles.substring(0, titles.length() - 1), keys, out);
        } catch (Exception exception) {
            logger.info("导出数据异常，异常信息如下：");
            logger.info(exception.getLocalizedMessage());
        }
    }

    @GetMapping("testExport")
    public void testExport(HttpServletResponse response){
        try (OutputStream out = response.getOutputStream()) {
            StringBuilder titles = new StringBuilder();
            List<FieldMapUtl.Item> fxFieldMap = FieldMapUtl.getFXFieldMap();
            List<String> keys = new ArrayList<>();
            for (FieldMapUtl.Item item : fxFieldMap) {
                titles.append(item.getLabel()).append(CsvExportUtil.CSV_COLUMN_SEPARATOR);
                keys.add(item.getField());
            }
            // 构造导出数据
            List<Map<String, String>> datas = new ArrayList<>();
            Map<String, String> mapInfo;
            List<FengXian> fengXianList = new ArrayList<>();
            FengXian create = new FengXian();
            create.setVehicleNo("粤B12345");
            fengXianList.add(create);
            for (FengXian fengXian : fengXianList) {
                mapInfo = new HashMap<>(keys.size());
                String value;
                for (String key : keys) {
                    try {
                        Field field = FengXian.class.getDeclaredField(key);
                        if (field.getType() == String.class) {
                            value = (String) field.get(fengXian);
                        } else if (field.getType() == Integer.class || field.getGenericType().getTypeName().equals("int")) {
                            value = ((Integer) field.get(fengXian)).toString();
                        } else if (field.getType() == Double.class || field.getGenericType().getTypeName().equals("double")) {
                            value = field.get(fengXian).toString();
                        } else if (field.getType() == LocalDateTime.class) {
                            value = ((LocalDateTime) field.get(fengXian)).toString().replace("T", " ");
                        } else if (field.getType() == LocalDate.class) {
                            value = ((LocalDate) field.get(fengXian)).toString();
                        } else {
                            value = "";
                        }
                    } catch (Exception exception) {
                        value = "";
                    }
                    if(value == null){
                        value = "";
                    }
                    mapInfo.put(key, value);
                }
                datas.add(mapInfo);
            }
            String fName = "处置-";
            CsvExportUtil.responseSetProperties(fName, response);
            CsvExportUtil.doExport(datas, titles.substring(0, titles.length() - 1), keys, out);
        } catch (Exception exception) {
            logger.info("导出数据异常，异常信息如下：");
            logger.info(exception.getLocalizedMessage());
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
