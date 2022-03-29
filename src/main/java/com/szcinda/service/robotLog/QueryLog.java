package com.szcinda.service.robotLog;

import com.szcinda.service.PageParams;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@EqualsAndHashCode(callSuper = true)
@Data
public class QueryLog extends PageParams {
    private String phone;
    private LocalDate createTimeStart;
    private LocalDate createTimeEnd;
    private String owner;
}
