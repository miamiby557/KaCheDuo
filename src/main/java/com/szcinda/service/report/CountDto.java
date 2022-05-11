package com.szcinda.service.report;

import lombok.Data;

import java.io.Serializable;
import java.lang.reflect.Field;

@Data
public class CountDto implements Serializable {
    public int type1Count = 0;
    public int type2Count = 0;
    public int type3Count = 0;
    public int type4Count = 0;
    public int type5Count = 0;
    public int type6Count = 0;
    public int type7Count = 0;
    public int type8Count = 0;
    public int type9Count = 0;
    public int type10Count = 0;

    public String company = "";
    public String month = "";
    public String day = "";
    public String totalCount = "";
    public int aliveTotal = 0;
    public int fxCount = 0;
    public int czCount = 0;
    public int manCount = 0;

    public void addType(int type) {
        try {
            for (int i = 1; i < 11; i++) {
                String fieldString = "type" + type + "Count";
                Field field = CountDto.class.getDeclaredField(fieldString);
                field.setAccessible(true);
                if (type == i) {
                    field.set(this, (Integer) field.get(this) + 1);
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
