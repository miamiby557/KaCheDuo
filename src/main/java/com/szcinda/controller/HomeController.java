package com.szcinda.controller;

import com.szcinda.repository.Driver;
import com.szcinda.repository.DriverRepository;
import com.szcinda.service.ScheduleService;
import com.szcinda.service.callback.CallParams;
import com.szcinda.service.callback.CallService;
import com.szcinda.service.mail.SendMailService;
import com.szcinda.service.robot.RobotGroupDto;
import com.szcinda.service.robot.RobotService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("home")
public class HomeController {

    private final RobotService robotService;

    private final SendMailService sendMailService;
    private final DriverRepository driverRepository;
    private final CallService callService;
    private final ScheduleService scheduleService;

    @Value("${body.tired.id}")
    private String tiredId;

    @Value("${over.speed.id}")
    private String overSpeedId;

    public HomeController(RobotService robotService, SendMailService sendMailService, DriverRepository driverRepository,
                          CallService callService, ScheduleService scheduleService) {
        this.robotService = robotService;
        this.sendMailService = sendMailService;
        this.driverRepository = driverRepository;
        this.callService = callService;
        this.scheduleService = scheduleService;
    }


    @GetMapping("/")
    public Result<String> test() {
        return Result.success(LocalDateTime.now().toString());
    }

    @GetMapping("querySelf/{owner}")
    public Result<List<RobotGroupDto>> querySelf(@PathVariable String owner) {
        return Result.success(robotService.querySelf(owner));
    }

    @GetMapping("testSendMail")
    public Result<String> testSendMail() {
        try {
            sendMailService.sendMail();
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return Result.success();
    }

    @GetMapping("sendMailOnceCompany/{id}")
    public Result<String> sendMailOnceCompany(@PathVariable String id) {
        try {
            sendMailService.sendOnceCompanyEmail(id);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return Result.success();
    }

    @GetMapping("testSendMail2")
    public Result<String> testSendMail2() {
        try {
            sendMailService.testSend();
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return Result.success();
    }

    @GetMapping("testSendDataToApp")
    public Result<String> testSendDataToApp() {
        scheduleService.sendToApp();
        return Result.success();
    }

    @GetMapping("queryByAdmin")
    public Result<List<RobotGroupDto>> queryByAdmin() {
        return Result.success(robotService.group());
    }


    @GetMapping("testPhone/{vehicleNo}/{templateId}")
    public Result<String> testPhone(@PathVariable String vehicleNo, @PathVariable String templateId) {
        Driver driver = driverRepository.findByVehicleNo(vehicleNo);
        if (driver != null && StringUtils.hasText(driver.getPhone())) {
            CallParams callParams = new CallParams();
            callParams.setPhone(driver.getPhone());
            if (tiredId.equals(templateId) || overSpeedId.equals(templateId)) {
                callParams.setTemplateId(templateId);
            } else {
                callParams.setTemplateId(overSpeedId);
            }
            callParams.setFxId("test");
            callService.call(callParams);
        }
        return Result.success();
    }
}
