package com.szcinda.service.user;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserCreateDto implements Serializable {
    private String account;
    private String company;
    private String password;
    private String wechat;
}
