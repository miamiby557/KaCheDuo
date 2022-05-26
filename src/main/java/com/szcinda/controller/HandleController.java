package com.szcinda.controller;

import com.szcinda.controller.util.AppUploadDto;
import com.szcinda.controller.util.DownPrams;
import com.szcinda.service.driver.DriverService;
import com.szcinda.service.robotTask.RobotTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RequestMapping("handle")
@RestController
public class HandleController {

    private final static Logger logger = LoggerFactory.getLogger(HandleController.class);

    private final RobotTaskService robotTaskService;
    private final DriverService driverService;

    @Value("${file.save.path}")
    private String savePath;

    public HandleController(RobotTaskService robotTaskService, DriverService driverService) {
        this.robotTaskService = robotTaskService;
        this.driverService = driverService;
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

    @PostMapping("appUpload")
    public Result<String> appUpload(@RequestBody AppUploadDto appUploadDto) {
        Assert.isTrue(StringUtils.hasText(appUploadDto.getVehicleNo()), "参数【车牌号】不能为空");
        Assert.isTrue(StringUtils.hasText(appUploadDto.getFilePath()), "参数【图片链接】不能为空");
        logger.info("APP接收到参数：" + appUploadDto.toString());
        driverService.generateChuliMissionFromAppUpload(appUploadDto.getVehicleNo(), appUploadDto.getFilePath());
        return Result.success();
    }

    @RequestMapping(value = "upload", method = RequestMethod.POST)
    public Result<String> driverAppUpload(HttpServletRequest request) {
        MultipartHttpServletRequest params = ((MultipartHttpServletRequest) request);
        String vehicleNo = params.getParameter("vehicleNo");
        MultipartFile file = ((MultipartHttpServletRequest) request).getFile("file");
        //保存文件
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        String dataStr = LocalDateTime.now().format(dateTimeFormatter);
        File path = new File(savePath, dataStr);
        if (!path.exists()) {
            path.mkdirs();
        }
        String fileName = file.getOriginalFilename();//获取文件名（包括后缀）
        String extension = fileName.substring(fileName.lastIndexOf("."));
        fileName = UUID.randomUUID().toString().replaceAll("-", "").toUpperCase() + extension;
        File saveFile = new File(path, fileName);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(saveFile);
            fos.write(file.getBytes()); // 写入文件
            fos.flush();
            driverService.generateChuliMissionFromAppUpload(vehicleNo, dataStr + File.separator + fileName);
            return Result.success();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return Result.fail("保存文件失败");
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


    public void upload(String vehicleNo, File file) {
        String baseUrl = "http://localhost:9061/api/handle/upload";  // 本地测试
        /*String baseUrl = "http://175.178.222.14/:9061/api/handle/upload";  // 正式网址*/
        String CHARSET = "utf-8"; //设置编码
        // 参数
        Map<String, String> params = new HashMap<>();
        params.put("vehicleNo", "粤BK09U8");

        String BOUNDARY = UUID.randomUUID().toString(); //边界标识
        String PREFIX = "--", LINE_END = "\r\n";
        InputStream is = null;
        OutputStream out = null;
        HttpURLConnection conn = null;
        try {
            URL url = new URL(baseUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(30 * 1000);
            conn.setConnectTimeout(30 * 1000);
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Charset", CHARSET);
            conn.setRequestProperty("connection", "keep-alive");
            conn.setRequestProperty("Content-Type", "multipart/form-data" + ";boundary=" + BOUNDARY);
            out = conn.getOutputStream();
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, String> entry : params.entrySet()) {
                sb.append(PREFIX).append(BOUNDARY).append(LINE_END).append("Content-Disposition: form-data; name=\"").append(entry.getKey()).append("\"").append(LINE_END).append("Content-Type: text/plain; charset=").append(CHARSET).append(LINE_END).append("Content-Transfer-Encoding: 8bit").append(LINE_END)
                        .append(LINE_END).append(entry.getValue()).append(LINE_END);
            }
            sb.append(PREFIX).append(BOUNDARY).append(LINE_END).append("Content-Disposition: form-data; name=\"file\"; filename=\"").append(file.getName()).append("\"").append(LINE_END).append("Content-Type: application/octet-stream; charset=").append(CHARSET).append(LINE_END)
                    .append(LINE_END);
            out.write(sb.toString().getBytes(CHARSET));
            is = new FileInputStream(file);
            byte[] bytes = new byte[1024];
            int len;
            while ((len = is.read(bytes)) != -1) {
                out.write(bytes, 0, len);
            }
            out.write(LINE_END.getBytes());
            out.write((PREFIX + BOUNDARY + PREFIX + LINE_END).getBytes());
            out.flush();
            //得到响应流
            InputStream inputStream = conn.getInputStream();
            //获取响应
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
            reader.close();
            conn.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void main(String[] args) {
        File file = new File("E:\\卡车多\\处置\\20220411122128.png");
        String baseUrl = "http://localhost:9061/api/handle/upload";  // 本地测试
//        String baseUrl = "http://175.178.222.14/:9061/api/handle/upload";  // 正式网址
        // 参数
        Map<String, String> params = new HashMap<>();
        params.put("vehicleNo", "粤BK09U8");
        String CHARSET = "utf-8"; //设置编码
        String BOUNDARY = UUID.randomUUID().toString(); // 边界标识 随机生成
        String PREFIX = "--", LINE_END = "\r\n";
        InputStream is = null;
        OutputStream out = null;
        HttpURLConnection conn = null;
        try {
            URL url = new URL(baseUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(30 * 1000);
            conn.setConnectTimeout(30 * 1000);
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Charset", CHARSET);
            conn.setRequestProperty("connection", "keep-alive");
            conn.setRequestProperty("Content-Type", "multipart/form-data" + ";boundary=" + BOUNDARY);
            out = conn.getOutputStream();
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, String> entry : params.entrySet()) {
                sb.append(PREFIX).append(BOUNDARY).append(LINE_END).append("Content-Disposition: form-data; name=\"").append(entry.getKey()).append("\"").append(LINE_END).append("Content-Type: text/plain; charset=").append(CHARSET).append(LINE_END).append("Content-Transfer-Encoding: 8bit").append(LINE_END)
                        .append(LINE_END).append(entry.getValue()).append(LINE_END);
            }
            sb.append(PREFIX).append(BOUNDARY).append(LINE_END).append("Content-Disposition: form-data; name=\"file\"; filename=\"").append(file.getName()).append("\"").append(LINE_END).append("Content-Type: application/octet-stream; charset=").append(CHARSET).append(LINE_END)
                    .append(LINE_END);
            out.write(sb.toString().getBytes(CHARSET));
            is = new FileInputStream(file);
            byte[] bytes = new byte[1024];
            int len;
            while ((len = is.read(bytes)) != -1) {
                out.write(bytes, 0, len);
            }
            out.write(LINE_END.getBytes());
            out.write((PREFIX + BOUNDARY + PREFIX + LINE_END).getBytes());
            out.flush();
            //得到响应流
            InputStream inputStream = conn.getInputStream();
            //获取响应
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
            reader.close();
            conn.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
