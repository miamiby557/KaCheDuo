package com.szcinda.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;

public interface DriverRepository extends JpaRepository<Driver, String>, JpaSpecificationExecutor<Driver> {
    Driver findByVehicleNo(String vehicleNo);
    List<Driver> findByOwner(String owner);
    List<Driver> findByVehicleNoIn(Collection<String> vehicleNos);
    Driver findByPhone(String phone);
    Driver findByWechat(String wechat);
    List<Driver> findByOwnerWechat(String ownerWechat);
}
