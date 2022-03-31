package com.szcinda.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface HistoryScreenShotTaskRepository extends JpaRepository<HistoryScreenShotTask, String>, JpaSpecificationExecutor<HistoryScreenShotTask> {
}
