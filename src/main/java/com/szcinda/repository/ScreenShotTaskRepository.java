package com.szcinda.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ScreenShotTaskRepository extends JpaRepository<ScreenShotTask, String>, JpaSpecificationExecutor<ScreenShotTask> {
    ScreenShotTask findFirstByOwnerWechat(String ownerWechat);
}
