package com.szcinda.service.driver;

import com.szcinda.service.PageResult;

import java.util.List;

public interface DriverService {
    void importDriver(List<DriverImportDto> driverImportDtos, String owner);

    void delete(String id);

    PageResult<DriverDto> query(DriverQuery query);

    void connect(DriverConnectDto connectDto);

    void savePic(DriverScreenShotDto shotDto);

    void confirm(String wechat);

    void update(DriverUpdateDto updateDto);
}
