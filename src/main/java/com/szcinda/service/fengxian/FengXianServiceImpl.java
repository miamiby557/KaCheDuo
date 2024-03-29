package com.szcinda.service.fengxian;

import com.szcinda.repository.*;
import com.szcinda.service.PageResult;
import com.szcinda.service.SnowFlakeFactory;
import com.szcinda.service.TypeStringUtils;
import com.szcinda.service.callback.CallParams;
import com.szcinda.service.callback.CallService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class FengXianServiceImpl implements FengXianService {


    private final FengXianRepository fengXianRepository;
    private final SnowFlakeFactory snowFlakeFactory;
    private final RobotRepository robotRepository;
    private final DriverRepository driverRepository;
    private final ScreenShotTaskRepository screenShotTaskRepository;
    private final CallService callService;

    @Value("${file.save.path}")
    private String savePath;

    @Value("${body.tired.id}")
    private String tiredId;

    @Value("${over.speed.id}")
    private String overSpeedId;


    public FengXianServiceImpl(FengXianRepository fengXianRepository, RobotRepository robotRepository, DriverRepository driverRepository,
                               ScreenShotTaskRepository screenShotTaskRepository, CallService callService) {
        this.fengXianRepository = fengXianRepository;
        this.robotRepository = robotRepository;
        this.driverRepository = driverRepository;
        this.screenShotTaskRepository = screenShotTaskRepository;
        this.callService = callService;
        this.snowFlakeFactory = SnowFlakeFactory.getInstance();
    }

    @Override
    public FengXian create(CreateFengXianDto dto) {
        // 不允许同一时间同一车牌新建风险
        FengXian fengXian = fengXianRepository.findByVehicleNoAndHappenTime(dto.getVehicleNo(), dto.getHappenTime());
        if (fengXian == null) {
            fengXian = new FengXian();
            BeanUtils.copyProperties(dto, fengXian);
            fengXian.setId(snowFlakeFactory.nextId("FX"));
            // 判断是否有关于生理疲劳报警 超速报警 需要打电话通知司机
            CallParams callParams = null;
            if (TypeStringUtils.tired_status.equals(fengXian.getDangerType())) {
                Driver driver = driverRepository.findByVehicleNo(fengXian.getVehicleNo());
                if (driver != null) {
                    callParams = new CallParams();
                    callParams.setPhone(driver.getPhone());
                    callParams.setTemplateId(tiredId);
                    fengXian.setPhone(driver.getPhone());
                }
            } else if (TypeStringUtils.over_status.equals(fengXian.getDangerType()) && over89(fengXian.getSpeed())) { // 超速，并且速度大于89每小时才电话通知
                Driver driver = driverRepository.findByVehicleNo(fengXian.getVehicleNo());
                if (driver != null) {
                    callParams = new CallParams();
                    callParams.setPhone(driver.getPhone());
                    callParams.setTemplateId(overSpeedId);
                    fengXian.setPhone(driver.getPhone());
                }
            }
            if (callParams != null) {
                callParams.setFxId(fengXian.getId());

                callParams.setVehicleNo(fengXian.getVehicleNo());
                callParams.setAccount(fengXian.getOwner());
                callParams.setCompany(fengXian.getCompany());

                callService.call(callParams);
            }
            fengXianRepository.save(fengXian);
        }
        return fengXian;
    }

    @Override
    public void batchCreate(List<CreateFengXianDto> dtos) {
        Robot robot = robotRepository.findByPhone(dtos.get(0).getOwner());
        for (CreateFengXianDto dto : dtos) {
            if (robot != null) {
                dto.setCompany(robot.getCompany());
            }
            this.create(dto);
        }
    }

    @Override
    public void chuZhi(ChuZhiDto chuZhiDto) {
        FengXian fengXian = fengXianRepository.findByVehicleNoAndHappenTime(chuZhiDto.getVehicleNo(), chuZhiDto.getHappenTime());
        if (fengXian != null) {
            fengXian.setDisposeTime(LocalDateTime.now());
            fengXian.setChuZhiType(TypeStringUtils.chuZhiStatus2);
            fengXian.setChuLiType(TypeStringUtils.fxHandleStatus1);
            fengXianRepository.save(fengXian);
            // 如果是疲劳驾驶或者是超速报警，则需要打电话
            // 如果是生理疲劳或者超速，需要打电话
            Driver driver = driverRepository.findByVehicleNo(fengXian.getVehicleNo());
            if (driver != null && StringUtils.hasText(driver.getPhone())) {
                CallParams callParams = null;
                if (TypeStringUtils.tired_status.equals(fengXian.getDangerType())) {
                    callParams = new CallParams();
                    callParams.setPhone(driver.getPhone());
                    callParams.setTemplateId(tiredId);
                } else if (TypeStringUtils.over_status.equals(fengXian.getDangerType()) && over89(fengXian.getSpeed())) { // 超速，并且速度大于89每小时才电话通知
                    callParams = new CallParams();
                    callParams.setPhone(driver.getPhone());
                    callParams.setTemplateId(overSpeedId);
                }
                if (callParams != null) {
                    callParams.setFxId(fengXian.getId());
                    callService.call(callParams);
                }
            }
        }
    }

    private boolean over89(String speed) {
        if (speed == null) {
            return false;
        }
        speed = speed.toLowerCase().replace("km/h", "").trim();
        return Float.parseFloat(speed) > 89;
    }


    @Override
    public void generateScreenShotMissions() {
        List<FengXian> fengXianList = fengXianRepository.findByChuLiType(TypeStringUtils.fxHandleStatus1);
        if (fengXianList.size() > 0) {
            for (FengXian fengXian : fengXianList) {
                // 创建一个截图的任务
                Driver driver = driverRepository.findByVehicleNo(fengXian.getVehicleNo());
                if (driver != null && StringUtils.hasText(driver.getWechat())) {
                    ScreenShotTask screenShotTask = new ScreenShotTask();
                    screenShotTask.setId(snowFlakeFactory.nextId("ST"));
                    screenShotTask.setFxId(fengXian.getId());
                    screenShotTask.setWechat(driver.getWechat());
                    screenShotTask.setVehicleNo(fengXian.getVehicleNo());
                    screenShotTask.setOwnerWechat(driver.getOwnerWechat());
                    screenShotTask.setStatus(TypeStringUtils.wechat_status3);
                    screenShotTask.setOwner(fengXian.getOwner());
                    screenShotTask.setHappenTime(fengXian.getHappenTime());
                    screenShotTask.setWxid(driver.getWxid());
                    screenShotTask.setContent(TypeStringUtils.getWechatContent(fengXian.getDangerType()));
                    screenShotTaskRepository.save(screenShotTask);
                }
            }
        }
    }

    @Override
    public void batchCreateHandle(List<CreateFengXianDto> dtos) {
        Robot robot = robotRepository.findByPhone(dtos.get(0).getOwner());
        for (CreateFengXianDto dto : dtos) {
            if (robot != null) {
                dto.setCompany(robot.getCompany());
            }
            dto.setChuLiType(TypeStringUtils.fxHandleStatus1);
            FengXian fengXian = this.create(dto);
            // 创建一个截图的任务
            Driver driver = driverRepository.findByVehicleNo(fengXian.getVehicleNo());
            if (driver != null && StringUtils.hasText(driver.getWechat())) {
                ScreenShotTask screenShotTask = new ScreenShotTask();
                screenShotTask.setId(snowFlakeFactory.nextId("ST"));
                screenShotTask.setFxId(fengXian.getId());
                screenShotTask.setWechat(driver.getWechat());
                screenShotTask.setVehicleNo(fengXian.getVehicleNo());
                screenShotTask.setOwnerWechat(driver.getOwnerWechat());
                screenShotTask.setStatus(TypeStringUtils.wechat_status3);
                screenShotTask.setOwner(fengXian.getOwner());
                screenShotTask.setHappenTime(fengXian.getHappenTime());
                screenShotTask.setWxid(driver.getWxid());
                screenShotTask.setContent(TypeStringUtils.getWechatContent(fengXian.getDangerType()));
                screenShotTaskRepository.save(screenShotTask);
            }
        }
    }

    @Override
    public PageResult<ChuZhiDetailDto> query(ChuZhiQuery params) {
        List<Robot> robots = robotRepository.findByOwner(params.getOwner());
        Specification<FengXian> specification = ((root, criteriaQuery, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (!StringUtils.isEmpty(params.getVehicleNo())) {
                Predicate vehicleNo = criteriaBuilder.equal(root.get("vehicleNo"), params.getVehicleNo());
                predicates.add(vehicleNo);
            }
            if (!StringUtils.isEmpty(params.getHappenTime())) {
                Predicate happenTime = criteriaBuilder.like(root.get("happenTime"), params.getHappenTime() + "%");
                predicates.add(happenTime);
            }
            if (!StringUtils.isEmpty(params.getUserName())) {
                Predicate owner = criteriaBuilder.equal(root.get("owner"), params.getUserName());
                predicates.add(owner);
            }
            if (!StringUtils.isEmpty(params.getCompany())) {
                Predicate company = criteriaBuilder.equal(root.get("company"), params.getCompany());
                predicates.add(company);
            }
            if (params.getCreateTimeStart() != null) {
                Predicate createTimeStart = criteriaBuilder.greaterThanOrEqualTo(root.get("createTime"), params.getCreateTimeStart().atStartOfDay());
                predicates.add(createTimeStart);
            }
            if (params.getCreateTimeEnd() != null) {
                Predicate createTimeEnd = criteriaBuilder.lessThan(root.get("createTime"), params.getCreateTimeEnd().plusDays(1).atStartOfDay());
                predicates.add(createTimeEnd);
            }
            if (params.getDisposeTimeStart() != null) {
                Predicate disposeTimeStart = criteriaBuilder.greaterThanOrEqualTo(root.get("disposeTime"), params.getDisposeTimeStart().atStartOfDay());
                predicates.add(disposeTimeStart);
            }
            if (params.getDisposeTimeEnd() != null) {
                Predicate disposeTimeEnd = criteriaBuilder.lessThan(root.get("disposeTime"), params.getDisposeTimeEnd().plusDays(1).atStartOfDay());
                predicates.add(disposeTimeEnd);
            }
            if (params.getStatusList() != null && params.getStatusList().size() > 0) {
                Expression<String> exp = root.get("dangerType");
                predicates.add(exp.in(params.getStatusList()));
            }
            List<String> phones = robots.stream().map(Robot::getPhone).collect(Collectors.toList());
            Expression<String> exp = root.get("owner");
            predicates.add(exp.in(phones));
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        });
        Sort order = new Sort(Sort.Direction.DESC, "createTime");
        Pageable pageable = new PageRequest(params.getPage() - 1, params.getPageSize(), order);
        Page<FengXian> details = fengXianRepository.findAll(specification, pageable);
        List<ChuZhiDetailDto> dtos = new ArrayList<>();
        if (details.getContent() != null) {
            for (FengXian fengXian : details.getContent()) {
                ChuZhiDetailDto dto = new ChuZhiDetailDto();
                BeanUtils.copyProperties(fengXian, dto);
                dtos.add(dto);
            }
        }
        return PageResult.of(dtos, params.getPage(), params.getPageSize(), details.getTotalElements());
    }

    @Override
    public List<FengXian> queryAll(ChuZhiQuery params) {
        List<Robot> robots = robotRepository.findByOwner(params.getOwner());
        Specification<FengXian> specification = ((root, criteriaQuery, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (!StringUtils.isEmpty(params.getVehicleNo())) {
                Predicate vehicleNo = criteriaBuilder.equal(root.get("vehicleNo"), params.getVehicleNo());
                predicates.add(vehicleNo);
            }
            if (!StringUtils.isEmpty(params.getHappenTime())) {
                Predicate happenTime = criteriaBuilder.like(root.get("happenTime"), params.getHappenTime() + "%");
                predicates.add(happenTime);
            }
            if (!StringUtils.isEmpty(params.getUserName())) {
                Predicate owner = criteriaBuilder.equal(root.get("owner"), params.getUserName());
                predicates.add(owner);
            }
            if (!StringUtils.isEmpty(params.getCompany())) {
                Predicate company = criteriaBuilder.equal(root.get("company"), params.getCompany());
                predicates.add(company);
            }
            if (params.getCreateTimeStart() == null || params.getCreateTimeEnd() == null) {
                LocalDate now = LocalDate.now();
                Predicate timeStart = criteriaBuilder.greaterThanOrEqualTo(root.get("createTime"), now.atStartOfDay());
                predicates.add(timeStart);
                Predicate timeEnd = criteriaBuilder.lessThan(root.get("createTime"), now.plusDays(1).atStartOfDay());
                predicates.add(timeEnd);
            } else {
                if (params.getCreateTimeStart() != null) {
                    Predicate createTimeStart = criteriaBuilder.greaterThanOrEqualTo(root.get("createTime"), params.getCreateTimeStart().atStartOfDay());
                    predicates.add(createTimeStart);
                }
                if (params.getCreateTimeEnd() != null) {
                    Predicate createTimeEnd = criteriaBuilder.lessThan(root.get("createTime"), params.getCreateTimeEnd().plusDays(1).atStartOfDay());
                    predicates.add(createTimeEnd);
                }
            }

            List<String> phones = robots.stream().map(Robot::getPhone).collect(Collectors.toList());
            Expression<String> exp = root.get("owner");
            predicates.add(exp.in(phones));
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        });
        Sort order = new Sort(Sort.Direction.DESC, "createTime");
        return fengXianRepository.findAll(specification, order);
    }

    @Override
    public void finish(String id) {
        FengXian fengXian = fengXianRepository.findOne(id);
        fengXian.setChuLiType(TypeStringUtils.fxHandleStatus2);
        fengXian.setChuLiTime(LocalDateTime.now());
        fengXianRepository.save(fengXian);
    }

    @Override
    public void error(HandleErrorDto errorDto) {
        FengXian fengXian = fengXianRepository.findOne(errorDto.getId());
        fengXian.setChuLiType(TypeStringUtils.fxHandleStatus3);
        fengXian.setChuLiTime(LocalDateTime.now());
        fengXianRepository.save(fengXian);
    }
}
