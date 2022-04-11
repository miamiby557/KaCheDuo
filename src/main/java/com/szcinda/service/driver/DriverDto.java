package com.szcinda.service.driver;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class DriverDto implements Serializable {
    public String id;
    public String name;
    public String phone;
    public String vehicleNo;
    public String company;
    public String wechat;
    public String wxid;
    public String owner;
    public String ownerWechat;// 来源微信号，就是哪个微信下的好友

    public static Map<String, String> getFieldMap() {
        Map<String, String> map = new HashMap<>();
        map.put("司机姓名", "name");
        map.put("联系电话", "phone");
        map.put("车牌号码", "vehicleNo");
        map.put("公司名称", "company");
        map.put("微信号(截图)", "wechat");
        map.put("微信ID(发消息)", "wxid");
        map.put("发消息的微信号", "ownerWechat");
        return map;
    }


    public static List<String> getFieldList() {
        List<String> list = new ArrayList<>();
        list.add("司机姓名");
        list.add("联系电话");
        list.add("车牌号码");
        list.add("公司名称");
        list.add("微信号(截图)");
        list.add("微信ID(发消息)");
        list.add("发消息的微信号");
        return list;
    }
}
