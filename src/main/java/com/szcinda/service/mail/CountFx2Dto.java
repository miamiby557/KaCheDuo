package com.szcinda.service.mail;

import lombok.Data;

import java.io.Serializable;

@Data
public class CountFx2Dto implements Serializable {
    public long osCount = 0;
    public long osCountP = 0;
    public long smkCount = 0;
    public long smkCountP = 0;
    public long hpCount = 0;
    public long hpCountP = 0;
    public long tiredCount = 0;
    public long tiredCountP = 0;
    public long ppCount = 0;
    public long ppCountP = 0;

    public long firstCount;
    public long firstCountP;
    public long secondCount;
    public long secondCountP;
    public long thirdCount;
    public long thirdCountP;
}
