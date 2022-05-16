package com.szcinda.service.mail;

import com.szcinda.repository.*;
import com.szcinda.service.SnowFlakeFactory;
import com.szcinda.service.TypeStringUtils;
import com.szcinda.service.report.CountDto;
import com.szcinda.service.report.ReportDto;
import net.sf.jxls.transformer.XLSTransformer;
import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.mail.internet.MimeMessage;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.szcinda.service.TypeStringUtils.*;

@Component
public class SendMailService {

    private final static Logger logger = LoggerFactory.getLogger(SendMailService.class);


    @Value("${spring.mail.username}")
    private String from;

    // 管理员微信号
    @Value("${admin.user.wechat}")
    private String wechats;

    // 抄送邮箱
    @Value("${cc.email}")
    private String ccEmail;

    @Autowired
    //用于发送文件
    private JavaMailSender mailSender;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private RobotRepository robotRepository;

    @Autowired
    private ScreenShotTaskRepository screenShotTaskRepository;

    private final SnowFlakeFactory snowFlakeFactory = SnowFlakeFactory.getInstance();

    @Autowired
    private FengXianRepository fengXianRepository;

    public void testSend() {
        sendAttachmentMail("huangrensen@uniner.com", "报表", "报表测试", null);
    }

    // 发送一次公司的邮件
    public void sendOnceCompanyEmail(String id) {
        logger.info(String.format("正在发送邮件：%s", id));
        // 主账号
        Robot robot = robotRepository.findById(id);
        // 没有成功发送邮件的列表
        List<String> emailList = new ArrayList<>();
        // 子账号
        List<Robot> robots = robotRepository.findByParentId(robot.getId());
        // 取出子账号
        List<String> accountList = robots.stream().filter(robot1 -> robot1.getParentId() != null && robot1.getParentId().equals(robot.getId())).map(Robot::getPhone).collect(Collectors.toList());
        accountList.add(robot.getPhone());
        // 先查出所有昨天的数据
        LocalDate lastDate = LocalDate.now().minusDays(1);
        Specification<Location> specification = ((root, criteriaQuery, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            Predicate timeStart = criteriaBuilder.greaterThanOrEqualTo(root.get("createTime"), lastDate.atStartOfDay());
            predicates.add(timeStart);
            Predicate timeEnd = criteriaBuilder.lessThan(root.get("createTime"), lastDate.plusDays(1).atStartOfDay());
            predicates.add(timeEnd);
            Expression<String> exp = root.get("owner");
            predicates.add(exp.in(accountList));
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        });
        List<Location> locationList = locationRepository.findAll(specification);
        String email = robot.getEmail();
        if (StringUtils.isEmpty(email)) {
            return;
        }

        // 取出关于这个账号的所有处置列表
        Specification<FengXian> specification2 = ((root, criteriaQuery, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            Predicate timeStart = criteriaBuilder.greaterThanOrEqualTo(root.get("createTime"), lastDate.atStartOfDay());
            predicates.add(timeStart);
            Predicate timeEnd = criteriaBuilder.lessThan(root.get("createTime"), lastDate.plusDays(1).atStartOfDay());
            predicates.add(timeEnd);
            Expression<String> exp = root.get("owner");
            predicates.add(exp.in(accountList));
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        });
        // 过滤出关于这个账号的风险处置
        List<FengXian> fengXianList = fengXianRepository.findAll(specification2);
        // 替换中文符号 去除空格
        email = email.trim().replaceAll(" ", "").replace("，", ",");
        String[] emailArray = email.split(",");

        Map<String, List<Location>> locationMap;
        if (locationList == null || locationList.size() == 0) {
            List<String> vehicleNos = fengXianList.stream().map(FengXian::getVehicleNo).distinct().collect(Collectors.toList());
            locationMap = new HashMap<>();
            for (String vehicleNo : vehicleNos) {
                locationMap.put(vehicleNo, new ArrayList<>());
            }
        } else {
            locationMap = locationList.stream().filter(location -> accountList.contains(location.getOwner())).collect(Collectors.groupingBy(Location::getVehicleNo));
        }
        // 填充excel数据的列表
        List<ReportDto> reportDtos = new ArrayList<>();
        // 统计
        CountDto countDto = new CountDto();

        locationMap.forEach((vehicleNo, dataList) -> {
            if (dataList != null && dataList.size() > 0) {
                for (Location location : dataList) {
                    ReportDto reportDto = new ReportDto();
                    reportDto.setVehicleNo(vehicleNo);
                    reportDto.setLocation(location.getHappenPlace());
                    if (location.getHappenTime() != null) {
                        reportDto.setCheckTime(location.getHappenTime().format(DateTimeFormatter.ofPattern("HH时mm分")));
                    }
                    reportDto.setSpeed(location.getSpeed());
                    reportDto.setVehicleType("重型货车");
                    reportDto.setMessage("");
                    // 正常行驶
                    reportDto.setType(1);
                    countDto.addType(1);
                    reportDtos.add(reportDto);
                }
            }
            // 不过滤疲劳驾驶或者超速
            List<FengXian> fengXians = fengXianList.stream().filter(fengXian -> fengXian.getVehicleNo().equals(vehicleNo) && inDangerType(fengXian.getDangerType()))
                    .collect(Collectors.toList());
            if (fengXians.size() > 0) {
                for (FengXian fengXian : fengXians) {
                    ReportDto reportDto = new ReportDto();
                    reportDto.setVehicleNo(vehicleNo);
                    reportDto.setLocation(fengXian.getHappenPlace());
                    reportDto.setCheckTime(formatCallTime(fengXian.getHappenTime()));
                    reportDto.setSpeed(fengXian.getSpeed());
                    reportDto.setVehicleType("重型货车");
                    this.handle(fengXian, reportDto, reportDtos, countDto);
                }
            }
        });
        // 写入文件
        String date = LocalDate.now().minusDays(1).toString();
        Map<String, Object> beans = new HashMap<>();

        countDto.setCompany(robot.getCompany());
        countDto.setMonth(String.valueOf(lastDate.getMonth().getValue()));
        countDto.setTotalCount(String.valueOf(robot.getCarCount()));
        countDto.setDay(String.valueOf(lastDate.getDayOfMonth()));
        countDto.setAliveTotal(reportDtos.stream().map(ReportDto::getVehicleNo).collect(Collectors.toSet()).size());
        countDto.setFxCount(reportDtos.stream().filter(reportDto -> StringUtils.isEmpty(reportDto.getType1())).map(ReportDto::getVehicleNo).collect(Collectors.toSet()).size());
        countDto.setCzCount((int) reportDtos.stream().filter(reportDto -> StringUtils.isEmpty(reportDto.getType1())).count());
        countDto.setManCount(countDto.getFxCount());

        // 追加序号
        int len = reportDtos.size();
        for (int i = 0; i < len; i++) {
            reportDtos.get(i).setIndex(i + 1);
        }

        beans.put("countDto", countDto);
        beans.put("vehicleList", reportDtos);
        InputStream is = null;
        OutputStream os = null;
        // 写出文件
        File saveFile = new File(System.getProperty("user.dir"), robot.getCompany() + "GPS监控表.xls");
        try {
            // 获取模板文件
            is = this.getClass().getClassLoader().getResourceAsStream("GPS监控表.xls");
            // 实例化 XLSTransformer 对象
            XLSTransformer xlsTransformer = new XLSTransformer();
            // 获取 Workbook ，传入 模板 和 数据
            Workbook workbook = xlsTransformer.transformXLS(is, beans);
            os = new BufferedOutputStream(new FileOutputStream(saveFile));
            // 输出
            workbook.write(os);
            os.flush();
            // 关闭和刷新管道，不然可能会出现表格数据不齐，打不开之类的问题
        } catch (Exception ignored) {

        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        if (!saveFile.exists()) {
            return;
        }
        String mailText = "详情见附件\n";
        if (reportDtos.size() == 0) {
            mailText += "<p><span style=\"font-size: 36px; color: rgb(255, 0, 0);\">请注意：GPS监控没有数据！</span><span style=\"font-size: 36px; color: rgb(255, 0, 0);\"></span></p>";
        }
        // 发送邮件
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper;
        try {
            helper = new MimeMessageHelper(message, true);
            String fileName = robot.getCompany() + "GPS监控表";
            //true代表支持多组件，如附件，图片等
            helper.setFrom(from);
            helper.setTo(emailArray);
            helper.setSubject(date + "-" + fileName);
            helper.setCc(ccEmail);
            helper.setText(mailText, true);
            FileSystemResource file = new FileSystemResource(saveFile);
            fileName = fileName + ".xls";
            helper.addAttachment(fileName, file);//添加附件，可多次调用该方法添加多个附件
            boolean hasSend = false;
            for (int i = 0; i < 3; i++) {
                try {
                    mailSender.send(message);
                    logger.info("这些账号发送成功：" + email);
                    hasSend = true;
                    break;
                } catch (Exception exception) {
                    exception.printStackTrace();
                    logger.info(exception.getLocalizedMessage());
                    Thread.sleep(2000);
                }
            }
            if (!hasSend) {
                // 没发送成功，就加入到未发送成功的列表
                emailList.add(robot.getCompany() + ":" + email);
                logger.info("这些账号发送失败：" + email);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // 删除临时文件
        try {
            saveFile.delete();
        } catch (Exception ignored) {
        }
        logger.info("发送动作完成");
        logger.info(String.format("没有发送成功的邮箱：%s", emailList.toString()));
        if (emailList.size() > 0) {
            // 需要创建一条微信发消息任务通知管理员
            if (StringUtils.hasText(wechats)) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("以下邮箱没有成功发送邮件").append("\n");
                for (int i = 0; i < emailList.size(); i++) {
                    stringBuilder.append(i + 1).append(",").append(emailList.get(i)).append("\n");
                }
                String[] strings = wechats.split(",");
                for (String wechat : strings) {
                    // 判断是否存在报警记录，存在则更新
                    List<ScreenShotTask> screenShotTasks = screenShotTaskRepository.findByWechatAndStatus(wechat, TypeStringUtils.wechat_status5);
                    if (screenShotTasks.size() > 0) {
                        ScreenShotTask screenShotTask = screenShotTasks.get(0);
                        screenShotTask.setContent(stringBuilder.toString());
                        screenShotTaskRepository.save(screenShotTask);
                    } else {
                        ScreenShotTask screenShotTask = new ScreenShotTask();
                        screenShotTask.setId(snowFlakeFactory.nextId("ST"));
                        screenShotTask.setWechat(wechat);
                        screenShotTask.setVehicleNo("");
                        screenShotTask.setOwnerWechat("anqin1588");
                        screenShotTask.setWxid(wechat);
                        screenShotTask.setOwner("");
                        screenShotTask.setStatus(TypeStringUtils.wechat_status5);
                        screenShotTask.setContent(stringBuilder.toString());
                        screenShotTaskRepository.save(screenShotTask);
                    }
                }
            }
        }
    }

    private String formatCallTime(String callTime) {

        try {
            if (callTime.length() > 19) {
                callTime = callTime.substring(0, 19);
                LocalDateTime time = LocalDateTime.parse(callTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                return time.format(DateTimeFormatter.ofPattern("HH时mm分"));
            }
        } catch (Exception e) {

        }
        return "";
    }

    static String[] dangerType = new String[]{"抽烟报警", "接打手机报警", "玩手机报警", "超速报警", "生理疲劳报警", "双脱把报警"};

    private boolean inDangerType(String type) {
        for (String ty : dangerType) {
            if (ty.equals(type)) {
                return true;
            }
        }
        return false;
    }


    // 公共处理方法
    private void handle(FengXian fengXian, ReportDto reportDto, List<ReportDto> reportDtos, CountDto countDto) {
        if (tired_status.equals(fengXian.getDangerType())) {
            if (StringUtils.hasText(fengXian.getCallTime())) {
                reportDto.setMessage(tired_status);
                reportDto.setHandleResult(messageStop);
                // 格式话时间
                reportDto.setHandleText(formatCallTime(fengXian.getCallTime()) + "已拨打电话通知");
                // 生理疲劳
                reportDto.setType(6);
                countDto.addType(6);
                reportDtos.add(reportDto);
            }
        } else if (over_status.equals(fengXian.getDangerType())) {
            if (StringUtils.hasText(fengXian.getCallTime())) {
                reportDto.setMessage(over_status);
                reportDto.setHandleResult(messageSlow);
                // 格式话时间
                reportDto.setHandleText(formatCallTime(fengXian.getCallTime()) + "已拨打电话通知");
                // 生理疲劳
                reportDto.setType(5);
                countDto.addType(5);
                reportDtos.add(reportDto);
            }
        } else {
            reportDto.setMessage(fengXian.getDangerType());
            reportDto.setHandleResult(messageSafe);
            if (fengXian.getDisposeTime() != null) {
                try {
                    reportDto.setHandleText(fengXian.getDisposeTime().format(DateTimeFormatter.ofPattern("HH时mm分")) + "已下发语音信息通知");
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
            }
            if (smoke_status.equals(fengXian.getDangerType())) {
                reportDto.setType(2);
                countDto.addType(2);
            } else if (hangup_phone_status.equals(fengXian.getDangerType())) {
                reportDto.setType(3);
                countDto.addType(3);
            } else if (play_phone_status.equals(fengXian.getDangerType())) {
                reportDto.setType(4);
                countDto.addType(4);
            }
            reportDtos.add(reportDto);
        }
    }


    @Scheduled(cron = "0 0 8 * * ?")
    public void sendMail() throws Exception {
        logger.info("正在全量发邮件");
        List<Robot> robots = robotRepository.findAll();
        // 发送失败的邮件
        List<String> emailList = new ArrayList<>();
        // 先查出所有昨天的数据
        LocalDate lastDate = LocalDate.now().minusDays(1);
        Specification<Location> specification = ((root, criteriaQuery, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            Predicate timeStart = criteriaBuilder.greaterThanOrEqualTo(root.get("createTime"), lastDate.atStartOfDay());
            predicates.add(timeStart);
            Predicate timeEnd = criteriaBuilder.lessThan(root.get("createTime"), lastDate.plusDays(1).atStartOfDay());
            predicates.add(timeEnd);
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        });
        List<Location> locationList = locationRepository.findAll(specification);
        // 过滤出主账号
        List<Robot> mainRobotList = robots.stream().filter(item -> StringUtils.isEmpty(item.getParentId())).collect(Collectors.toList());
        for (Robot robot : mainRobotList) {
            String email = robot.getEmail();
            if (StringUtils.isEmpty(email)) {
                continue;
            }
            // 取出子账号
            List<String> accountList = robots.stream().filter(robot1 -> robot1.getParentId() != null && robot1.getParentId().equals(robot.getId())).map(Robot::getPhone).collect(Collectors.toList());
            accountList.add(robot.getPhone());
            // 取出关于这个账号的所有处置列表
            Specification<FengXian> specification2 = ((root, criteriaQuery, criteriaBuilder) -> {
                List<Predicate> predicates = new ArrayList<>();
                Predicate timeStart = criteriaBuilder.greaterThanOrEqualTo(root.get("createTime"), lastDate.atStartOfDay());
                predicates.add(timeStart);
                Predicate timeEnd = criteriaBuilder.lessThan(root.get("createTime"), lastDate.plusDays(1).atStartOfDay());
                predicates.add(timeEnd);
                Expression<String> exp = root.get("owner");
                predicates.add(exp.in(accountList));
                return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
            });
            // 过滤出关于这个账号的风险处置
            List<FengXian> fengXianList = fengXianRepository.findAll(specification2);
            // 替换中文符号 去除空格
            email = email.trim().replaceAll(" ", "").replace("，", ",");
            String[] emailArray = email.split(",");
            // 过滤出关于当前账号的位置监控
            List<Location> ownerLocationList = locationList.stream().filter(location -> accountList.contains(location.getOwner())).collect(Collectors.toList());
            Map<String, List<Location>> locationMap;
            if (ownerLocationList.size() == 0) {
                List<String> vehicleNos = fengXianList.stream().filter(fengXian -> accountList.contains(fengXian.getOwner())).map(FengXian::getVehicleNo).distinct().collect(Collectors.toList());
                locationMap = new HashMap<>();
                for (String vehicleNo : vehicleNos) {
                    locationMap.put(vehicleNo, new ArrayList<>());
                }
            } else {
                locationMap = locationList.stream().filter(location -> accountList.contains(location.getOwner())).collect(Collectors.groupingBy(Location::getVehicleNo));
            }

            List<ReportDto> reportDtos = new ArrayList<>();
            CountDto countDto = new CountDto();// 统计

            locationMap.forEach((vehicleNo, dataList) -> {
                for (Location location : dataList) {
                    ReportDto reportDto = new ReportDto();
                    reportDto.setVehicleNo(vehicleNo);
                    reportDto.setLocation(location.getHappenPlace());
                    if (location.getHappenTime() != null) {
                        reportDto.setCheckTime(location.getHappenTime().format(DateTimeFormatter.ofPattern("HH时mm分")));
                    }
                    reportDto.setSpeed(location.getSpeed());
                    reportDto.setVehicleType("重型货车");
                    reportDto.setMessage("");
                    // 正常行驶
                    reportDto.setType(1);
                    countDto.addType(1);
                    reportDtos.add(reportDto);
                }
                // 不过滤疲劳驾驶或者超速
                List<FengXian> fengXians = fengXianList.stream().filter(fengXian -> fengXian.getVehicleNo().equals(vehicleNo) && inDangerType(fengXian.getDangerType()))
                        .collect(Collectors.toList());
                if (fengXians.size() > 0) {
                    for (FengXian fengXian : fengXians) {
                        ReportDto reportDto = new ReportDto();
                        reportDto.setVehicleNo(vehicleNo);
                        reportDto.setLocation(fengXian.getHappenPlace());
                        reportDto.setCheckTime(formatCallTime(fengXian.getHappenTime()));
                        reportDto.setSpeed(fengXian.getSpeed());
                        reportDto.setVehicleType("重型货车");
                        this.handle(fengXian, reportDto, reportDtos, countDto);
                    }
                }
            });
            // 写入文件
            String date = LocalDate.now().minusDays(1).toString();
            Map<String, Object> beans = new HashMap<>();

            countDto.setCompany(robot.getCompany());
            countDto.setMonth(String.valueOf(lastDate.getMonth().getValue()));
            countDto.setTotalCount(String.valueOf(robot.getCarCount()));
            countDto.setDay(String.valueOf(lastDate.getDayOfMonth()));
            countDto.setAliveTotal(reportDtos.stream().map(ReportDto::getVehicleNo).collect(Collectors.toSet()).size());
            countDto.setFxCount(reportDtos.stream().filter(reportDto -> StringUtils.isEmpty(reportDto.getType1())).map(ReportDto::getVehicleNo).collect(Collectors.toSet()).size());
            countDto.setCzCount((int) reportDtos.stream().filter(reportDto -> StringUtils.isEmpty(reportDto.getType1())).count());
            countDto.setManCount(countDto.getFxCount());

            // 追加序号
            int len = reportDtos.size();
            for (int i = 0; i < len; i++) {
                reportDtos.get(i).setIndex(i + 1);
            }

            beans.put("countDto", countDto);
            beans.put("vehicleList", reportDtos);
            InputStream is = null;
            OutputStream os = null;
            // 写出文件
            File saveFile = new File(System.getProperty("user.dir"), robot.getCompany() + "GPS监控表.xls");
            try {
                // 获取模板文件
                is = this.getClass().getClassLoader().getResourceAsStream("GPS监控表.xls");
                // 实例化 XLSTransformer 对象
                XLSTransformer xlsTransformer = new XLSTransformer();
                // 获取 Workbook ，传入 模板 和 数据
                Workbook workbook = xlsTransformer.transformXLS(is, beans);
                os = new BufferedOutputStream(new FileOutputStream(saveFile));
                // 输出
                workbook.write(os);
                // 关闭和刷新管道，不然可能会出现表格数据不齐，打不开之类的问题
            } catch (Exception ignored) {

            } finally {
                if (is != null) {
                    is.close();
                }
                if (os != null) {
                    os.flush();
                    os.close();
                }
            }
            if (!saveFile.exists()) {
                continue;
            }
            String mailText = "详情见附件\n";
            if (reportDtos.size() == 0) {
                mailText += "<p><span style=\"font-size: 36px; color: rgb(255, 0, 0);\">请注意：GPS监控没有数据！</span><span style=\"font-size: 36px; color: rgb(255, 0, 0);\"></span></p>";
            }
            // 发送邮件
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper;
            try {
                helper = new MimeMessageHelper(message, true);
                String fileName = robot.getCompany() + "GPS监控表";
                //true代表支持多组件，如附件，图片等
                helper.setFrom(from);
                helper.setTo(emailArray);
                helper.setSubject(date + "-" + fileName);
                helper.setCc(ccEmail);
                helper.setText(mailText, true);
                FileSystemResource file = new FileSystemResource(saveFile);
                fileName = fileName + ".xls";
                helper.addAttachment(fileName, file);//添加附件，可多次调用该方法添加多个附件
                boolean hasSend = false;
                for (int i = 0; i < 3; i++) {
                    try {
                        mailSender.send(message);
                        logger.info("这些账号发送成功：" + email);
                        hasSend = true;
                        break;
                    } catch (Exception exception) {
                        exception.printStackTrace();
                        logger.info(exception.getLocalizedMessage());
                        Thread.sleep(2000);
                    }
                }
                if (!hasSend) {
                    // 没发送成功，就加入到未发送成功的列表
                    emailList.add(robot.getCompany() + ":" + email);
                    logger.info("这些账号发送失败：" + email);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            // 删除临时文件
            try {
                saveFile.delete();
            } catch (Exception ignored) {
            }
        }
        logger.info("发送动作完成");
        logger.info(String.format("没有发送成功的邮箱：%s", emailList.toString()));
        if (emailList.size() > 0) {
            // 需要创建一条微信发消息任务通知管理员
            if (StringUtils.hasText(wechats)) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("以下邮箱没有成功发送邮件").append("\n");
                for (int i = 0; i < emailList.size(); i++) {
                    stringBuilder.append(i + 1).append(",").append(emailList.get(i)).append("\n");
                }
                String[] strings = wechats.split(",");
                for (String wechat : strings) {
                    // 判断是否存在报警记录，存在则更新
                    List<ScreenShotTask> screenShotTasks = screenShotTaskRepository.findByWechatAndStatus(wechat, TypeStringUtils.wechat_status5);
                    if (screenShotTasks.size() > 0) {
                        ScreenShotTask screenShotTask = screenShotTasks.get(0);
                        screenShotTask.setContent(stringBuilder.toString());
                        screenShotTaskRepository.save(screenShotTask);
                    } else {
                        ScreenShotTask screenShotTask = new ScreenShotTask();
                        screenShotTask.setId(snowFlakeFactory.nextId("ST"));
                        screenShotTask.setWechat(wechat);
                        screenShotTask.setVehicleNo("");
                        screenShotTask.setOwnerWechat("anqin1588");
                        screenShotTask.setWxid(wechat);
                        screenShotTask.setOwner("");
                        screenShotTask.setStatus(TypeStringUtils.wechat_status5);
                        screenShotTask.setContent(stringBuilder.toString());
                        screenShotTaskRepository.save(screenShotTask);
                    }
                }
            }
        }
    }


    public void sendAttachmentMail(String to, String subject, String content, String filePath) {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper;
        try {
            helper = new MimeMessageHelper(message, true);
            //true代表支持多组件，如附件，图片等
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, true);
            if (filePath != null) {
                FileSystemResource file = new FileSystemResource(new File(filePath));
                String fileName = file.getFilename();
                helper.addAttachment(fileName, file);//添加附件，可多次调用该方法添加多个附件
            }
            mailSender.send(message);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }
}
