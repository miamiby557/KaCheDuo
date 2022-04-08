package com.szcinda.service.callback;

import lombok.Data;

import java.io.Serializable;

@Data
public class CallbackData implements Serializable {
    private String notify;
    private CallbackData.Info info;
    private CallbackData.Subject subject;
    private String data;
    private String timestamp;


    @Data
    static class Subject implements Serializable {
        private String caller;
        private String called;
        private int business;
        private int ttsCount;
        private int ttsLength;
        private int ivrCount;
        private int ivrTime;
        private int duration;
        private double cost;
        private String recordFilename;
        private int recordSize;
        private String createTime;
        private String answerTime;
        private String releaseTime;
        private String dtmf;
        private int direction;
        private int callout;
        private int softCause;
    }

    @Data
    static class Info implements Serializable {
        private String appID;
        private String callID;
        private String sessionID;
    }
}
