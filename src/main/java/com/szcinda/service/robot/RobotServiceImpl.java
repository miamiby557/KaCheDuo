package com.szcinda.service.robot;

import com.szcinda.controller.util.CarCountDto;
import com.szcinda.repository.*;
import com.szcinda.service.*;
import com.szcinda.service.robotTask.CreateRobotTaskDto;
import com.szcinda.service.robotTask.RobotTaskService;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import javax.persistence.criteria.Predicate;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.szcinda.service.ScheduleService.robotSearchLocationList;

@Service
@Transactional
public class RobotServiceImpl implements RobotService {
    private final RobotRepository robotRepository;
    private final SnowFlakeFactory snowFlakeFactory;
    private final UserRepository userRepository;
    private final ScheduleService scheduleService;
    private final RobotTaskService robotTaskService;
    private final CarCountRepository carCountRepository;

    public RobotServiceImpl(RobotRepository robotRepository, UserRepository userRepository, ScheduleService scheduleService, RobotTaskService robotTaskService, CarCountRepository carCountRepository) {
        this.robotRepository = robotRepository;
        this.userRepository = userRepository;
        this.scheduleService = scheduleService;
        this.robotTaskService = robotTaskService;
        this.carCountRepository = carCountRepository;
        this.snowFlakeFactory = SnowFlakeFactory.getInstance();
    }

    @Override
    public void create(CreateRobotDto dto) {
        Assert.isTrue(dto.getSubRobotList().size() > 0, "必须添加至少一条子账号数据");
        Robot robot = robotRepository.findByPhone(dto.getPhone());
        Assert.isTrue(robot == null, String.format("存在相同登录帐号【%s】，不允许创建", dto.getPhone()));
        robot = robotRepository.findByAccount2(dto.getAccount2());
        Assert.isTrue(robot == null, String.format("存在相同登录帐号【%s】，不允许创建", dto.getAccount2()));
        robot = robotRepository.findByCompanyAndParentIdIsNull(dto.getCompany());
        Assert.isTrue(robot == null, String.format("存在相同公司名称【%s】，不允许创建", dto.getAccount2()));
        // 判断子账号有没有创建过
        for (CreateRobotDto createRobotDto : dto.getSubRobotList()) {
            robot = robotRepository.findByPhone(createRobotDto.getPhone());
            Assert.isTrue(robot == null, String.format("存在相同登录帐号【%s】，不允许创建", createRobotDto.getPhone()));
        }
        // 做一下符号的容错
        if (StringUtils.hasText(dto.getEmail())) {
            dto.setEmail(dto.getEmail().replace('，', ','));
        }
        robot = new Robot();
        BeanUtils.copyProperties(dto, robot);
        robot.setId(snowFlakeFactory.nextId("RB"));
        robot.setType(TypeStringUtils.robotType1);
        robotRepository.save(robot);
        scheduleService.addRobotFromCopyOnWriteRobots(robot);
        ScheduleService.robotPwdMap.put(robot.getPhone(), robot.getPwd());
        // 处理和位置监控
        // 创建处理帐号
        Robot robot2 = new Robot();
        robot2.setCompany(dto.getCompany());
        robot2.setPhone(dto.getAccount2());
        robot2.setPwd(dto.getPwd2());
        robot2.setOwner(dto.getOwner());
        robot2.setId(snowFlakeFactory.nextId("RB"));
        robot2.setParentId(robot.getId());
        robot2.setType(TypeStringUtils.robotType3);
        robotRepository.save(robot2);
        ScheduleService.robotPwdMap.put(robot2.getPhone(), robot2.getPwd());
        for (CreateRobotDto createRobotDto : dto.getSubRobotList()) {
            Robot subRobot = new Robot();
            BeanUtils.copyProperties(createRobotDto, subRobot);
            subRobot.setId(snowFlakeFactory.nextId("RB"));
            subRobot.setParentId(robot.getId());
            subRobot.setOwner(robot.getOwner());
            subRobot.setCompany(dto.getCompany());
            subRobot.setType(TypeStringUtils.robotType2);
            robotRepository.save(subRobot);
            scheduleService.addRobotFromCopyOnWriteRobots(subRobot);
            ScheduleService.robotPwdMap.put(subRobot.getPhone(), subRobot.getPwd());
        }
        ScheduleService.copyOnWriteRobots.clear();
    }

