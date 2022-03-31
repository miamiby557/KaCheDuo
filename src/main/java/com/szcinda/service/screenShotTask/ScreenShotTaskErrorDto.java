package com.szcinda.service.screenShotTask;

import lombok.Data;

import java.io.Serializable;

@Data
public class ScreenShotTaskErrorDto implements Serializable {
    private String id;
    private String message;
}
