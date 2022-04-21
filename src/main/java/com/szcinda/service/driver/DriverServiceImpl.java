package com.szcinda.service.driver;

import com.szcinda.repository.*;
import com.szcinda.service.PageResult;
import com.szcinda.service.ScheduleService;
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

import javax.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class DriverServiceImpl implements DriverService {
    private final DriverRepository driverRepository;
    private final SnowFlakeFactory snowFlakeFactory;
    private final FengXianRepository fengXianRepository;
    private final RobotTaskRepository robotTaskRepository;
    private final ScreenShotTaskRepository screenShotTaskRepository;
    private final HistoryScreenShotTaskRepository historyScreenShotTaskRepository;
    private final ScheduleService scheduleService;

    public DriverServiceImpl(DriverRepository driverRepository, FengXianRepository fengXianRepository,
                             RobotTaskRepository robotTaskRepository, ScreenShotTaskRepository screenShotTaskRepository,
                             HistoryScreenShotTaskRepository historyScreenShotTaskRepository, ScheduleService scheduleService) {
        this.driverRepository = driverRepository;
        this.fengXianRepository = fengXianRepository;
        this.robotTaskRepository = robotTaskRepository;
        this.screenShotTaskRepository = screenShotTaskRepository;
        this.historyScreenShotTaskRepository = historyScreenShotTaskRepository;
        this.scheduleService = scheduleService;
        this.snowFlakeFactory = SnowFlakeFactory.getInstance();
    }

    @Override
    public void importDriver(List<DriverImportDto> driverImportDtos, String owner) {
        List<Driver> drivers = new ArrayList<>();
        for (DriverImportDto driverImportDto : driverImportDtos) {
            Driver driver = driverRepository.findByVehicleNo(driverImportDto.getVehicleNo());
            if (driver != null) {
                driver.setCompany(driverImportDto.getCompany());
                driver.setName(driverImportDto.getName());
                driver.setPhone(driverImportDto.getPhone());
                driverRepository.save(driver);
            } else {
                driver = new Driver();
                BeanUtils.copyProperties(driverImportDto, driver);
                driver.setId(snowFlakeFactory.nextId("DR"));
                driver.setOwner(owner);
                drivers.add(driver);
            }
            if (drivers.size() > 0)
                driverRepository.save(drivers);
        }
    }

    @Override
    public void delete(String id) {
        driverRepository.delete(id);
    }

    @Override
    public PageResult<DriverDto> query(DriverQuery query) {
        Specification<Driver> specification = ((root, criteriaQuery, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (!StringUtils.isEmpty(query.getName())) {
                Predicate name = criteriaBuilder.like(root.get("name"), "%" + query.getName() + "%");
                predicates.add(name);
            }
            if (!StringUtils.isEmpty(query.getVehicleNo())) {
                Predicate vehicleNo = criteriaBuilder.like(root.get("vehicleNo"), "%" + query.getVehicleNo() + "%");
                predicates.add(vehicleNo);
            }
            if (!StringUtils.isEmpty(query.getCompany())) {
                Predicate company = criteriaBuilder.equal(root.get("company"), query.getCompany());
                predicates.add(company);
            }
            if (query.getFriend() != null) {
                Predicate company = criteriaBuilder.equal(root.get("friend"), query.getFriend());
                predicates.add(company);
            }
            Predicate owner = criteriaBuilder.equal(root.get("owner"), query.getOwner());
            predicates.add(owner);
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        });
        Sort order = new Sort(Sort.Direction.DESC, "createTime");
        Pageable pageable = new PageRequest(query.getPage() - 1, query.getPageSize(), order);
        Page<Driver> drivers = driverRepository.findAll(specification, pageable);
        List<DriverDto> driverDtos = new ArrayList<>();
        for (Driver driver : drivers) {
            DriverDto dto = new DriverDto();
            BeanUtils.copyProperties(driver, dto);
            driverDtos.add(dto);
        }
        return PageResult.of(driverDtos, query.getPage(), query.getPageSize(), drivers.getTotalElements());
    }

    @Override
    public void connect(DriverConnectDto connectDto) {
        Driver driver = driverRepository.findByVehicleNo(connectDto.getVehicleNo());
        if (driver != null) {
            driver.setWechat(connectDto.getWechat());
            driver.setOwnerWechat(connectDto.getOwnerWechat());
            driver.setWxid(connectDto.getWxid());
            driver.setFriend(true);
            driverRepository.save(driver);
        }else{
            driver = new Driver();
            driver.setId(snowFlakeFactory.nextId("DR"));
            driver.setVehicleNo(connectDto.getVehicleNo());
            driver.setOwnerWechat(connectDto.getOwnerWechat());
            driver.setWechat(connectDto.getWechat());
            driver.setWxid(connectDto.getWxid());
            driver.setCompany("");
            driver.setPhone("");
            driver.setName("");
            driver.setOwner("");
            driverRepository.save(driver);
        }
    }

    @Override
    public void savePic(DriverScreenShotDto shotDto) {
        // 删除截图任务， 生成一条历史截图任务
        ScreenShotTask screenShotTask = screenShotTaskRepository.findOne(shotDto.getScreenTaskId());
        HistoryScreenShotTask historyScreenShotTask = new HistoryScreenShotTask();
        BeanUtils.copyProperties(screenShotTask, historyScreenShotTask);
        historyScreenShotTask.setType(TypeStringUtils.screen_status1);
        historyScreenShotTask.setFilePath(shotDto.getFilePath());
        historyScreenShotTask.setId(snowFlakeFactory.nextId("HT"));
        historyScreenShotTaskRepository.save(historyScreenShotTask);
        screenShotTaskRepository.delete(screenShotTask);
        // 把其他的关于这个车牌的违规也一起变成已截图，减少微信截图次数
        List<ScreenShotTask> screenShotTasks = screenShotTaskRepository.findByVehicleNo(screenShotTask.getVehicleNo());
        for (ScreenShotTask shotTask : screenShotTasks) {
            historyScreenShotTask = new HistoryScreenShotTask();
            BeanUtils.copyProperties(shotTask, historyScreenShotTask);
            historyScreenShotTask.setType(TypeStringUtils.screen_status1);
            historyScreenShotTask.setId(snowFlakeFactory.nextId("HT"));
            historyScreenShotTask.setFilePath(shotDto.getFilePath());
            historyScreenShotTaskRepository.save(historyScreenShotTask);
            screenShotTaskRepository.delete(shotTask);
        }
        // 创建处理任务，把历史关于这个车牌号码未处理的都生成一个处理任务
        List<Robot> copyOnWriteRobots = scheduleService.queryAllRobotsFromCopyOnWriteRobots();
        List<FengXian> fengXianList = fengXianRepository.findByVehicleNoAndChuLiType(screenShotTask.getVehicleNo(), TypeStringUtils.fxHandleStatus1);
        for (FengXian fengXian : fengXianList) {
            fengXian.setFilePath(shotDto.getFilePath());
            fengXianRepository.save(fengXian);
            RobotTask task = new RobotTask();
            task.setId(snowFlakeFactory.nextId("RT"));
            task.setTaskStatus(TypeStringUtils.taskStatus1);
            task.setTaskType(TypeStringUtils.robotType3);
            task.setFxId(fengXian.getId());
            task.setVehicleNo(fengXian.getVehicleNo());
            task.setHappenTime(fengXian.getHappenTime());
            task.setFilePath(shotDto.getFilePath());
            // 过滤出处置账号所属的主账号下的处理账号，需要判断处理账号是否启用
            copyOnWriteRobots.stream().filter(item -> item.getPhone().equals(fengXian.getOwner()))
                    .findFirst().flatMap(runRobot -> copyOnWriteRobots.stream().filter(item -> item.getId().equals(runRobot.getParentId()))
                    .findFirst().flatMap(masterRobot -> copyOnWriteRobots.stream().filter(item -> StringUtils.hasText(item.getParentId()) && item.getParentId().equals(masterRobot.getId()))
                            .findFirst())).ifPresent(chuLiRobot -> {
                if (chuLiRobot.isRun()) {
                    task.setUserName(chuLiRobot.getPhone());
                    task.setPwd(chuLiRobot.getPwd());
                    task.setCompany(chuLiRobot.getCompany());
                }
            });
            if (StringUtils.hasText(task.getUserName())) {
                robotTaskRepository.save(task);
            }
        }
    }

    @Override
    public void confirm(String wechat) {
        // 把待回复的截图任务状态改为已回复
        List<ScreenShotTask> screenShotTasks = screenShotTaskRepository.findByWechatAndStatus(wechat, TypeStringUtils.wechat_status1);
        if (screenShotTasks.size() > 0) {
            for (ScreenShotTask screenShotTask : screenShotTasks) {
                screenShotTask.setStatus(TypeStringUtils.wechat_status2);
                if (StringUtils.hasText(screenShotTask.getFxId())) {
                    FengXian fengXian = fengXianRepository.findOne(screenShotTask.getFxId());
                    if (fengXian != null) {
                        // 更新回复时间
                        fengXian.setMessageReceiveTime(LocalDateTime.now().toString().replace('T', ' '));
                        fengXianRepository.save(fengXian);
                    }
                }
            }
            screenShotTaskRepository.save(screenShotTasks);
        }
        // 更新关于这个车牌的历史风险处置的回复时间
        Driver driver = driverRepository.findByWechat(wechat);
        if (driver != null && StringUtils.hasText(driver.getVehicleNo())) {
            List<FengXian> fengXianList = fengXianRepository.findByVehicleNoAndChuLiType(driver.getVehicleNo(), TypeStringUtils.fxHandleStatus1);
            for (FengXian fengXian : fengXianList) {
                // 更新回复时间
                fengXian.setMessageReceiveTime(LocalDateTime.now().toString().replace('T', ' '));
            }
            fengXianRepository.save(fengXianList);
        }
    }

    @Override
    public void update(DriverUpdateDto updateDto) {
        Driver driver = driverRepository.findOne(updateDto.getId());
        driver.setWechat(updateDto.getWechat());
        driver.setWxid(updateDto.getWxid());
        driver.setOwnerWechat(updateDto.getOwnerWechat());
        driverRepository.save(driver);
    }

    @Override
    public List<DriverDto> queryNoWechat(String owner) {
        Specification<Driver> specification = ((root, criteriaQuery, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            Predicate ownerPre = criteriaBuilder.equal(root.get("owner"), owner);
            predicates.add(ownerPre);
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        });
        List<Driver> drivers = driverRepository.findAll(specification).stream().filter(driver -> StringUtils.isEmpty(driver.getWechat())).collect(Collectors.toList());
        List<DriverDto> driverDtos = new ArrayList<>();
        for (Driver driver : drivers) {
            DriverDto dto = new DriverDto();
            BeanUtils.copyProperties(driver, dto);
            driverDtos.add(dto);
        }
        return driverDtos;
    }

    @Override
    public List<DriverDto> queryNotFriend(String owner) {
        Specification<Driver> specification = ((root, criteriaQuery, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            Predicate ownerPre = criteriaBuilder.equal(root.get("owner"), owner);
            predicates.add(ownerPre);
            Predicate friend = criteriaBuilder.equal(root.get("friend"), false);
            predicates.add(friend);
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        });
        List<Driver> drivers = driverRepository.findAll(specification);
        List<DriverDto> driverDtos = new ArrayList<>();
        for (Driver driver : drivers) {
            DriverDto dto = new DriverDto();
            BeanUtils.copyProperties(driver, dto);
            driverDtos.add(dto);
        }
        return driverDtos;
    }

    @Override
    public void updateInfo(UpdateDriverInfo driverInfo) {
        List<Driver> drivers = driverRepository.findByOwnerWechat(driverInfo.getOwner());
        List<String> wxids = driverInfo.getData();
        if (wxids == null || wxids.size() == 0) {
            return;
        }
        for (Driver driver : drivers) {
            if (wxids.contains(driver.getWxid())) {
                driver.setFriend(true);
            } else {
                driver.setFriend(false);
            }
        }
        driverRepository.save(drivers);
    }
}
