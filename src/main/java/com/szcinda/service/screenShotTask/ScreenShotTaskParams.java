package com.szcinda.service.screenShotTask;

import com.szcinda.service.PageParams;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@EqualsAndHashCode(callSuper = true)
@Data
public class ScreenShotTaskParams extends PageParams {
    private String vehicleNo;
    private LocalDate createTimeStart;
    private LocalDate createTimeEnd;
    private String owner;
}
