package com.szcinda.service.robotTask;

import com.szcinda.repository.RobotTask;
import com.szcinda.service.PageResult;

import java.util.List;

public interface RobotTaskService {
    PageResult<RobotTask> query(RobotTaskQuery taskQuery);
    void create(CreateRobotTaskDto dto);
    void run(String id);
    void error(TaskErrorDto errorDto);
    void finish(String id);
    List<RobotTask> getStandByList();
    boolean checkIsStandby(String userName);

    void createTask(String userName);

    void reRun(String id);

    void createAllUserNameTask();
}
