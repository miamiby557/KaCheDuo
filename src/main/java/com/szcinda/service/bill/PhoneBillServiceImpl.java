package com.szcinda.service.bill;

import com.szcinda.repository.PhoneBill;
import com.szcinda.repository.PhoneBillRepository;
import com.szcinda.service.PageResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class PhoneBillServiceImpl implements PhoneBillService {

    private final PhoneBillRepository phoneBillRepository;

    public PhoneBillServiceImpl(PhoneBillRepository phoneBillRepository) {
        this.phoneBillRepository = phoneBillRepository;
    }

    @Override
    public PageResult<PhoneBill> query(BillQuery params) {
        Specification<PhoneBill> specification = ((root, criteriaQuery, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (!StringUtils.isEmpty(params.getPhone())) {
                Predicate called = criteriaBuilder.equal(root.get("called"), params.getPhone());
                predicates.add(called);
            }
            if (params.getCallTimeStart() != null) {
                Predicate timeStart = criteriaBuilder.greaterThanOrEqualTo(root.get("callCreateTime"), params.getCallTimeStart().atStartOfDay());
                predicates.add(timeStart);
            }
            if (params.getCallTimeEnd() != null) {
                Predicate timeEnd = criteriaBuilder.lessThan(root.get("callCreateTime"), params.getCallTimeEnd().plusDays(1).atStartOfDay());
                predicates.add(timeEnd);
            }
            if (StringUtils.hasText(params.getStatus())) {
                Predicate status = criteriaBuilder.equal(root.get("status"), params.getStatus());
                predicates.add(status);
            }
            if (StringUtils.hasText(params.getVehicleNo())) {
                Predicate vehicleNo = criteriaBuilder.equal(root.get("vehicleNo"), params.getVehicleNo());
                predicates.add(vehicleNo);
            }
            if (StringUtils.hasText(params.getAccount())) {
                Predicate account = criteriaBuilder.equal(root.get("account"), params.getAccount());
                predicates.add(account);
            }
            if (StringUtils.hasText(params.getCompany())) {
                Predicate company = criteriaBuilder.equal(root.get("company"), params.getCompany());
                predicates.add(company);
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        });
        Sort order = new Sort(Sort.Direction.DESC, "callCreateTime");
        Pageable pageable = new PageRequest(params.getPage() - 1, params.getPageSize(), order);
        Page<PhoneBill> details = phoneBillRepository.findAll(specification, pageable);
        return PageResult.of(details.getContent(), params.getPage(), params.getPageSize(), details.getTotalElements());
    }

    @Override
    public List<PhoneBill> queryAll(BillQuery params) {
        Specification<PhoneBill> specification = ((root, criteriaQuery, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (!StringUtils.isEmpty(params.getPhone())) {
                Predicate called = criteriaBuilder.equal(root.get("called"), params.getPhone());
                predicates.add(called);
            }
            if (params.getCallTimeStart() == null || params.getCallTimeEnd() == null) {
                LocalDate now = LocalDate.now();
                Predicate timeStart = criteriaBuilder.greaterThanOrEqualTo(root.get("callCreateTime"), now.atStartOfDay());
                predicates.add(timeStart);
                Predicate timeEnd = criteriaBuilder.lessThan(root.get("callCreateTime"), now.plusDays(1).atStartOfDay());
                predicates.add(timeEnd);
            } else {
                if (params.getCallTimeStart() != null) {
                    Predicate timeStart = criteriaBuilder.greaterThanOrEqualTo(root.get("callCreateTime"), params.getCallTimeStart().atStartOfDay());
                    predicates.add(timeStart);
                }
                if (params.getCallTimeEnd() != null) {
                    Predicate timeEnd = criteriaBuilder.lessThan(root.get("callCreateTime"), params.getCallTimeEnd().plusDays(1).atStartOfDay());
                    predicates.add(timeEnd);
                }
            }
            if (StringUtils.hasText(params.getStatus())) {
                Predicate status = criteriaBuilder.equal(root.get("status"), params.getStatus());
                predicates.add(status);
            }
            if (StringUtils.hasText(params.getVehicleNo())) {
                Predicate vehicleNo = criteriaBuilder.equal(root.get("vehicleNo"), params.getVehicleNo());
                predicates.add(vehicleNo);
            }
            if (StringUtils.hasText(params.getAccount())) {
                Predicate account = criteriaBuilder.equal(root.get("account"), params.getAccount());
                predicates.add(account);
            }
            if (StringUtils.hasText(params.getCompany())) {
                Predicate company = criteriaBuilder.equal(root.get("company"), params.getCompany());
                predicates.add(company);
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        });
        return phoneBillRepository.findAll(specification);
    }
}
