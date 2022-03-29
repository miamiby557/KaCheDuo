package com.szcinda.service.location;

import com.szcinda.service.PageParams;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@EqualsAndHashCode(callSuper = true)
@Data
public class LocationQuery extends PageParams {
    private String vehicleNo;
    private LocalDate happenTimeStart;
    private LocalDate happenTimeEnd;
    private LocalDate createTimeStart;
    private LocalDate createTimeEnd;
    private String owner;
    private String userName;
}
