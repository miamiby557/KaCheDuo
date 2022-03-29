package com.szcinda.repository;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.Entity;

@EqualsAndHashCode(callSuper = true)
@Data
@Entity
public class Robot extends BaseEntity{
    private String company;
    private String phone;
    private String pwd;
    private String account2;//处理、位置监控 用到此账号
    private String pwd2;
    private String parentId;
    private String owner;
    private boolean run = true;
}
