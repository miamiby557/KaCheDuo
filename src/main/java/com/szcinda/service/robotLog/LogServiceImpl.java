package com.szcinda.service.robotLog;

import com.szcinda.repository.*;
import com.szcinda.service.PageResult;
import com.szcinda.service.SnowFlakeFactory;
import com.szcinda.service.TypeStringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class LogServiceImpl implements LogService {
    private final RobotLogRepository robotLogRepository;
    private final SnowFlakeFactory snowFlakeFactory;
    private final RobotRepository robotRepository;
    private final RobotTaskRepository robotTaskRepository;

    public LogServiceImpl(RobotLogRepository robotLogRepository, RobotRepository robotRepository, RobotTaskRepository robotTaskRepository) {
        this.robotLogRepository = robotLogRepository;
        this.robotRepository = robotRepository;
        this.robotTaskRepository = robotTaskRepository;
        this.snowFlakeFactory = SnowFlakeFactory.getInstance();
    }

    @Override
    public void create(CreateLogDto logDto) {
        RobotLog log = new RobotLog();
        BeanUtils.copyProperties(logDto, log);
        log.setId(snowFlakeFactory.nextId("LG"));
        Robot robot;
        robot = robotRepository.findByPhone(logDto.getPhone());
        if (robot == null) {
            robot = robotRepository.findByAccount2(logDto.getPhone());
        }
        if (robot != null) {
            log.setCompany(robot.getCompany());
        }
        if (logDto.getContent() != null && (logDto.getContent().contains("位置监控操作失败") || logDto.getContent().contains("点击展开所有车辆按钮失败"))) {
            // 如果是位置监控，则重试
            RobotTask robotTask = robotTaskRepository.findById(logDto.getTaskId());
            if (robotTask != null) {
                robotTask.setTaskStatus(TypeStringUtils.taskStatus1);
                robotTaskRepository.save(robotTask);
            }
        }
        robotLogRepository.save(log);
    }

    @Override
    public PageResult<RobotLog> query(QueryLog params) {
        List<Robot> robots = robotRepository.findByOwner(params.getOwner());
        Specification<RobotLog> specification = ((root, criteriaQuery, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (!StringUtils.isEmpty(params.getPhone())) {
                Predicate phone = criteriaBuilder.like(root.get("phone"), params.getPhone());
                predicates.add(phone);
            }
            List<String> phones = robots.stream().map(Robot::getPhone).collect(Collectors.toList());
            Expression<String> exp = root.get("phone");
            predicates.add(exp.in(phones));
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        });
        Sort order = new Sort(Sort.Direction.DESC, "createTime");
        Pageable pageable = new PageRequest(params.getPage() - 1, params.getPageSize(), order);
        Page<RobotLog> details = robotLogRepository.findAll(specification, pageable);
        return PageResult.of(details.getContent(), params.getPage(), params.getPageSize(), details.getTotalElements());
    }
}
