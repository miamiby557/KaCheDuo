package com.szcinda.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface RobotRepository extends JpaRepository<Robot, String>, JpaSpecificationExecutor<Robot> {
    List<Robot> findByOwnerAndParentIdIsNull(String owner);

    List<Robot> findByOwner(String owner);

    Robot findByPhone(String phone);

    Robot findByCompanyAndParentIdIsNull(String company);

    Robot findByAccount2(String phone);

    Robot findById(String id);

    List<Robot> findByParentId(String id);

    List<Robot> findByParentIdIsNull();

    List<Robot> findByType(String type);

    Robot findByParentIdAndType(String parentId, String type);
}
