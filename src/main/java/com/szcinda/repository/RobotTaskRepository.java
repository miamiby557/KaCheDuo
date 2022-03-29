package com.szcinda.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface RobotTaskRepository extends JpaRepository<RobotTask, String>, JpaSpecificationExecutor<RobotTask> {
    RobotTask findById(String id);
    List<RobotTask> findByTaskStatus(String taskStatus);
    List<RobotTask> findByUserNameAndTaskTypeAndTaskStatusIn(String userName, String taskType, List<String> status);
}
