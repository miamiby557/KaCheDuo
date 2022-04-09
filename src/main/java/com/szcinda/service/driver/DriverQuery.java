package com.szcinda.service.driver;

import com.szcinda.service.PageParams;
import lombok.Data;

import java.io.Serializable;

@Data
public class DriverQuery extends PageParams {
    private String name;
    private String vehicleNo;
    private String owner;
}
