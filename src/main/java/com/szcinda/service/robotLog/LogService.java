package com.szcinda.service.robotLog;

import com.szcinda.repository.RobotLog;
import com.szcinda.service.PageResult;

public interface LogService {
    void create(CreateLogDto logDto);
    PageResult<RobotLog> query(QueryLog queryLog);
}
