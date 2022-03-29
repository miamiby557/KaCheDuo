package com.szcinda.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface FengXianRepository extends JpaRepository<FengXian, String>, JpaSpecificationExecutor<FengXian> {
    FengXian findByVehicleNoAndHappenTime(String vehicleNo, String happenTime);
}
