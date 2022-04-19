package com.szcinda.service.phone;

import com.szcinda.repository.Phone;
import com.szcinda.repository.PhoneRepository;
import com.szcinda.service.SnowFlakeFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Transactional
public class PhoneServiceImpl implements PhoneService {
    private final PhoneRepository phoneRepository;
    private final SnowFlakeFactory snowFlakeFactory;

    private static final ConcurrentHashMap<String, LocalDateTime> phoneMap = new ConcurrentHashMap<>();

    public PhoneServiceImpl(PhoneRepository phoneRepository) {
        this.phoneRepository = phoneRepository;
        this.snowFlakeFactory = SnowFlakeFactory.getInstance();
    }

    @Override
    public void create(String phone) {
        Phone record = new Phone();
        record.setPhone(phone);
        record.setId(snowFlakeFactory.nextId("PH"));
        phoneRepository.save(record);
    }

    @Override
    public void delete(String id) {
        phoneRepository.delete(id);
    }

    @Override
    public List<PhoneDto> getAll() {
        List<Phone> phones = phoneRepository.findAll();
        List<PhoneDto> dtos = new ArrayList<>();
        for (Phone phone : phones) {
            PhoneDto dto = new PhoneDto();
            BeanUtils.copyProperties(phone, dto);
            if (phoneMap.containsKey(phone.getPhone())) {
                dto.setTime(phoneMap.get(phone.getPhone()));
            }
            dtos.add(dto);
        }
        return dtos;
    }

    @Override
    public void alive(String phone) {
        phoneMap.put(phone, LocalDateTime.now());
    }
}
