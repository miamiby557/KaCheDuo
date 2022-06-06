package com.szcinda.service.mail;

import lombok.Data;

import java.io.Serializable;

@Data
public class CountFxDto implements Serializable {
    public long osCount = 0;
    public long tiredCount = 0;
    public long ppCount = 0;
    public long hpCount = 0;
    public long smkCount = 0;
    public long stbCount = 0;
    public long otCount = 0;
    public long bmsCount = 0;
    public long sbtdCount = 0;
    public long zpCount = 0;
    public long plCount = 0;
    public long gjCount = 0;
    public long pzCount = 0;
    public long idCardCount = 0;
    public long pljswCount = 0;
    public long otherCount = 0;
    public long unliveCount = 0;


    public long mon = 0;
    public long tue = 0;
    public long wed = 0;
    public long thur = 0;
    public long fri = 0;
    public long sat = 0;
    public long sun = 0;

    public void setWeekDayValue(int index, long val) {
        switch (index) {
            case 0:
                mon = val;
                break;
            case 1:
                tue = val;
                break;
            case 2:
                wed = val;
                break;
            case 3:
                thur = val;
                break;
            case 4:
                fri = val;
                break;
            case 5:
                sat = val;
                break;
            case 6:
                sun = val;
                break;
        }
    }
}
