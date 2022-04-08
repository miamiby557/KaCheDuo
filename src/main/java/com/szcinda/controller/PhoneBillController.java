package com.szcinda.controller;

import com.szcinda.repository.PhoneBill;
import com.szcinda.service.PageResult;
import com.szcinda.service.bill.BillQuery;
import com.szcinda.service.bill.PhoneBillService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("phoneBill")
public class PhoneBillController {

    private final PhoneBillService phoneBillService;

    public PhoneBillController(PhoneBillService phoneBillService) {
        this.phoneBillService = phoneBillService;
    }


    @PostMapping("query")
    public PageResult<PhoneBill> query(@RequestBody BillQuery query) {
        return phoneBillService.query(query);
    }
}
