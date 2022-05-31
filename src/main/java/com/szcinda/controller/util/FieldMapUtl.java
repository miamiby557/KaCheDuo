package com.szcinda.controller.util;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FieldMapUtl {

    public static List<Item> getFXFieldMap() {
        List<Item> fieldMap = new ArrayList<>();
        fieldMap.add(new Item("vehicleNo", "车牌号"));
        fieldMap.add(new Item("vehicleColor", "车牌颜色"));
        fieldMap.add(new Item("area", "所属地区"));
        fieldMap.add(new Item("thirdOrg", "第三方监控机构"));
        fieldMap.add(new Item("currentDriver", "当前驾驶员"));
        fieldMap.add(new Item("businessScope", "经营范围"));
        fieldMap.add(new Item("dangerType", "风险类型"));
        fieldMap.add(new Item("dangerLevel", "风险等级"));
        fieldMap.add(new Item("speed", "行车速度KM/H"));
        fieldMap.add(new Item("happenTime", "发生时间"));
        fieldMap.add(new Item("happenPlace", "发生位置"));
        fieldMap.add(new Item("gdCreateTime", "两客创建时间"));
        fieldMap.add(new Item("owner", "所属账号"));
        fieldMap.add(new Item("company", "所属公司"));
        fieldMap.add(new Item("disposeTime", "处置时间"));
        fieldMap.add(new Item("messageSendTime", "微信通知时间"));
        fieldMap.add(new Item("messageReceiveTime", "微信回复时间"));
        fieldMap.add(new Item("chuLiTime", "处理时间"));
        fieldMap.add(new Item("callTime", "外呼时间"));
        fieldMap.add(new Item("called", "是否接通"));
        fieldMap.add(new Item("phone", "外呼号码"));
        fieldMap.add(new Item("hangUpTime", "接通时间"));
        fieldMap.add(new Item("seconds", "接通时长"));
        return fieldMap;
    }

    @Data
    static
    public class Item implements Serializable {
        private String field; // 数据库字段
        private String label; // 中文名称

        public Item(String field, String label) {
            this.field = field;
            this.label = label;
        }
    }
}


