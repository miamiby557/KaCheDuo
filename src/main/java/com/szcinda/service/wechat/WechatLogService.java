package com.szcinda.service.wechat;

import com.szcinda.repository.WechatLog;
import com.szcinda.service.PageResult;

public interface WechatLogService {
    void create(LogCreateDto createDto);

    PageResult<WechatLog> query(WechatLogQueryDto queryDto);
}
