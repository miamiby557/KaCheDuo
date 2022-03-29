package com.szcinda.service.user;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserDto implements Serializable {
    private String id;
    private String account;
    private String company;
    private String password;
    private String wechat;
}
