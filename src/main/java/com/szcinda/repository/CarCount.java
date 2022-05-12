package com.szcinda.repository;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.Entity;
import java.time.LocalDate;

@EqualsAndHashCode(callSuper = true)
@Data
@Entity
public class CarCount extends BaseEntity{
    private String account;
    private String company;
    private LocalDate date;
    private int count;
}
