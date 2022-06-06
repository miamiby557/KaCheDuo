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


    public void addTypeCount(String type) {
        switch (type) {
            case "超速报警":
                osCount++;
                break;
            case "生理疲劳报警":
                tiredCount++;
                break;
            case "玩手机报警":
                ppCount++;
                break;
            case "接打手机报警":
                hpCount++;
                break;
            case "抽烟报警":
                smkCount++;
                break;
            case "双手脱把报警":
                stbCount++;
                break;
            case "超时驾驶报警":
                otCount++;
                break;
            case "不目视前方报警":
                bmsCount++;
                break;
            case "设备遮挡失效报警":
                sbtdCount++;
                break;
            case "DSM自动抓拍事件":
                zpCount++;
                break;
            case "车道偏离报警":
                plCount++;
                break;
            case "车距过近报警":
                gjCount++;
                break;
            case "车辆碰撞报警":
                pzCount++;
                break;
            case "驾驶员身份识别":
                idCardCount++;
                break;
            case "偏离驾驶位报警":
                pljswCount++;
                break;
            case "其他":
                otherCount++;
                break;
            case "设备通讯失效报警":
                unliveCount++;
                break;
        }
    }

    // 原始警情每天数量
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

    //风险类型每天数量
    public long mon1 = 0;
    public long tue1 = 0;
    public long wed1 = 0;
    public long thur1 = 0;
    public long fri1 = 0;
    public long sat1 = 0;
    public long sun1 = 0;


    public void setWeekDayValue2(int index) {
        switch (index) {
            case 0:
                mon1++;
                break;
            case 1:
                tue1++;
                break;
            case 2:
                wed1++;
                break;
            case 3:
                thur1++;
                break;
            case 4:
                fri1++;
                break;
            case 5:
                sat1++;
                break;
            case 6:
                sun1++;
                break;
        }
    }
}
