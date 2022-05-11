package com.szcinda.service.fengxian;

import com.szcinda.repository.FengXian;
import com.szcinda.service.PageResult;

import java.util.List;

public interface FengXianService {
    FengXian create(CreateFengXianDto dto);

    void batchCreate(List<CreateFengXianDto> dtos);

    void chuZhi(ChuZhiDto chuZhiDto);

    PageResult<ChuZhiDetailDto> query(ChuZhiQuery query);

    void finish(String id);

    void error(HandleErrorDto errorDto);

    void generateScreenShotMissions();

    void batchCreateHandle(List<CreateFengXianDto> dtos);

    List<FengXian> queryAll(ChuZhiQuery params);
}
