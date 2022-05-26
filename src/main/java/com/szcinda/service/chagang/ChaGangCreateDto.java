package com.szcinda.service.chagang;

import lombok.Data;

import java.io.Serializable;

@Data
public class ChaGangCreateDto implements Serializable {
    private String company;
    private String account;
    private String pwd;
}
