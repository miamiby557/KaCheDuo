package com.szcinda.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface WechatLogRepository extends JpaRepository<WechatLog, String>, JpaSpecificationExecutor<WechatLog> {
}
