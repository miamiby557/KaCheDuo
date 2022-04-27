package com.szcinda.service.location;

import com.szcinda.repository.Location;
import com.szcinda.repository.LocationRepository;
import com.szcinda.repository.Robot;
import com.szcinda.repository.RobotRepository;
import com.szcinda.service.PageResult;
import com.szcinda.service.SnowFlakeFactory;
import com.szcinda.service.robotTask.RobotTaskServiceImpl;
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
public class LocationServiceImpl implements LocationService {

    private final LocationRepository locationRepository;
    private final SnowFlakeFactory snowFlakeFactory;
    private final RobotRepository robotRepository;

    public LocationServiceImpl(LocationRepository locationRepository, RobotRepository robotRepository) {
        this.locationRepository = locationRepository;
        this.robotRepository = robotRepository;
        this.snowFlakeFactory = SnowFlakeFactory.getInstance();
    }

    @Override
    public void create(CreateLocationDto dto) {
        Location location = new Location();
        BeanUtils.copyProperties(dto, location);
        location.setId(snowFlakeFactory.nextId("L"));
        locationRepository.save(location);
    }

    @Override
    public void batchCreate(List<CreateLocationDto> dtos) {
        if (dtos != null && dtos.size() > 0) {
            Robot robot = robotRepository.findByAccount2(dtos.get(0).getOwner());
            for (CreateLocationDto dto : dtos) {
                if (robot != null) {
                    dto.setUserCompany(robot.getCompany());
                }
                this.create(dto);
            }
            // 删除正在运行的账号
            String owner = dtos.get(0).getOwner();
            RobotTaskServiceImpl.handleAccountMap.remove(owner);
        }

    }

    @Override
    public PageResult<LocationDto> query(LocationQuery params) {
        List<Robot> robots = robotRepository.findByOwner(params.getOwner());
        Specification<Location> specification = ((root, criteriaQuery, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (!StringUtils.isEmpty(params.getVehicleNo())) {
                Predicate vehicleNo = criteriaBuilder.equal(root.get("vehicleNo"), params.getVehicleNo());
                predicates.add(vehicleNo);
            }
            if (params.getHappenTimeStart() != null) {
                Predicate happenTimeStart = criteriaBuilder.greaterThanOrEqualTo(root.get("happenTime"), params.getHappenTimeStart().atStartOfDay());
                predicates.add(happenTimeStart);
            }
            if (params.getHappenTimeEnd() != null) {
                Predicate happenTimeEnd = criteriaBuilder.lessThan(root.get("happenTime"), params.getHappenTimeEnd().plusDays(1).atStartOfDay());
                predicates.add(happenTimeEnd);
            }
            if (params.getCreateTimeStart() != null) {
                Predicate createTimeStart = criteriaBuilder.greaterThanOrEqualTo(root.get("createTime"), params.getCreateTimeStart().atStartOfDay());
                predicates.add(createTimeStart);
            }
            if (params.getCreateTimeEnd() != null) {
                Predicate createTimeEnd = criteriaBuilder.lessThan(root.get("createTime"), params.getCreateTimeEnd().plusDays(1).atStartOfDay());
                predicates.add(createTimeEnd);
            }
            if (StringUtils.hasText(params.getUserName())) {
                Predicate owner = criteriaBuilder.equal(root.get("owner"), params.getUserName());
                predicates.add(owner);
            }
            if (StringUtils.hasText(params.getUserCompany())) {
                Predicate userCompany = criteriaBuilder.equal(root.get("userCompany"), params.getUserCompany());
                predicates.add(userCompany);
            }
            // 过滤出位置监控的帐号
            List<String> phones = robots.stream().filter(robot -> StringUtils.hasText(robot.getAccount2())).map(Robot::getAccount2).collect(Collectors.toList());
            Expression<String> exp = root.get("owner");
            predicates.add(exp.in(phones));
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        });
        Sort order = new Sort(Sort.Direction.DESC, "createTime");
        Pageable pageable = new PageRequest(params.getPage() - 1, params.getPageSize(), order);
        Page<Location> details = locationRepository.findAll(specification, pageable);
        List<LocationDto> dtos = new ArrayList<>();
        if (details.getContent() != null) {
            for (Location location : details.getContent()) {
                LocationDto dto = new LocationDto();
                BeanUtils.copyProperties(location, dto);
                dtos.add(dto);
            }
        }
        return PageResult.of(dtos, params.getPage(), params.getPageSize(), details.getTotalElements());
    }
}
