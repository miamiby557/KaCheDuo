package com.szcinda.service.fengxian;

import com.szcinda.service.PageResult;

import java.util.List;

public interface FengXianService {
    void create(CreateFengXianDto dto);

    void batchCreate(List<CreateFengXianDto> dtos);

    void chuZhi(ChuZhiDto chuZhiDto);

    PageResult<ChuZhiDetailDto> query(ChuZhiQuery query);

    void finish(String id);

    void error(HandleErrorDto errorDto);

    void generateScreenShotMissions();
}
