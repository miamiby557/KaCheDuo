package com.szcinda.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ChaGangRecordRepository extends JpaRepository<ChaGangRecord, String>, JpaSpecificationExecutor<ChaGangRecord> {
}
