package com.szcinda.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface ScreenShotTaskRepository extends JpaRepository<ScreenShotTask, String>, JpaSpecificationExecutor<ScreenShotTask> {
    ScreenShotTask findFirstByOwnerWechatAndStatus(String ownerWechat, String status);
    ScreenShotTask findFirstByOwnerWechatAndStatusIn(String ownerWechat, List<String> statusList);
    List<ScreenShotTask> findByWechatAndStatus(String vehicleNo, String status);
    List<ScreenShotTask> findByStatus(String status);
    List<ScreenShotTask> findByVehicleNo(String vehicleNo);
}
