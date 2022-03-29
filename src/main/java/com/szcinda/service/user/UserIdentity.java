package com.szcinda.service.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serializable;

@Data
public class UserIdentity implements Serializable {
    private String id;
    private String account;
    private String token;
    @JsonIgnore
    private String password;
    private String company;
    private boolean admin;

    public UserIdentity(String id, String account, String password, String company, boolean isAdmin) {
        this.id = id;
        this.account = account;
        this.password = password;
        this.company = company;
        this.admin = isAdmin;
    }
}
