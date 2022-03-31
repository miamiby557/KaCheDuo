package com.szcinda.service.driver;

import com.szcinda.repository.*;
import com.szcinda.service.ScheduleService;
import com.szcinda.service.SnowFlakeFactory;
import com.szcinda.service.TypeStringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@Transactional
public class DriverServiceImpl implements DriverService {
    private final DriverRepository driverRepository;
    private final SnowFlakeFactory snowFlakeFactory;
    private final FengXianRepository fengXianRepository;
    private final RobotTaskRepository robotTaskRepository;
    private final ScreenShotTaskRepository screenShotTaskRepository;
    private final HistoryScreenShotTaskRepository historyScreenShotTaskRepository;

    public DriverServiceImpl(DriverRepository driverRepository, FengXianRepository fengXianRepository,
                             RobotTaskRepository robotTaskRepository, ScreenShotTaskRepository screenShotTaskRepository,
                             HistoryScreenShotTaskRepository historyScreenShotTaskRepository) {
        this.driverRepository = driverRepository;
        this.fengXianRepository = fengXianRepository;
        this.robotTaskRepository = robotTaskRepository;
        this.screenShotTaskRepository = screenShotTaskRepository;
        this.historyScreenShotTaskRepository = historyScreenShotTaskRepository;
        this.snowFlakeFactory = SnowFlakeFactory.getInstance();
    }

    @Override
    public void importDriver(List<DriverImportDto> driverImportDtos, String owner) {
        List<Driver> drivers = new ArrayList<>();
        for (DriverImportDto driverImportDto : driverImportDtos) {
            Driver driver = driverRepository.findByVehicleNo(driverImportDto.getVehicleNo());
            if (driver != null) {
                driver.setName(driver.getName());
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
    public List<DriverDto> query(DriverQuery query) {
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
            Predicate owner = criteriaBuilder.equal(root.get("owner"), query.getOwner());
            predicates.add(owner);
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
    public void connect(DriverConnectDto connectDto) {
        Driver driver = driverRepository.findByVehicleNo(connectDto.getVehicleNo());
        if (driver != null) {
            driver.setWechat(connectDto.getWechat());
            driver.setOwnerWechat(connectDto.getOwnerWechat());
            driverRepository.save(driver);
        }
    }

    @Override
    public void savePic(DriverScreenShotDto shotDto) {
        FengXian fengXian = fengXianRepository.findOne(shotDto.getFxId());
        if (fengXian != null) {
            fengXian.setMessageSendTime(LocalDateTime.now().toString().replace('T', ' '));
            fengXian.setFilePath(shotDto.getFilePath());
            fengXianRepository.save(fengXian);
            // 删除截图任务， 生成一条历史截图任务
            ScreenShotTask screenShotTask = screenShotTaskRepository.findOne(shotDto.getScreenTaskId());
            HistoryScreenShotTask historyScreenShotTask = new HistoryScreenShotTask();
            BeanUtils.copyProperties(screenShotTask, historyScreenShotTask);
            historyScreenShotTask.setType(TypeStringUtils.screen_status1);
            historyScreenShotTask.setId(snowFlakeFactory.nextId("HT"));
            historyScreenShotTaskRepository.save(historyScreenShotTask);
            screenShotTaskRepository.delete(screenShotTask);
            // 创建一个处理任务
            CopyOnWriteArrayList<Robot> copyOnWriteRobots = ScheduleService.copyOnWriteRobots;
            RobotTask task = new RobotTask();
            task.setId(snowFlakeFactory.nextId("RT"));
            task.setTaskStatus(TypeStringUtils.robotType1);
            task.setTaskType(TypeStringUtils.robotType3);
            task.setFxId(fengXian.getId());
            task.setVehicleNo(fengXian.getVehicleNo());
            task.setHappenTime(fengXian.getHappenTime());
            for (Robot robot : copyOnWriteRobots) {
                if (robot.getPhone().equals(fengXian.getOwner()) && TypeStringUtils.robotType3.equals(robot.getType())) {
                    task.setUserName(robot.getPhone());
                    task.setPwd(robot.getPwd());
                    task.setCompany(robot.getCompany());
                    break;
                }
            }
            if (StringUtils.hasText(task.getUserName())) {
                robotTaskRepository.save(task);
            }
        }
    }
}
