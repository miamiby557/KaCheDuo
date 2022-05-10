package com.szcinda.service.bill;

import com.szcinda.repository.PhoneBill;
import com.szcinda.service.PageResult;

import java.util.List;

public interface PhoneBillService {
    PageResult<PhoneBill> query(BillQuery query);

    List<PhoneBill> queryAll(BillQuery params);
}
