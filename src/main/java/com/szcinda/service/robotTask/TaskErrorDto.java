package com.szcinda.service.robotTask;

import lombok.Data;

import java.io.Serializable;

@Data
public class TaskErrorDto implements Serializable {
    private String id;
    private String message;
}
