package com.szcinda.service.user;

import com.szcinda.service.PageParams;
import lombok.Data;

@Data
public class UserQueryDto extends PageParams {
    private String account;
}
