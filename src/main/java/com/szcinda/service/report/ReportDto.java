package com.szcinda.service.report;

import lombok.Data;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.time.LocalDateTime;

@Data
public class ReportDto implements Serializable {
    public int index;
    public String vehicleNo = ""; // 车牌号
    public String vehicleType = ""; // 车型
    public String checkTime = ""; // 抽查时间
    public String deviceStatus = "正常"; // 设备是否正常
    public String speed = "";// 车速
    public String message = "";// 报警内容
    public String location = "";// 车辆所在位置
    public String type1 = ""; // 正常行驶
    public String type2 = ""; // 抽烟
    public String type3 = ""; // 接打电话
    public String type4 = ""; // 玩手机
    public String type5 = ""; // 超速
    public String type6 = ""; // 生理疲劳
    public String type7 = ""; // 未带安全带
    public String type8 = ""; // 双手脱离方向盘
    public String type9 = ""; // 掉线
    public String type10 = ""; // 其他异常
    public String handleText = "";// 处理
    public String handleResult = "";// 处理结果

    private LocalDateTime happenTime;

    public void setSpeed(String speed) {
        if (StringUtils.hasText(speed)) {
            this.speed = speed.toLowerCase().replace("km/h", "");
        }
    }

    public void setType(int type) {
        try {
            for (int i = 1; i < 11; i++) {
                String fieldString = "type" + type;
                Field field = ReportDto.class.getDeclaredField(fieldString);
                field.setAccessible(true);
                if (type == i) {
                    field.set(this, "√");
                    break;
                } else {
                    field.set(this, "");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