    @Override
    public void update(UpdateRobotDto dto) {
        Assert.isTrue(dto.getSubRobotList().size() > 0, "必须添加至少一条子账号数据");
        Robot robot = robotRepository.findByPhone(dto.getPhone());
        Assert.isTrue(robot == null || robot.getId().equals(dto.getId()), String.format("存在相同登录帐号【%s】，不允许修改", dto.getPhone()));
        robot = robotRepository.findByCompanyAndParentIdIsNull(dto.getCompany());
        Assert.isTrue(robot == null || robot.getId().equals(dto.getId()), String.format("存在相同公司名称【%s】，不允许修改", dto.getPhone()));
        robot = robotRepository.findByAccount2(dto.getAccount2());
        Assert.isTrue(robot == null || robot.getId().equals(dto.getId()), String.format("存在相同登录帐号【%s】，不允许创建", dto.getAccount2()));
        robot = robotRepository.findById(dto.getId());
        robot.setEmail(dto.getEmail());
        robot.setChargePhone(dto.getChargePhone());
        robot.setPhone(dto.getPhone());
        robot.setPwd(dto.getPwd());
        robot.setCompany(dto.getCompany());
        robot.setType(TypeStringUtils.robotType1);
        robot.setAccount2(dto.getAccount2());
        robot.setPwd2(dto.getPwd2());
        robot.setCarCount(dto.getCarCount());
        // 做一下符号的容错
        if (StringUtils.hasText(robot.getEmail())) {
            robot.setEmail(robot.getEmail().replace('，', ','));
        }
        robotRepository.save(robot);
        scheduleService.updateRobotFromCopyOnWriteRobots(robot);
        ScheduleService.robotPwdMap.put(robot.getPhone(), robot.getPwd());
        List<Robot> subRobots = robotRepository.findByParentId(robot.getId());
        for (Robot subRobot : subRobots) {
            ScheduleService.robotPwdMap.remove(subRobot.getPhone());
            robotRepository.delete(subRobot);
            scheduleService.removeRobotFromCopyOnWriteRobots(subRobot.getId());
        }
        // 判断处理帐号有没有创建过
        Robot subRobot = robotRepository.findByPhone(dto.getAccount2());
        Assert.isTrue(subRobot == null, String.format("存在相同(处理、位置监控)帐号【%s】，不允许修改", dto.getAccount2()));
        // 判断子账号有没有创建过
        for (UpdateRobotDto createRobotDto : dto.getSubRobotList()) {
            subRobot = robotRepository.findByPhone(createRobotDto.getPhone());
            Assert.isTrue(subRobot == null, String.format("存在相同登录帐号【%s】，不允许修改", createRobotDto.getPhone()));
        }
        Robot robot2 = new Robot();
        robot2.setCompany(dto.getCompany());
        robot2.setPhone(dto.getAccount2());
        robot2.setPwd(dto.getPwd2());
        robot2.setId(snowFlakeFactory.nextId("RB"));
        robot2.setParentId(robot.getId());
        robot2.setOwner(dto.getOwner());
        robot2.setType(TypeStringUtils.robotType3);
        robotRepository.save(robot2);
        ScheduleService.robotPwdMap.put(robot2.getPhone(), robot2.getPwd());
        for (UpdateRobotDto createRobotDto : dto.getSubRobotList()) {
            subRobot = new Robot();
            BeanUtils.copyProperties(createRobotDto, subRobot);
            subRobot.setId(snowFlakeFactory.nextId("RB"));
            subRobot.setParentId(robot.getId());
            subRobot.setOwner(robot.getOwner());
            subRobot.setCompany(dto.getCompany());
            subRobot.setType(TypeStringUtils.robotType2);
            robotRepository.save(subRobot);
            scheduleService.addRobotFromCopyOnWriteRobots(subRobot);
            ScheduleService.robotPwdMap.put(subRobot.getPhone(), subRobot.getPwd());
        }
        ScheduleService.copyOnWriteRobots.clear();
    }

