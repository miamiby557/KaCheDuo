package com.szcinda.controller;

import com.szcinda.controller.util.DownPrams;
import com.szcinda.service.robotTask.RobotTaskService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;

@RequestMapping("handle")
@RestController
public class HandleController {

    private final RobotTaskService robotTaskService;

    @Value("${file.save.path}")
    private String savePath;

    public HandleController(RobotTaskService robotTaskService) {
        this.robotTaskService = robotTaskService;
    }


    // 释放帐号
    @GetMapping("release/{userName}")
    public Result<String> release(@PathVariable String userName) {
        robotTaskService.release(userName);
        return Result.success();
    }

    // 锁定账号，不要同时处理和位置监控，因为用的是同一个账号，会互踢
    @GetMapping("lock/{userName}")
    public Result<String> lock(@PathVariable String userName) {
        robotTaskService.lock(userName);
        return Result.success();
    }


    @PostMapping("downloadFile")
    public void downloadFile(@RequestBody DownPrams downPrams, HttpServletResponse response) {
        File file = new File(savePath, downPrams.getFileName());
        String fName = URLEncoder.encode(file.getName());
        response.setHeader("Content-Disposition", "attachment;filename=" + fName);
        // 发送给客户端的数据
        byte[] buff = new byte[1024];
        BufferedInputStream bis = null;
        OutputStream outputStream = null;
        try {
            outputStream = response.getOutputStream();
            bis = new BufferedInputStream(new FileInputStream(file));
            int i = bis.read(buff);
            while (i != -1) {
                outputStream.write(buff, 0, buff.length);
                outputStream.flush();
                i = bis.read(buff);
            }
            outputStream.flush();
        } catch (Exception ignored) {
        } finally {
            try {
                assert bis != null;
                bis.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
            try {
                assert outputStream != null;
                outputStream.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }

}
