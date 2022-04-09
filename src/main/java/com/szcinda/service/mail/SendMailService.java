package com.szcinda.service.mail;

import com.szcinda.repository.*;
import com.szcinda.service.report.ReportDto;
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
import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
                                if (((new BigDecimal(speed)).compareTo(BigDecimal.ZERO))> 0) {
                                    location = l;
                                    break;
                                }
                            }
                        } catch (Exception ignored) {

                        }
                    }
                    if(location == null){
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
                reportDtos.add(reportDto);
            });
            // 写入文件

            // 发送邮件
            // 删除临时文件
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
