package com.szcinda.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;

public interface CarCountRepository extends JpaRepository<CarCount, String>, JpaSpecificationExecutor<CarCount> {
    CarCount findByAccountAndDate(String account, LocalDate date);
}
