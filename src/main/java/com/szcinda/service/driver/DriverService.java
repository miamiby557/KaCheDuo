package com.szcinda.service.driver;

import com.szcinda.repository.Driver;
import com.szcinda.service.PageResult;

import java.util.List;

public interface DriverService {
    void importDriver(List<DriverImportDto> driverImportDtos, String owner);

    void delete(String id);

    PageResult<DriverDto> query(DriverQuery query);

    void connect(DriverConnectDto connectDto);

    void savePic(DriverScreenShotDto shotDto);

    // APP接收到图片后生成处理任务
    void generateChuliMissionFromAppUpload(String vehicleNo, String filePath);

    void confirm(String wechat);

    void update(DriverUpdateDto updateDto);
    List<DriverDto> queryNoWechat(String owner);

    List<DriverDto> queryNotFriend(String owner);

    void updateInfo(UpdateDriverInfo driverInfo);
}
