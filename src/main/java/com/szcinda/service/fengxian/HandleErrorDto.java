package com.szcinda.service.fengxian;

import lombok.Data;

import java.io.Serializable;

@Data
public class HandleErrorDto implements Serializable {
    private String id;
    private String message;
}