    @Override
    public PageResult<RobotDto> query(QueryRobotParams params) {
        LocalDateTime now = LocalDateTime.now();
        Specification<Robot> specification = ((root, criteriaQuery, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            Predicate owner = criteriaBuilder.equal(root.get("owner"), params.getOwner());
            predicates.add(owner);
            if (StringUtils.hasText(params.getCompany())) {
                Predicate company = criteriaBuilder.like(root.get("company"), "%" + params.getCompany() + "%");
                predicates.add(company);
            }
            Predicate parentId = criteriaBuilder.isNull(root.get("parentId"));
            predicates.add(parentId);
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        });
        Sort order = new Sort(Sort.Direction.DESC, "createTime");
        Pageable pageable = new PageRequest(params.getPage() - 1, params.getPageSize(), order);
        Page<Robot> robots = robotRepository.findAll(specification, pageable);
        List<RobotDto> robotDtos = new ArrayList<>();
        if (robots.getContent().size() > 0) {
            ConcurrentHashMap<String, LocalDateTime> robotAliveMap = ScheduleService.robotAliveMap;
            for (Robot robot : robots) {
                RobotDto dto = new RobotDto();
                BeanUtils.copyProperties(robot, dto);
                if (robotAliveMap.containsKey(robot.getPhone())) {
                    Duration duration = Duration.between(now, robotAliveMap.get(robot.getPhone()));
                    long minutes = Math.abs(duration.toMinutes());//相差的分钟数
                    dto.setAlive(minutes <= 5);
                }
                dto.setLastTime(robotAliveMap.get(robot.getPhone()));
                // 查找子账号
                List<Robot> subRobots = robotRepository.findByParentId(robot.getId());
                for (Robot subRobot : subRobots) {
                    if (TypeStringUtils.robotType3.equals(subRobot.getType())) {
                        dto.setAccount2(subRobot.getPhone());
                        dto.setPwd2(subRobot.getPwd());
                        dto.setId2(subRobot.getId());// 处理机器人的id
                        dto.setRun2(subRobot.isRun()); // 机器人处理是否启动
                        dto.setRunLocation(subRobot.isRunLocation()); // 是否启动位置查询
                    } else {
                        RobotDto subRobotDto = new RobotDto();
                        BeanUtils.copyProperties(subRobot, subRobotDto);
                        if (robotAliveMap.containsKey(subRobotDto.getPhone())) {
                            Duration duration = Duration.between(now, robotAliveMap.get(subRobotDto.getPhone()));
                            long minutes = Math.abs(duration.toMinutes());//相差的分钟数
                            subRobotDto.setAlive(minutes <= 5);
                        }
                        subRobotDto.setLastTime(robotAliveMap.get(subRobotDto.getPhone()));
                        dto.getSubRobots().add(subRobotDto);
                    }
                }
                robotDtos.add(dto);
            }
        }
        return PageResult.of(robotDtos, params.getPage(), params.getPageSize(), robots.getTotalElements());
    }


    @Override
    public List<RobotGroupDto> querySelf(String owner) {
        LocalDateTime now = LocalDateTime.now();
        List<Robot> robots = scheduleService.queryBySelfFromCopyOnWriteRobots(owner);
        List<RobotGroupDto> robotDtos = new ArrayList<>();
        ConcurrentHashMap<String, LocalDateTime> robotAliveMap = ScheduleService.robotAliveMap;
        for (Robot robot : robots) {
            RobotGroupDto groupDto = new RobotGroupDto();
            groupDto.setOwner(robot.getPhone());
            groupDto.setCompany(robot.getCompany());
            RobotDto dto = new RobotDto();
            BeanUtils.copyProperties(robot, dto);
            if (robotAliveMap.containsKey(robot.getPhone())) {
                Duration duration = Duration.between(now, robotAliveMap.get(robot.getPhone()));
                long minutes = Math.abs(duration.toMinutes());//相差的分钟数
                dto.setAlive(minutes <= 5);
            }
            dto.setLastTime(robotAliveMap.get(robot.getPhone()));
            dto.setPhone(dto.getPhone() + "(" + dto.getType() + ")");
            groupDto.getSubRobots().add(dto);
            // 查找子账号
            List<Robot> subRobots = scheduleService.querySubRobotsFromCopyOnWriteRobots(robot.getId());
            for (Robot subRobot : subRobots) {
                RobotDto subRobotDto = new RobotDto();
                BeanUtils.copyProperties(subRobot, subRobotDto);
                if (robotAliveMap.containsKey(subRobotDto.getPhone())) {
                    Duration duration = Duration.between(now, robotAliveMap.get(subRobotDto.getPhone()));
                    long minutes = Math.abs(duration.toMinutes());//相差的分钟数
                    subRobotDto.setAlive(minutes <= 5);
                }
                subRobotDto.setLastTime(robotAliveMap.get(subRobotDto.getPhone()));
                subRobotDto.setPhone(subRobotDto.getPhone() + "(" + subRobotDto.getType() + ")");
                groupDto.getSubRobots().add(subRobotDto);
            }
            robotDtos.add(groupDto);
        }
        return robotDtos;
    }


