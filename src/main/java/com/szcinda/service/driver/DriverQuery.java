package com.szcinda.service.driver;

import com.szcinda.service.PageParams;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Data
public class DriverQuery extends PageParams {
    private String name;
    private String vehicleNo;
    private String owner;
    private String company;
    private Boolean friend;
}
