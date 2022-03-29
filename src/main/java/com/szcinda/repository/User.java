package com.szcinda.repository;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.Entity;

@EqualsAndHashCode(callSuper = true)
@Data
@Entity
public class User extends BaseEntity {
    private String account;
    private String company;
    private String password;
    private String wechat;
}
