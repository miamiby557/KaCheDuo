package com.szcinda.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ChaGangRepository extends JpaRepository<ChaGang, String>, JpaSpecificationExecutor<ChaGang> {
    ChaGang findByAccount(String account);
}
