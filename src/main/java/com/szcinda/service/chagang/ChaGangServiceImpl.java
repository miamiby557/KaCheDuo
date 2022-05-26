package com.szcinda.service.chagang;

import com.szcinda.repository.ChaGang;
import com.szcinda.repository.ChaGangRepository;
import com.szcinda.repository.ScreenShotTask;
import com.szcinda.repository.ScreenShotTaskRepository;
import com.szcinda.service.SnowFlakeFactory;
import com.szcinda.service.TypeStringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Transactional
public class ChaGangServiceImpl implements ChaGangService {

    private final ChaGangRepository chaGangRepository;
    private final ScreenShotTaskRepository screenShotTaskRepository;
    private final SnowFlakeFactory snowFlakeFactory = SnowFlakeFactory.getInstance();

    // 管理员微信号
    @Value("${admin.user.wechat}")
    private String wechats;

    private final ConcurrentHashMap<String, LocalDateTime> chaGangMap = new ConcurrentHashMap<>();

    public ChaGangServiceImpl(ChaGangRepository chaGangRepository, ScreenShotTaskRepository screenShotTaskRepository) {
        this.chaGangRepository = chaGangRepository;
        this.screenShotTaskRepository = screenShotTaskRepository;
    }

    @Override
    public void create(ChaGangCreateDto createDto) {
        ChaGang chaGang = chaGangRepository.findByAccount(createDto.getAccount());
        Assert.isTrue(chaGang == null, String.format("存在账号【%s】的查岗账号", createDto.getAccount()));
        chaGang = new ChaGang();
        BeanUtils.copyProperties(createDto, chaGang);
        chaGang.setId(snowFlakeFactory.nextId("CG"));
        chaGangRepository.save(chaGang);
    }

    @Override
    public List<ChaGangDto> query() {
        List<ChaGang> chaGangs = chaGangRepository.findAll();
        List<ChaGangDto> dtos = new ArrayList<>();
        for (ChaGang chaGang : chaGangs) {
            ChaGangDto dto = new ChaGangDto();
            BeanUtils.copyProperties(chaGang, dto);
            if (chaGangMap.containsKey(chaGang.getAccount())) {
                dto.setAlive(true);
                dto.setLastTime(chaGangMap.get(chaGang.getAccount()));
            }
            dtos.add(dto);
        }
        return dtos;
    }

    @Override
    public void alive(String account) {
        chaGangMap.put(account, LocalDateTime.now());
    }

    @Override
    public void delete(String id) {
        chaGangRepository.delete(id);
    }

    @Scheduled(cron = "0/30 * * * * ?")
    public void checkAlive() {
        List<String> deleteList = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        chaGangMap.forEach((account, time) -> {
            Duration duration = Duration.between(now, time);
            long minutes = Math.abs(duration.toMinutes());//相差的分钟数
            if (minutes > 5) {
                deleteList.add(account);
            }
        });
        if (deleteList.size() > 0) {
            String[] strings = wechats.split(",");
            for (String account : deleteList) {
                chaGangMap.remove(account);
                for (String wechat : strings) {
                    ScreenShotTask screenShotTask = new ScreenShotTask();
                    screenShotTask.setId(snowFlakeFactory.nextId("ST"));
                    screenShotTask.setWechat(wechat);
                    screenShotTask.setVehicleNo("");
                    screenShotTask.setOwnerWechat("anqin1588");
                    screenShotTask.setWxid(wechat);
                    screenShotTask.setOwner("");
                    screenShotTask.setStatus(TypeStringUtils.wechat_status5);
                    screenShotTask.setContent(String.format("当前时间【%s】查岗账号【%s】已下线，请运维赶紧处理", now.toString(), account));
                    screenShotTaskRepository.save(screenShotTask);
                }
            }
        }
    }
}
