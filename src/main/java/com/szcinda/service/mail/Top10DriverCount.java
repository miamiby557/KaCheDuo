package com.szcinda.service.mail;

import lombok.Data;

import java.io.Serializable;

@Data
public class Top10DriverCount implements Serializable {
    public int index;
    public String no;
    public Long count;
}
