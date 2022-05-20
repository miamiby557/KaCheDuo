package com.szcinda.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class HttpUtil {

    private final static Logger logger = LoggerFactory.getLogger(HttpUtil.class);

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void post(PostAppDataDto params) {
        HttpClient client = new DefaultHttpClient();
        String app_url = "http://uat.quanroon.com/kcd-uif/client/carInfo/carIllegalInfo";
        HttpPost post = new HttpPost(app_url);
        post.setHeader("Content-Type", "application/json");
        String result = "";
        try {
            logger.info(String.format("发送数据到APP:%s", objectMapper.writeValueAsString(params)));
            StringEntity s = new StringEntity(objectMapper.writeValueAsString(params), "utf-8");
            s.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE,
                    "application/json"));
            post.setEntity(s);
            // 发送请求
            HttpResponse httpResponse = client.execute(post);

            // 获取响应输入流
            InputStream inStream = httpResponse.getEntity().getContent();
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    inStream, StandardCharsets.UTF_8));
            StringBuilder strber = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null)
                strber.append(line).append("\n");
            inStream.close();
            result = strber.toString();
            logger.info(String.format("APP端返回：%s", result));
            if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                logger.info("请求服务器成功，做相应处理");
            } else {
                logger.info("请求服务端失败");
            }
        } catch (Exception e) {
            logger.info(String.format("发送数据到APP异常:%s", e.getMessage()));
            throw new RuntimeException(e);
        }

    }

}
