package com.szcinda.service.robot;

import com.szcinda.service.PageParams;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class QueryRobotParams extends PageParams {
    private String owner;
    private String company;
}
