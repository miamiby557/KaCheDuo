package com.szcinda.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface RobotLogRepository extends JpaRepository<RobotLog, String>, JpaSpecificationExecutor<RobotLog> {
}
