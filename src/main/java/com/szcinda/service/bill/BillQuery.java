package com.szcinda.service.bill;

import com.szcinda.service.PageParams;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@EqualsAndHashCode(callSuper = true)
@Data
public class BillQuery extends PageParams {
    private String phone;
    private String status;
    private LocalDate callTimeStart;
    private LocalDate callTimeEnd;

    private String vehicleNo;
    private String account;
    private String company;
}
