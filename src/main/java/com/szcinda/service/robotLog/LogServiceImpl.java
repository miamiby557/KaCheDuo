package com.szcinda.service.robotLog;

import com.szcinda.repository.Robot;
import com.szcinda.repository.RobotLog;
import com.szcinda.repository.RobotLogRepository;
import com.szcinda.repository.RobotRepository;
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

    public LogServiceImpl(RobotLogRepository robotLogRepository, RobotRepository robotRepository) {
        this.robotLogRepository = robotLogRepository;
        this.robotRepository = robotRepository;
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
