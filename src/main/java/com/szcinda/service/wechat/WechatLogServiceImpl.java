package com.szcinda.service.wechat;

import com.szcinda.repository.WechatLog;
import com.szcinda.repository.WechatLogRepository;
import com.szcinda.service.PageResult;
import com.szcinda.service.SnowFlakeFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class WechatLogServiceImpl implements WechatLogService {
    private final WechatLogRepository wechatLogRepository;
    private final SnowFlakeFactory snowFlakeFactory;

    public WechatLogServiceImpl(WechatLogRepository wechatLogRepository) {
        this.wechatLogRepository = wechatLogRepository;
        this.snowFlakeFactory = SnowFlakeFactory.getInstance();
    }

    @Override
    public void create(LogCreateDto createDto) {
        WechatLog wechatLog = new WechatLog();
        BeanUtils.copyProperties(createDto, wechatLog);
        wechatLog.setId(snowFlakeFactory.nextId("WL"));
        wechatLogRepository.save(wechatLog);
    }

    @Override
    public PageResult<WechatLog> query(WechatLogQueryDto params) {
        Specification<WechatLog> specification = ((root, criteriaQuery, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (!StringUtils.isEmpty(params.getWxid())) {
                Predicate wxid = criteriaBuilder.equal(root.get("wxid"), params.getWxid());
                predicates.add(wxid);
            }
            if (!StringUtils.isEmpty(params.getWechat())) {
                Predicate wechat = criteriaBuilder.equal(root.get("wechat"), params.getWechat());
                predicates.add(wechat);
            }
            if (params.getCreateTimeStart() != null) {
                Predicate createTimeStart = criteriaBuilder.greaterThanOrEqualTo(root.get("createTime"), params.getCreateTimeStart().atStartOfDay());
                predicates.add(createTimeStart);
            }
            if (params.getCreateTimeEnd() != null) {
                Predicate createTimeEnd = criteriaBuilder.lessThan(root.get("createTime"), params.getCreateTimeEnd().plusDays(1).atStartOfDay());
                predicates.add(createTimeEnd);
            }
            if (!StringUtils.isEmpty(params.getContent())) {
                Predicate content = criteriaBuilder.like(root.get("content"), "%" + params.getContent() + "%");
                predicates.add(content);
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        });
        Sort order = new Sort(Sort.Direction.DESC, "createTime");
        Pageable pageable = new PageRequest(params.getPage() - 1, params.getPageSize(), order);
        Page<WechatLog> details = wechatLogRepository.findAll(specification, pageable);
        return PageResult.of(details.getContent(), params.getPage(), params.getPageSize(), details.getTotalElements());
    }
}
