package com.szcinda.service.callback;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.szcinda.controller.Result;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.tomcat.util.codec.binary.Base64;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

public class VoiceApi {
    private static final String ACCOUNT_SID = "c51b0661f537b79fdcd56f38b0079a40";
    private static final String AUTH_TOKEN = "ac38ea6e5dbf1ac4fa4a0bed2fc47bcb";
    private static final String DATE_FORMAT = "yyyyMMddHHmmss";
    private static final String BASE_URL = "https://api.139130.com:9999/api/v1.0.0";
    private static final String CAPTCHA_URL = "/voice/verify";
    private static final String NOTIFICATION_URL = "/voice/notify";
    private static final String CLICK_CALL_URL = "/voice/clickcall";
    private static final String SIG_PARAM_STR = "?sig=";
    private static final String JSON_CONTENT_TYPE = "application/json;charset=utf-8";


    private static final ObjectMapper objectMapper = new ObjectMapper();


    public static void main(String[] args) throws Exception {
        try {
//            sendVoiceCaptcha();
            CallParams callParams = new CallParams();
            callParams.setPhone("13427990185");
            callParams.setDataId("test");
            callParams.setTemplateId("27667");
            Result<String> result = sendVoiceNotification(callParams);
            System.out.println(result);
//            sendClickToCall();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*语音通知DEMO*/
    public static Result<String> sendVoiceNotification(CallParams callParams) {
        HttpURLConnection conn = null;
        DataOutputStream output = null;
        try {
            String timestamp = getTimeStamp();
            String sig = getSig(timestamp);
            String url = getUrl(sig);
            String authorization = getAuthorization(timestamp);
            String requestContent = getJsonRequestBody(callParams);
            conn = getConnection(url, authorization, requestContent.length());
            output = new DataOutputStream(conn.getOutputStream());
            output.write(requestContent.getBytes(StandardCharsets.UTF_8));
            output.close();
            printResponse(conn);
            return Result.success();
        } catch (Exception ignored) {

        } finally {
            if (conn != null) {
                conn.disconnect();
            }
            if (output != null) {
                try {
                    output.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return Result.fail("通话失败");
    }


    /*获取yyyyMMddHHmmss格式时间戳*/
    private static String getTimeStamp() {
        return new SimpleDateFormat(DATE_FORMAT).format(new Date());
    }

    /*获取sig字符串*/
    private static String getSig(String timestamp) {
        return DigestUtils.sha1Hex(ACCOUNT_SID + AUTH_TOKEN + timestamp).toLowerCase();
    }

    /*获取请求完整URL*/
    private static String getUrl(String sig) {
        return BASE_URL + VoiceApi.NOTIFICATION_URL + SIG_PARAM_STR + sig;
    }

    /*获取Authorization*/
    private static String getAuthorization(String timestamp) {
        return Base64.encodeBase64String((ACCOUNT_SID + ":" + timestamp).getBytes());
    }

    /*获取完整HttpURLConnection对象*/
    private static HttpURLConnection getConnection(String url, String authorization, int length) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", JSON_CONTENT_TYPE);
        conn.setRequestProperty("Accept", JSON_CONTENT_TYPE);
        conn.setRequestProperty("Authorization", authorization);
        conn.setRequestProperty("Content-Length", length + "");
        return conn;
    }

    /*获取请求body*/
    private static String getJsonRequestBody(CallParams callParams) throws Exception {
        String appId = "6442bb77ee95b693a412f0a6ab8c28ae";
        String templateId = callParams.getTemplateId();
        String called = callParams.getPhone();
        String calledDisplay = "";

        Map<String, Object> request = new HashMap<>();

        Map<String, Object> info = new HashMap<>();
        info.put("appID", appId);

        Map<String, Object> subject = new HashMap<>();
        subject.put("called", called);
        subject.put("calledDisplay", calledDisplay);
        subject.put("templateID", templateId);

        if (callParams.getParams() != null) {
            subject.put("params", callParams.getParams());
        }
        subject.put("playTimes", 3);
        request.put("info", info);
        request.put("subject", subject);
        request.put("data", callParams.getDataId());
        request.put("timestamp", String.valueOf(System.currentTimeMillis()));

        return objectMapper.writeValueAsString(request);
    }

    /*打印请求响应结果*/
    private static void printResponse(HttpURLConnection conn) throws IOException {
        StringBuilder response = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String tmp;
        while ((tmp = reader.readLine()) != null) {
            System.out.println(tmp);
            response.append(tmp);
        }
    }
}
