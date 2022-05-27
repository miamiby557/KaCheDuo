package com.szcinda.service.chagang;

import com.szcinda.repository.ChaGangRecord;
import com.szcinda.service.PageResult;

import java.util.List;

public interface ChaGangService {
    void create(ChaGangCreateDto createDto);

    List<ChaGangDto> query();

    void alive(String account);

    void delete(String id);

    void createRecord(ChaGangRecordCreateDto recordCreateDto);

    PageResult<ChaGangRecord> query(RecordQueryDto queryDto);
}
