package com.szcinda.service;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class PostAppDataDto implements Serializable {
    private String vehicleNo;
    private String company;
    private String phone;

    private List<Item> dangerList;

    @Data
    public static class Item implements Serializable {
        private String id;
        private String vehicleColor;
        private String area;
        private String thirdOrg;
        private String currentDriver;
        private String businessScope;
        private String dangerType;
        private String dangerLevel;
        private String speed;
        private String happenTime;
        private String happenPlace;
        private String gdCreateTime;
    }

}


