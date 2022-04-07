package com.szcinda.repository;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkRobotRepository extends JpaRepository<WorkRobot, String> {
    WorkRobot findByUserName(String userName);

    void deleteByUserName(String userName);
}
