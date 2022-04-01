package com.szcinda.service.screenShotTask;

import com.szcinda.repository.HistoryScreenShotTask;
import com.szcinda.repository.ScreenShotTask;
import com.szcinda.service.PageResult;

import java.util.List;


public interface ScreenShotTaskService {

    void error(ScreenShotTaskErrorDto dto);

    List<ScreenShotTask> queryRunning(ScreenShotTaskParams params);

    PageResult<HistoryScreenShotTaskDto> query(ScreenShotTaskParams params);

    ScreenShotTask findOneMission(String ownerWechat);

    ScreenShotTask findOneSendMission(String ownerWechat);

    void finishSend(String screenShotId);
}
