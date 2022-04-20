package com.szcinda.service.wechat;

import com.szcinda.repository.Wechat;

import java.util.List;

public interface WechatService {
    void create(String no);
    void delete(String id);
    List<Wechat> query();
    void sync(String no);
}
