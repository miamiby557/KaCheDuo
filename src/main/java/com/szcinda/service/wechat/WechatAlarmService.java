package com.szcinda.service.wechat;

import com.szcinda.repository.ScreenShotTask;
import com.szcinda.repository.ScreenShotTaskRepository;
import com.szcinda.service.SnowFlakeFactory;
import com.szcinda.service.TypeStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WechatAlarmService {


    // 管理员微信号
    @Value("${admin.user.wechat}")
    private String wechats;

    private static final ConcurrentHashMap<String, Integer> errorCountMap = new ConcurrentHashMap<>();


    // 获取错误次数
    public int geErrorCount(String account) {
        if (errorCountMap.containsKey(account)) {
            Integer count = errorCountMap.get(account);
            if (count != null) {
                return count;
            }
        }
        return 0;
    }

    // 累计一次错误
    public void plusError(String account) {
        if (errorCountMap.containsKey(account)) {
            Integer count = errorCountMap.get(account);
            if (count == null) {
                count = 1;
            } else {
                count += 1;
            }
            errorCountMap.put(account, count);
        } else {
            errorCountMap.put(account, 1);
        }
    }

    //  消除所有错误 一次成功就消除所有错误
    public void minusError(String account) {
        errorCountMap.put(account, 0);
    }

    @Autowired
    private ScreenShotTaskRepository screenShotTaskRepository;

    private final SnowFlakeFactory snowFlakeFactory = SnowFlakeFactory.getInstance();

    public void sendMsg(String msg) {
        if (StringUtils.isEmpty(wechats)) {
            return;
        }
        String[] strings = wechats.split(",");
        for (String wechat : strings) {
            // 判断是否存在报警记录，存在则更新
            List<ScreenShotTask> screenShotTasks = screenShotTaskRepository.findByWechatAndStatus(wechat, TypeStringUtils.wechat_status5);
            if (screenShotTasks.size() > 0) {
                ScreenShotTask screenShotTask = screenShotTasks.get(0);
                screenShotTask.setContent(msg);
                screenShotTaskRepository.save(screenShotTask);
            } else {
                ScreenShotTask screenShotTask = new ScreenShotTask();
                screenShotTask.setId(snowFlakeFactory.nextId("ST"));
                screenShotTask.setWechat(wechat);
                screenShotTask.setVehicleNo("");
                screenShotTask.setOwnerWechat("anqin1588");
                screenShotTask.setWxid(wechat);
                screenShotTask.setOwner("");
                screenShotTask.setStatus(TypeStringUtils.wechat_status5);
                screenShotTask.setContent(msg);
                screenShotTaskRepository.save(screenShotTask);
            }
        }
    }
}
