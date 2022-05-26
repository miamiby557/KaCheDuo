package com.szcinda.service.chagang;

import java.util.List;

public interface ChaGangService {
    void create(ChaGangCreateDto createDto);

    List<ChaGangDto> query();

    void alive(String account);

    void delete(String id);
}
