package com.szcinda.service.chagang;

import com.szcinda.repository.*;
import com.szcinda.service.PageResult;
import com.szcinda.service.SnowFlakeFactory;
import com.szcinda.service.TypeStringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import javax.persistence.criteria.Predicate;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@Transactional
public class ChaGangServiceImpl implements ChaGangService {

    private final ChaGangRepository chaGangRepository;
    private final ChaGangRecordRepository chaGangRecordRepository;
    private final ScreenShotTaskRepository screenShotTaskRepository;
    private final SnowFlakeFactory snowFlakeFactory = SnowFlakeFactory.getInstance();

    private static List<ChaGang> accountList = new ArrayList<>();


    // 记录上一次发送微信提醒时间
    private static final ConcurrentHashMap<String, LocalDateTime> lastMap = new ConcurrentHashMap<>();

    // 管理员微信号
    @Value("${admin.user.wechat}")
    private String wechats;

    private final ConcurrentHashMap<String, LocalDateTime> chaGangMap = new ConcurrentHashMap<>();

    public ChaGangServiceImpl(ChaGangRepository chaGangRepository, ChaGangRecordRepository chaGangRecordRepository, ScreenShotTaskRepository screenShotTaskRepository) {
        this.chaGangRepository = chaGangRepository;
        this.chaGangRecordRepository = chaGangRecordRepository;
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
        accountList.clear();
    }

    @Override
    public List<ChaGangDto> query() {
        List<ChaGang> chaGangs = chaGangRepository.findAll();
        List<ChaGangDto> dtos = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (ChaGang chaGang : chaGangs) {
            ChaGangDto dto = new ChaGangDto();
            BeanUtils.copyProperties(chaGang, dto);
            if (chaGangMap.containsKey(chaGang.getAccount())) {
                Duration duration = Duration.between(now, chaGangMap.get(chaGang.getAccount()));
                long minutes = Math.abs(duration.toMinutes());//相差的分钟数
                dto.setAlive(minutes <= 5);
            }
            dto.setLastTime(chaGangMap.get(chaGang.getAccount()));
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
        accountList.clear();

    }

    @Override
    public void createRecord(ChaGangRecordCreateDto recordCreateDto) {
        ChaGangRecord chaGangRecord = new ChaGangRecord();
        BeanUtils.copyProperties(recordCreateDto, chaGangRecord);
        chaGangRecord.setId(snowFlakeFactory.nextId("CG"));
        chaGangRecordRepository.save(chaGangRecord);
    }


    @Override
    public PageResult<ChaGangRecord> query(RecordQueryDto params) {
        Specification<ChaGangRecord> specification = ((root, criteriaQuery, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(params.getAnswerUser())) {
                Predicate answerUser = criteriaBuilder.like(root.get("answerUser"), "%" + params.getAnswerUser() + "%");
                predicates.add(answerUser);
            }
            if (params.getCreateTimeStart() != null) {
                Predicate timeStart = criteriaBuilder.greaterThanOrEqualTo(root.get("createTime"), params.getCreateTimeStart().atStartOfDay());
                predicates.add(timeStart);
            }
            if (params.getCreateTimeEnd() != null) {
                Predicate timeEnd = criteriaBuilder.lessThan(root.get("createTime"), params.getCreateTimeEnd().plusDays(1).atStartOfDay());
                predicates.add(timeEnd);
            }
            if (params.getInquireTimeStart() != null) {
                Predicate timeStart = criteriaBuilder.greaterThanOrEqualTo(root.get("inquireTime"), params.getInquireTimeStart().atStartOfDay());
                predicates.add(timeStart);
            }
            if (params.getInquireTimeEnd() != null) {
                Predicate timeEnd = criteriaBuilder.lessThan(root.get("inquireTime"), params.getInquireTimeEnd().plusDays(1).atStartOfDay());
                predicates.add(timeEnd);
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        });
        Sort order = new Sort(Sort.Direction.DESC, "createTime");
        Pageable pageable = new PageRequest(params.getPage() - 1, params.getPageSize(), order);
        Page<ChaGangRecord> details = chaGangRecordRepository.findAll(specification, pageable);
        return PageResult.of(details.getContent(), params.getPage(), params.getPageSize(), details.getTotalElements());
    }

    public List<ChaGang> getList() {
        if (accountList.size() == 0) {
            accountList = chaGangRepository.findAll();
        }
        return accountList;
    }

    @Scheduled(cron = "0/30 * * * * ?")
    public void checkAlive() {
        LocalDateTime now = LocalDateTime.now();
        List<String> alarmAccountList = new ArrayList<>();
        List<ChaGang> list = getList();
        for (ChaGang chaGang : list) {
            if (chaGangMap.containsKey(chaGang.getAccount())) {
                Duration duration = Duration.between(now, chaGangMap.get(chaGang.getAccount()));
                long minutes = Math.abs(duration.toMinutes());//相差的分钟数
                if (minutes > 5) {
                    alarmAccountList.add(chaGang.getAccount());
                }
            } else {
                alarmAccountList.add(chaGang.getAccount());
            }
        }
        // 过滤15分钟前发送过的账号记录
        alarmAccountList = alarmAccountList.stream().filter(account -> {
            if (!lastMap.containsKey(account)) {
                return true;
            }
            Duration duration = Duration.between(now, lastMap.get(account));
            long minutes = Math.abs(duration.toMinutes());//相差的分钟数
            return minutes > 15;
        }).collect(Collectors.toList());
        if (alarmAccountList.size() > 0) {
            String[] strings = wechats.split(",");
            for (String wechat : strings) {
                ScreenShotTask screenShotTask = new ScreenShotTask();
                screenShotTask.setId(snowFlakeFactory.nextId("ST"));
                screenShotTask.setWechat(wechat);
                screenShotTask.setVehicleNo("");
                screenShotTask.setOwnerWechat("anqin1588");
                screenShotTask.setWxid(wechat);
                screenShotTask.setOwner("");
                screenShotTask.setStatus(TypeStringUtils.wechat_status5);
                screenShotTask.setContent(String.format("当前时间【%s】查岗账号【%s】已下线，请运维赶紧处理", now.toString(), String.join(",", alarmAccountList)));
                screenShotTaskRepository.save(screenShotTask);
            }
            for (String account : alarmAccountList) {
                // 记录这一次发送的时间
                lastMap.put(account, LocalDateTime.now());
            }
        }
    }
}
