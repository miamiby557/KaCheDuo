package com.szcinda.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PhoneBillRepository extends JpaRepository<PhoneBill, String>, JpaSpecificationExecutor<PhoneBill> {
}
