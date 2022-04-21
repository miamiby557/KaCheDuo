package com.szcinda.service.robotTask;

import com.szcinda.repository.HistoryTask;
import com.szcinda.repository.RobotTask;
import com.szcinda.service.PageResult;

import java.util.List;

public interface RobotTaskService {
    PageResult<HistoryTask> query(RobotTaskQuery taskQuery);

    List<RobotTask> queryRunningTask(RobotTaskQuery taskQuery);
    void create(CreateRobotTaskDto dto);
    void run(String id);
    void error(TaskErrorDto errorDto);
    void finish(String id);
    List<RobotTaskDto> getStandByList();
    boolean checkIsStandby(String userName);

    void createTask(String userName);

    void reRun(String id);

    void createAllUserNameTask();

    void release(String userName);

    void lock(String userName);

    void reRunHistoryTask(String id);

    List<RobotTaskDto> getOneLocationMission();
}
