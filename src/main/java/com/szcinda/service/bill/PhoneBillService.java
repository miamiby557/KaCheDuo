package com.szcinda.service.bill;

import com.szcinda.repository.PhoneBill;
import com.szcinda.service.PageResult;

public interface PhoneBillService {
    PageResult<PhoneBill> query(BillQuery query);
}
