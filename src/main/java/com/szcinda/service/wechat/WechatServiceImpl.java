package com.szcinda.service.wechat;

import com.szcinda.repository.Wechat;
import com.szcinda.repository.WechatRepository;
import com.szcinda.service.ScheduleService;
import com.szcinda.service.SnowFlakeFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class WechatServiceImpl implements WechatService {
    private final WechatRepository wechatRepository;
    private final SnowFlakeFactory snowFlakeFactory;

    public WechatServiceImpl(WechatRepository wechatRepository) {
        this.wechatRepository = wechatRepository;
        this.snowFlakeFactory = SnowFlakeFactory.getInstance();
    }

    @Override
    public void create(String no) {
        Wechat wechat = new Wechat();
        wechat.setNo(no);
        wechat.setId(snowFlakeFactory.nextId("WX"));
        wechatRepository.save(wechat);
    }

    @Override
    public void delete(String id) {
        wechatRepository.delete(id);
    }

    @Override
    public List<Wechat> query() {
        return wechatRepository.findAll();
    }

    @Override
    public void sync(String no) {
        if (!ScheduleService.needSyncFriendWechatList.contains(no)) {
            ScheduleService.needSyncFriendWechatList.add(no);
        }
    }
}
