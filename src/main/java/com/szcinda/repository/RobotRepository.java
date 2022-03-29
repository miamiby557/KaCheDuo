package com.szcinda.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RobotRepository extends JpaRepository<Robot, String> {
    List<Robot> findByOwnerAndParentIdIsNull(String owner);
    List<Robot> findByOwner(String owner);
    Robot findByPhone(String phone);
    Robot findByAccount2(String phone);
    Robot findById(String id);
    List<Robot> findByParentId(String id);
    List<Robot> findByParentIdIsNull();
}
