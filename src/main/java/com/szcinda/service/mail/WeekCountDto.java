package com.szcinda.service.mail;

import lombok.Data;

import java.io.Serializable;

@Data
public class WeekCountDto implements Serializable {
    public long carCount = 0;
    public long wgCount = 0;
    public long totalCount = 0;
    public long fxCount = 0;
    public long czCount = 0;
    public long phoneCount = 0;
    public long aqCount = 0;
    public long ccCount = 21;
    public long sendCount = 7;
}
