package com.szcinda.repository;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.Entity;
import java.time.LocalDate;

@EqualsAndHashCode(callSuper = true)
@Data
@Entity
public class ChaGang extends BaseEntity {
    private String company;
    private String account;
    private String pwd;
}
