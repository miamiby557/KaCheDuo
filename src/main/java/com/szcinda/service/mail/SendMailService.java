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

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import java.io.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
            String[] emailArray = email.split(",");
            List<Robot> subRobotList = robots.stream().filter(item -> robot.getId().equals(item.getParentId())).collect(Collectors.toList());
            List<String> userNameList = subRobotList.stream().map(Robot::getPhone).collect(Collectors.toList());
            // 按照车牌分组
            Map<String, List<Location>> locationMap = locationList.stream().filter(location -> userNameList.contains(location.getOwner())).collect(Collectors.groupingBy(Location::getVehicleNo));
            // 一个车牌会有多条记录 如果是多条，则取其中一条速度不为0的
            List<ReportDto> reportDtos = new ArrayList<>();
            locationMap.forEach((vehicleNo, dataList) -> {
                ReportDto reportDto = new ReportDto();
                Location location = null;
                if (dataList.size() > 1) {
                    for (Location l : dataList) {
                        try {
                            String speed = l.getSpeed();
                            if (StringUtils.hasText(speed)) {
                                speed = speed.toLowerCase().replace("km/h", "");
                                if (((new BigDecimal(speed)).compareTo(BigDecimal.ZERO)) > 0) {
                                    location = l;
                                    break;
                                }
                            }
                        } catch (Exception ignored) {

                        }
                    }
                    if (location == null) {
                        location = dataList.get(0);
                    }
                } else {
                    location = dataList.get(0);
                }
                // 找到速度大于0的一条记录
                reportDto.setVehicleNo(vehicleNo);
                reportDto.setLocation(location.getHappenPlace());
                reportDto.setCheckTime(checkTime);
                reportDto.setSpeed(location.getSpeed());
                reportDto.setVehicleType("重型货车");
                reportDto.setMessage("");
                reportDtos.add(reportDto);
            });
            // 写入文件
            String date = LocalDate.now().toString();
            Map<String, Object> beans = new HashMap<>();
            beans.put("date", date);
            beans.put("time", checkTime);
            beans.put("carCount", reportDtos.size());
            beans.put("vehicleList", reportDtos);
            InputStream is = null;
            OutputStream os = null;
            // 写出文件
            String filePath = System.getProperty("user.dir") + File.separator + "GPS监控表.xls";
            try {
                // 获取模板文件
                is = this.getClass().getClassLoader().getResourceAsStream("GPS监控表.xls");
                // 实例化 XLSTransformer 对象
                XLSTransformer xlsTransformer = new XLSTransformer();
                // 获取 Workbook ，传入 模板 和 数据
                Workbook workbook = xlsTransformer.transformXLS(is, beans);
                os = new BufferedOutputStream(new FileOutputStream(filePath));
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
            File saveFile = new File(filePath);
            if (!saveFile.exists()) {
                continue;
            }
            // 发送邮件
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper;
            for (String em : emailArray) {
                try {
                    helper = new MimeMessageHelper(message, true);
                    //true代表支持多组件，如附件，图片等
                    helper.setFrom(from);
                    helper.setTo(em);
                    helper.setSubject(date + "GPS监控表");
                    helper.setText("详情见附件", false);
                    FileSystemResource file = new FileSystemResource(saveFile);
                    String fileName = file.getFilename();
                    helper.addAttachment(fileName, file);//添加附件，可多次调用该方法添加多个附件
                    mailSender.send(message);
                    // 删除临时文件
                    try {
                        saveFile.delete();
                    } catch (Exception ignored) {
                    }
                } catch (MessagingException e) {
                    e.printStackTrace();
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
            FileSystemResource file = new FileSystemResource(new File(filePath));
            String fileName = file.getFilename();
            helper.addAttachment(fileName, file);//添加附件，可多次调用该方法添加多个附件
            mailSender.send(message);
        } catch (Exception ignored) {
        }


    }
}
