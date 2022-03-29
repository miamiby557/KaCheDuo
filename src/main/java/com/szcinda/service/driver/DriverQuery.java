package com.szcinda.service.driver;

import lombok.Data;

import java.io.Serializable;

@Data
public class DriverQuery implements Serializable {
    private String name;
    private String vehicleNo;
    private String owner;
}
