package com.szcinda.service.driver;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class UpdateDriverInfo implements Serializable {
    private String owner;
    private List<String> data; // 微信ID
}
