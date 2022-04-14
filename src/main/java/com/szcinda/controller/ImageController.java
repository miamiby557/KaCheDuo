package com.szcinda.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;

@RequestMapping("image")
@RestController
public class ImageController {

    @Value("${file.save.path}")
    private String savePath;

    @GetMapping("{date}/{fileName:.+}")
    public void getImage(@PathVariable String date, @PathVariable String fileName, HttpServletResponse response) {
        File file = new File(savePath, date);
        if (file.exists()) {
            file = new File(file, fileName);
            if (file.exists()) {
                // 实现文件下载
                byte[] buffer = new byte[1024];
                FileInputStream fis = null;
                BufferedInputStream bis = null;
                try {
                    // 配置文件下载
                    response.setHeader("content-type", "application/octet-stream");
                    response.setContentType("application/octet-stream");
                    // 下载文件能正常显示中文
                    response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(fileName, "UTF-8"));
                    fis = new FileInputStream(file);
                    bis = new BufferedInputStream(fis);
                    OutputStream os = response.getOutputStream();
                    int i = bis.read(buffer);
                    while (i != -1) {
                        os.write(buffer, 0, i);
                        i = bis.read(buffer);
                    }
                } catch (Exception ignored) {
                } finally {
                    if (bis != null) {
                        try {
                            bis.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if (fis != null) {
                        try {
                            fis.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }
}
