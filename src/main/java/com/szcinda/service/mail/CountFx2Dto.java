package com.szcinda.service.mail;

import lombok.Data;

import java.io.Serializable;

@Data
public class CountFx2Dto implements Serializable {
    public long osCount = 0;
    public String osCountP = "=B68/B65*100%";
    public long smkCount = 0;
    public String smkCountP = "=D68/B65*100%";
    public long hpCount = 0;
    public String hpCountP = "=F68/B65*100%";
    public long tiredCount = 0;
    public String tiredCountP = "=H68/B65*100%";
    public long ppCount = 0;
    public String ppCountP = "=J68/B65*100%";

    public long firstCount = 0;
    public String firstCountP = "=M68/M65*100%";
    public long secondCount = 0;
    public String secondCountP = "=O68/M65*100%";
    public long thirdCount = 0;
    public String thirdCountP = "=Q68/B65*100%";

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