    @Override
    public void delete(String id) {
        Robot robot = robotRepository.findById(id);
        List<Robot> subRobots = robotRepository.findByParentId(id);
        robotRepository.delete(robot);
        scheduleService.removeRobotFromCopyOnWriteRobots(id);
        ScheduleService.robotPwdMap.remove(robot.getPhone());
        if (subRobots.size() > 0) {
            for (Robot subRobot : subRobots) {
                robotRepository.delete(subRobot);
                scheduleService.removeRobotFromCopyOnWriteRobots(subRobot.getId());
                ScheduleService.robotPwdMap.remove(subRobot.getPhone());
            }
        }
        ScheduleService.copyOnWriteRobots.clear();
    }


    @Override
    public void stop(String id) {
        Robot robot = robotRepository.findById(id);
        List<Robot> subRobots = robotRepository.findByParentId(id);
        robot.setRun(false);
        robotRepository.save(robot);
        scheduleService.updateRobotFromCopyOnWriteRobots(robot);
        if (subRobots.size() > 0) {
            for (Robot subRobot : subRobots) {
                subRobot.setRun(false);
                robotRepository.save(subRobot);
                scheduleService.updateRobotFromCopyOnWriteRobots(subRobot);
            }
        }
        ScheduleService.copyOnWriteRobots.clear();
    }

    @Override
    public void start(String id) {
        Robot robot = robotRepository.findById(id);
        List<Robot> subRobots = robotRepository.findByParentId(id);
        robot.setRun(true);
        robotRepository.save(robot);
        // 把主账号改为运行
        scheduleService.updateRobotFromCopyOnWriteRobots(robot);
        if (subRobots.size() > 0) {
            for (Robot subRobot : subRobots) {
                subRobot.setRun(true);
                robotRepository.save(subRobot);
                scheduleService.updateRobotFromCopyOnWriteRobots(subRobot);
            }
        }
        ScheduleService.copyOnWriteRobots.clear();
    }

    @Override
    public List<RobotGroupDto> group() {
        List<RobotGroupDto> groupDtos = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        List<Robot> robots = scheduleService.queryAllRobotsFromCopyOnWriteRobots();
        Map<String, List<Robot>> map = robots.stream().collect(Collectors.groupingBy(Robot::getOwner));
        ConcurrentHashMap<String, LocalDateTime> robotAliveMap = ScheduleService.robotAliveMap;
        map.forEach((owner, list) -> {
            RobotGroupDto groupDto = new RobotGroupDto();
            User user = userRepository.findById(owner);
            groupDto.setOwner(user.getAccount());
            groupDto.setCompany(user.getCompany());
            for (Robot robot : list) {
                RobotDto dto = new RobotDto();
                BeanUtils.copyProperties(robot, dto);
                if (robotAliveMap.containsKey(robot.getPhone())) {
                    Duration duration = Duration.between(now, robotAliveMap.get(robot.getPhone()));
                    long minutes = Math.abs(duration.toMinutes());//相差的分钟数
                    dto.setAlive(minutes <= 5);
                }
                dto.setLastTime(robotAliveMap.get(robot.getPhone()));
                dto.setPhone(dto.getPhone() + "(" + dto.getType() + ")");
                groupDto.getSubRobots().add(dto);
            }
            groupDtos.add(groupDto);
        });
        return groupDtos;
    }

