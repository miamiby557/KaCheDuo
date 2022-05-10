package com.szcinda.service.mail;

import com.szcinda.repository.*;
import com.szcinda.service.report.ReportDto;
import net.sf.jxls.transformer.XLSTransformer;
import org.apache.poi.ss.usermodel.Workbook;
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

import static com.szcinda.service.TypeStringUtils.over_status;
import static com.szcinda.service.TypeStringUtils.tired_status;

@Component
public class SendMailService {


    @Value("${spring.mail.username}")
    private String from;

    @Autowired
    //用于发送文件
    private JavaMailSender mailSender;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private RobotRepository robotRepository;

    @Autowired
    private FengXianRepository fengXianRepository;

    public void testSend() {
        sendAttachmentMail("huangrensen@uniner.com", "报表", "报表测试", null);
    }

    // 发送一次公司的邮件
    public void sendOnceCompanyEmail(String id){
        // 主账号
        Robot robot = robotRepository.findById(id);
        // 子账号
        List<Robot> robots = robotRepository.findByParentId(robot.getId());
        String checkTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH时mm分"));
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
        String email = robot.getEmail();
        if (StringUtils.isEmpty(email)) {
            return;
        }
        // 取出子账号
        List<String> accountList = robots.stream().filter(robot1 -> robot1.getParentId() != null && robot1.getParentId().equals(robot.getId())).map(Robot::getPhone).collect(Collectors.toList());
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
        // 替换中文符号
        email = email.replace("，", ",");
        String[] emailArray = email.split(",");
        List<Robot> subRobotList = robots.stream().filter(item -> robot.getId().equals(item.getParentId())).collect(Collectors.toList());
        List<String> userNameList = subRobotList.stream().map(Robot::getPhone).collect(Collectors.toList());
        // 按照车牌分组
        Map<String, List<Location>> locationMap = locationList.stream().filter(location -> userNameList.contains(location.getOwner())).collect(Collectors.groupingBy(Location::getVehicleNo));
        List<ReportDto> reportDtos = new ArrayList<>();
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
                reportDtos.add(reportDto);
            }
            // 不过滤疲劳驾驶或者超速
            List<FengXian> fengXians = fengXianList.stream().filter(fengXian -> fengXian.getVehicleNo().equals(vehicleNo))
                    .collect(Collectors.toList());
            if (fengXians.size() > 0) {
                for (FengXian fengXian : fengXians) {
                    ReportDto reportDto = new ReportDto();
                    reportDto.setVehicleNo(vehicleNo);
                    reportDto.setLocation(fengXian.getHappenPlace());
                    reportDto.setCheckTime(checkTime);
                    reportDto.setSpeed(fengXian.getSpeed());
                    reportDto.setVehicleType("重型货车");
                    if (tired_status.equals(fengXian.getDangerType())) {
                        reportDto.setMessage("疲劳报警");
                        reportDto.setHandleResult("通知司机停车休息");
                    } else if (over_status.equals(fengXian.getDangerType())) {
                        reportDto.setMessage("超速报警");
                        reportDto.setHandleResult("通知司机降低车速");
                    } else {
                        reportDto.setMessage(fengXian.getDangerType());
                        reportDto.setHandleResult("通知司机注意安全驾驶");
//                            if("接打手机报警".equals(fengXian.getDangerType())){
//                                reportDto.setHandleResult("通知司机注意安全驾驶");
//                            }else if("玩手机报警".equals(fengXian.getDangerType())){
//                                reportDto.setHandleResult("通知司机注意安全驾驶");
//                            }else if("抽烟报警".equals(fengXian.getDangerType())){
//                                reportDto.setHandleResult("通知司机注意安全驾驶");
//                            }
                    }
                    if (fengXian.getChuLiTime() != null) {
                        try {
                            reportDto.setHandleText(fengXian.getChuLiTime().format(DateTimeFormatter.ofPattern("HH时mm分")) + "已下发语音信息通知");
                        } catch (Exception exception) {
                            exception.printStackTrace();
                        }
                    }
                    reportDtos.add(reportDto);
                }
            }
        });
        // 写入文件
        String date = LocalDate.now().toString();
        Map<String, Object> beans = new HashMap<>();
        beans.put("date", date);
        beans.put("time", checkTime);
        beans.put("carCount", reportDtos.stream().map(ReportDto::getVehicleNo).collect(Collectors.toSet()).size());
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
        for (String em : emailArray) {
            try {
                helper = new MimeMessageHelper(message, true);
                //true代表支持多组件，如附件，图片等
                helper.setFrom(from);
                helper.setTo(em.trim());
                helper.setSubject(date + "-" + robot.getCompany() + "GPS监控表");
                helper.setText(mailText, true);
                FileSystemResource file = new FileSystemResource(saveFile);
                helper.addAttachment("GPS监控表.xls", file);//添加附件，可多次调用该方法添加多个附件
                for (int i = 0; i < 3; i++) {
                    try {
                        mailSender.send(message);
                        break;
                    } catch (Exception exception) {
                        exception.printStackTrace();
                        Thread.sleep(2000);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // 删除临时文件
        try {
            saveFile.delete();
        } catch (Exception ignored) {
        }
    }


    @Scheduled(cron = "0 0 8 * * ?")
    public void sendMail() throws Exception {
        List<Robot> robots = robotRepository.findAll();
        String checkTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH时mm分"));
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
            // 替换中文符号
            email = email.replace("，", ",");
            String[] emailArray = email.split(",");
            List<Robot> subRobotList = robots.stream().filter(item -> robot.getId().equals(item.getParentId())).collect(Collectors.toList());
            List<String> userNameList = subRobotList.stream().map(Robot::getPhone).collect(Collectors.toList());
            // 按照车牌分组
            Map<String, List<Location>> locationMap = locationList.stream().filter(location -> userNameList.contains(location.getOwner())).collect(Collectors.groupingBy(Location::getVehicleNo));
            List<ReportDto> reportDtos = new ArrayList<>();
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
                    reportDtos.add(reportDto);
                }
                // 不过滤疲劳驾驶或者超速
                List<FengXian> fengXians = fengXianList.stream().filter(fengXian -> fengXian.getVehicleNo().equals(vehicleNo))
                        .collect(Collectors.toList());
                if (fengXians.size() > 0) {
                    for (FengXian fengXian : fengXians) {
                        ReportDto reportDto = new ReportDto();
                        reportDto.setVehicleNo(vehicleNo);
                        reportDto.setLocation(fengXian.getHappenPlace());
                        reportDto.setCheckTime(checkTime);
                        reportDto.setSpeed(fengXian.getSpeed());
                        reportDto.setVehicleType("重型货车");
                        if (tired_status.equals(fengXian.getDangerType())) {
                            reportDto.setMessage("疲劳报警");
                            reportDto.setHandleResult("通知司机停车休息");
                        } else if (over_status.equals(fengXian.getDangerType())) {
                            reportDto.setMessage("超速报警");
                            reportDto.setHandleResult("通知司机降低车速");
                        } else {
                            reportDto.setMessage(fengXian.getDangerType());
                            reportDto.setHandleResult("通知司机注意安全驾驶");
//                            if("接打手机报警".equals(fengXian.getDangerType())){
//                                reportDto.setHandleResult("通知司机注意安全驾驶");
//                            }else if("玩手机报警".equals(fengXian.getDangerType())){
//                                reportDto.setHandleResult("通知司机注意安全驾驶");
//                            }else if("抽烟报警".equals(fengXian.getDangerType())){
//                                reportDto.setHandleResult("通知司机注意安全驾驶");
//                            }
                        }
                        if (fengXian.getChuLiTime() != null) {
                            try {
                                reportDto.setHandleText(fengXian.getChuLiTime().format(DateTimeFormatter.ofPattern("HH时mm分")) + "已下发语音信息通知");
                            } catch (Exception exception) {
                                exception.printStackTrace();
                            }
                        }
                        reportDtos.add(reportDto);
                    }
                }
            });
            // 写入文件
            String date = LocalDate.now().toString();
            Map<String, Object> beans = new HashMap<>();
            beans.put("date", date);
            beans.put("time", checkTime);
            beans.put("carCount", reportDtos.stream().map(ReportDto::getVehicleNo).collect(Collectors.toSet()).size());
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
            for (String em : emailArray) {
                try {
                    helper = new MimeMessageHelper(message, true);
                    //true代表支持多组件，如附件，图片等
                    helper.setFrom(from);
                    helper.setTo(em.trim());
                    helper.setSubject(date + "-" + robot.getCompany() + "GPS监控表");
                    helper.setText(mailText, true);
                    FileSystemResource file = new FileSystemResource(saveFile);
                    helper.addAttachment("GPS监控表.xls", file);//添加附件，可多次调用该方法添加多个附件
                    for (int i = 0; i < 3; i++) {
                        try {
                            mailSender.send(message);
                            break;
                        } catch (Exception exception) {
                            exception.printStackTrace();
                            Thread.sleep(2000);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            // 删除临时文件
            try {
                saveFile.delete();
            } catch (Exception ignored) {
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
