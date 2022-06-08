package com.szcinda.service.mail;

import lombok.Data;
import org.springframework.util.StringUtils;

import java.io.Serializable;

@Data
public class CountFx2Dto implements Serializable {
    public long osCount = 0;
    public double osCountP = 0;
    public long smkCount = 0;
    public double smkCountP = 0;
    public long hpCount = 0;
    public double hpCountP = 0;
    public long tiredCount = 0;
    public double tiredCountP = 0;
    public long ppCount = 0;
    public double ppCountP = 0;

    public long firstCount = 0;
    public double firstCountP = 0;
    public long secondCount = 0;
    public double secondCountP = 0;
    public long thirdCount = 0;
    public double thirdCountP = 0;

    public long levelCount = 0;


    public void addLevelCount(String levelType) {
        if(StringUtils.isEmpty(levelType)){
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
