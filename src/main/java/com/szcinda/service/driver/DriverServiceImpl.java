package com.szcinda.service.driver;

import com.szcinda.repository.Driver;
import com.szcinda.repository.DriverRepository;
import com.szcinda.service.SnowFlakeFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class DriverServiceImpl implements DriverService {
    private final DriverRepository driverRepository;
    private final SnowFlakeFactory snowFlakeFactory;

    public DriverServiceImpl(DriverRepository driverRepository) {
        this.driverRepository = driverRepository;
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
}
