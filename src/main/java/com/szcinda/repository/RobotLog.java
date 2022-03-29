package com.szcinda.repository;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.Entity;

@EqualsAndHashCode(callSuper = true)
@Data
@Entity
public class RobotLog extends BaseEntity{
    private String phone;
    private String company;
    private String content;
}
