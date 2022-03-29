package com.szcinda.service.location;

import com.szcinda.repository.Location;
import com.szcinda.service.PageResult;

import java.util.List;

public interface LocationService {
    void create(CreateLocationDto dto);

    void batchCreate(List<CreateLocationDto> dtos);

    PageResult<LocationDto> query(LocationQuery query);
}
