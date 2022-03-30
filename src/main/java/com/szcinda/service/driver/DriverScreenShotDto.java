package com.szcinda.service.driver;

import lombok.Data;

import java.io.Serializable;

@Data
public class DriverScreenShotDto implements Serializable {
    private String fxId;
    private String filePath;
}
