package com.szcinda.controller;

import com.szcinda.service.mail.SendMailService;
import com.szcinda.service.robot.RobotGroupDto;
import com.szcinda.service.robot.RobotService;
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

    public HomeController(RobotService robotService, SendMailService sendMailService) {
        this.robotService = robotService;
        this.sendMailService = sendMailService;
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

    @GetMapping("queryByAdmin")
    public Result<List<RobotGroupDto>> queryByAdmin() {
        return Result.success(robotService.group());
    }


}
