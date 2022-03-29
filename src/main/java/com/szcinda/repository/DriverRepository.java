package com.szcinda.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface DriverRepository extends JpaRepository<Driver, String>, JpaSpecificationExecutor<Driver> {
    Driver findByVehicleNo(String vehicleNo);
    List<Driver> findByOwner(String owner);
}
