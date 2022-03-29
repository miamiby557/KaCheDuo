package com.szcinda.service.robotLog;

import lombok.Data;

import java.io.Serializable;

@Data
public class CreateLogDto implements Serializable {
    private String phone;
    private String content;
}
