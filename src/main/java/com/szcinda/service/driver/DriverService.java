package com.szcinda.service.driver;

import java.util.List;

public interface DriverService {
    void importDriver(List<DriverImportDto> driverImportDtos, String owner);

    void delete(String id);

    List<DriverDto> query(DriverQuery query);
}
