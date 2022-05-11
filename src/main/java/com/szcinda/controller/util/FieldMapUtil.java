package com.szcinda.controller.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FieldMapUtil {
    public static Map<String, String> fxMap(){
        Map<String, String> map = new HashMap<>();

        map.put("车牌号码", "vehicleNo");
        map.put("车牌颜色", "vehicleColor");
        map.put("所属地区", "area");
        map.put("第三方监控机构", "thirdOrg");
        map.put("当前驾驶员", "currentDriver");
        map.put("经营范围", "businessScope");
        map.put("风险类型", "dangerType");
        map.put("风险等级", "dangerLevel");
        map.put("行车速度km/h", "speed");
        map.put("发生时间", "happenTime");
        map.put("发生位置", "happenPlace");
        map.put("创建时间", "gdCreateTime");
        map.put("处置时间", "disposeTime");
        map.put("微信通知时间", "messageSendTime");
        map.put("微信回复时间", "messageReceiveTime");
        map.put("处理时间", "chuLiTime");
        map.put("外呼时间", "callTime");
        map.put("接通时长", "seconds");
        map.put("登录账号", "owner");
        map.put("所属公司", "company");
        return map;
    }

    public static List<String> fxColumns(){
        List<String> columns = new ArrayList<>();
        columns.add("车牌号码");
        columns.add("车牌颜色");
        columns.add("所属地区");
        columns.add("第三方监控机构");
        columns.add("当前驾驶员");
        columns.add("经营范围");
        columns.add("风险类型");
        columns.add("风险等级");
        columns.add("行车速度km/h");
        columns.add("发生时间");
        columns.add("发生位置");
        columns.add("创建时间");
        columns.add("处置时间");
        columns.add("微信通知时间");
        columns.add("微信回复时间");
        columns.add("处理时间");
        columns.add("外呼时间");
        columns.add("接通时长");
        columns.add("登录账号");
        columns.add("所属公司");

        return columns;
    }
}
