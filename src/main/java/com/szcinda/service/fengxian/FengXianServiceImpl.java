package com.szcinda.service.fengxian;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import sun.misc.BASE64Encoder;

import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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

    @Value("${file.save.path}")
    private String savePath;

    public FengXianServiceImpl(FengXianRepository fengXianRepository, RobotRepository robotRepository, DriverRepository driverRepository,
                               ScreenShotTaskRepository screenShotTaskRepository) {
        this.fengXianRepository = fengXianRepository;
        this.robotRepository = robotRepository;
        this.driverRepository = driverRepository;
        this.screenShotTaskRepository = screenShotTaskRepository;
        this.snowFlakeFactory = SnowFlakeFactory.getInstance();
    }

    @Override
    public FengXian create(CreateFengXianDto dto) {
        FengXian fengXian = fengXianRepository.findByVehicleNoAndHappenTime(dto.getVehicleNo(), dto.getHappenTime());
        if (fengXian == null) {
            fengXian = new FengXian();
            BeanUtils.copyProperties(dto, fengXian);
            fengXian.setId(snowFlakeFactory.nextId("FX"));
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
            fengXian.setChuLiType(TypeStringUtils.fxHandleStatus1);
            fengXianRepository.save(fengXian);
            // 创建一个截图的任务
            Driver driver = driverRepository.findByVehicleNo(fengXian.getVehicleNo());
            if (driver != null && StringUtils.hasText(driver.getWechat())) {
                ScreenShotTask screenShotTask = new ScreenShotTask();
                screenShotTask.setId(snowFlakeFactory.nextId("ST"));
                screenShotTask.setFxId(fengXian.getId());
                screenShotTask.setWechat(driver.getWechat());
                screenShotTask.setVehicleNo(fengXian.getVehicleNo());
                screenShotTask.setOwnerWechat(driver.getOwnerWechat());
                screenShotTask.setOwner(fengXian.getOwner());
                screenShotTask.setWxid(driver.getWxid());
                screenShotTask.setStatus(TypeStringUtils.wechat_status3);
                screenShotTask.setContent(TypeStringUtils.getWechatContent(fengXian.getDangerType()));
                screenShotTaskRepository.save(screenShotTask);
            }
        }
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
                // 图片转base64
                if (StringUtils.hasText(fengXian.getFilePath())) {
                    File saveFile = new File(savePath, fengXian.getFilePath());
                    FileInputStream inputFile = null;
                    try {
                        if (!saveFile.exists()) {
                            continue;
                        }
                        inputFile = new FileInputStream(saveFile);
                        byte[] buffer = new byte[(int) saveFile.length()];
                        inputFile.read(buffer);
                        inputFile.close();
                        dto.setBase64(new BASE64Encoder().encode(buffer));
                    } catch (Exception ignored) {

                    } finally {
                        assert inputFile != null;
                        try {
                            inputFile.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                dtos.add(dto);
            }
        }
        return PageResult.of(dtos, params.getPage(), params.getPageSize(), details.getTotalElements());
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
