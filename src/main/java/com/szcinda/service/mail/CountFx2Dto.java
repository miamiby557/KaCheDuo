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

    public long firstCount = 0;
    public long firstCountP = 0;
    public long secondCount = 0;
    public long secondCountP = 0;
    public long thirdCount = 0;
    public long thirdCountP = 0;

    public long levelCount = 0;


    public void addLevelCount(String levelType) {
        if(levelType == null){
            return;
        }
        switch (levelType) {
            case "一级风险":
                firstCount++;
                levelCount++;
                break;
            case "二级风险":
                secondCount++;
                levelCount++;
                break;
            case "三级风险":
                thirdCount++;
                levelCount++;
                break;
        }
    }
}
