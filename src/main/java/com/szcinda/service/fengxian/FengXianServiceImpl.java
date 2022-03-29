package com.szcinda.service.fengxian;

import com.szcinda.repository.FengXian;
import com.szcinda.repository.FengXianRepository;
import com.szcinda.repository.Robot;
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

    public FengXianServiceImpl(FengXianRepository fengXianRepository, RobotRepository robotRepository) {
        this.fengXianRepository = fengXianRepository;
        this.robotRepository = robotRepository;
        this.snowFlakeFactory = SnowFlakeFactory.getInstance();
    }

    @Override
    public void create(CreateFengXianDto dto) {
        FengXian fengXian = fengXianRepository.findByVehicleNoAndHappenTime(dto.getVehicleNo(), dto.getHappenTime());
        if (fengXian == null) {
            fengXian = new FengXian();
            BeanUtils.copyProperties(dto, fengXian);
            fengXian.setId(snowFlakeFactory.nextId("FX"));
            fengXianRepository.save(fengXian);
        }
    }

    @Override
    public void batchCreate(List<CreateFengXianDto> dtos) {
        Robot robot = robotRepository.findByPhone(dtos.get(0).getOwner());
        for (CreateFengXianDto dto : dtos) {
            if(robot!=null){
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
            fengXianRepository.save(fengXian);
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
}
