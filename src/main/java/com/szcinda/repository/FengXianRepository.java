package com.szcinda.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface FengXianRepository extends JpaRepository<FengXian, String>, JpaSpecificationExecutor<FengXian> {
    FengXian findByVehicleNoAndHappenTime(String vehicleNo, String happenTime);
    FengXian findByVehicleNoAndHappenTimeAndChuZhiType(String vehicleNo, String happenTime, String chuZhiType);
    List<FengXian> findByChuLiType(String type);
    List<FengXian> findByVehicleNoAndChuLiType(String vehicle, String type);
}
