package com.szcinda.service.user;

import lombok.Data;

import java.io.Serializable;

@Data
public class UpdatePWDDto implements Serializable {
    private String id;
    private String oldPwd;
    private String newPwd;
    private String password;
}
