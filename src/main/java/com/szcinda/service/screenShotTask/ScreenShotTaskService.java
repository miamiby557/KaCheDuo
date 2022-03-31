package com.szcinda.service.screenShotTask;

import com.szcinda.repository.HistoryScreenShotTask;
import com.szcinda.repository.ScreenShotTask;
import com.szcinda.service.PageResult;

import java.util.List;


public interface ScreenShotTaskService {
    void finish(String id);

    void error(ScreenShotTaskErrorDto dto);

    List<ScreenShotTask> queryRunning(ScreenShotTaskParams params);

    PageResult<HistoryScreenShotTask> query(ScreenShotTaskParams params);
}
