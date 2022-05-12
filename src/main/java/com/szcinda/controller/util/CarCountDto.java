package com.szcinda.controller.util;

import lombok.Data;

import java.io.Serializable;

@Data
public class CarCountDto implements Serializable {
    private String userName;
    private int count;
}
