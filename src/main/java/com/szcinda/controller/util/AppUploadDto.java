package com.szcinda.controller.util;

import lombok.Data;

import java.io.Serializable;

@Data
public class AppUploadDto implements Serializable {
    private String vehicleNo;
    private String filePath;
}