    @Override
    public List<RobotDto> find10Account() {
        List<RobotDto> robotDtos = new ArrayList<>();
        List<Robot> robots = scheduleService.queryAllRobotsFromCopyOnWriteRobots();
        for (Robot robot : robots) {
            // 判断机器人是否在线
            boolean inMap = scheduleService.isAlive(robot.getPhone());
            if (StringUtils.isEmpty(robot.getParentId()) && !inMap && robot.isRun()) {
                RobotDto dto = new RobotDto();
                dto.setId(robot.getId());
                dto.setPhone(robot.getPhone());
                dto.setPwd(robot.getPwd());
                robotDtos.add(dto);
            }
        }
        return robotDtos;
    }

    @Override
    public List<String> getLocationRobots(String owner) {
        List<String> names = new ArrayList<>();
        List<Robot> robots = robotRepository.findByOwnerAndParentIdIsNull(owner);
        for (Robot robot : robots) {
            if (StringUtils.hasText(robot.getAccount2())) {
                names.add(robot.getAccount2());
            }
        }
        return names;
    }

    @Override
    public void startLocation(String id) {
        Robot robot = robotRepository.findById(id);
        robot.setRunLocation(true);
        robotRepository.save(robot);
    }

    @Override
    public void stopLocation(String id) {
        Robot robot = robotRepository.findById(id);
        robot.setRunLocation(false);
        robotRepository.save(robot);
    }

    @Override
    public void batchRunOnceLocation() {
        List<Robot> robots = robotRepository.findByType(TypeStringUtils.robotType3);
        // 清空历史任务
        robotSearchLocationList.clear();
        for (Robot robot : robots) {
            if (robot.isRun()) {
                //加到位置监控的列表
                robotSearchLocationList.add(robot.getPhone());
                // 创建一条位置监控的任务
                CreateRobotTaskDto taskDto = new CreateRobotTaskDto();
                taskDto.setTaskType(TypeStringUtils.robotType3);
                taskDto.setUserName(robot.getPhone());
                taskDto.setPwd(robot.getPwd());
                taskDto.setCompany(robot.getCompany());
                robotTaskService.create(taskDto);
            }
        }
    }

    @Override
    public void runOnceLocation(String id) {
        Robot robot = robotRepository.findById(id);
        List<Robot> robots = robotRepository.findByParentId(robot.getId());
        robots.stream().filter(r -> TypeStringUtils.robotType3.equals(r.getType())).findFirst()
                .ifPresent(robot1 -> {
                    if (robot1.isRun()) {
                        // 清空历史任务
                        robotSearchLocationList.remove(robot1.getPhone());
                        // 创建一条位置监控的任务
                        CreateRobotTaskDto taskDto = new CreateRobotTaskDto();
                        taskDto.setTaskType(TypeStringUtils.robotType3);
                        taskDto.setUserName(robot1.getPhone());
                        taskDto.setPwd(robot1.getPwd());
                        taskDto.setCompany(robot1.getCompany());
                        robotTaskService.create(taskDto);
                    }
                });
    }

    @Override
    public void updateCarCount(CarCountDto carCountDto) {
        Robot robot = robotRepository.findByPhone(carCountDto.getUserName());
        if (robot != null) {
            robot.setCarCount(carCountDto.getCount());
            robotRepository.save(robot);
            // 更新每天的车辆数量
            CarCount carCount = carCountRepository.findByAccountAndDate(carCountDto.getUserName(), LocalDate.now());
            if (carCount == null) {
                carCount = new CarCount();
                carCount.setId(snowFlakeFactory.nextId("CC"));
                carCount.setDate(LocalDate.now());
                carCount.setCompany(robot.getCompany());
                carCount.setAccount(robot.getPhone());
            }
            carCount.setCount(carCountDto.getCount());
            carCountRepository.save(carCount);
        }

    }

}
