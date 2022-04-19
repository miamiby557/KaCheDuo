package com.szcinda.service.phone;

import com.szcinda.repository.Phone;

import java.util.List;

public interface PhoneService {
    void create(String phone);
    void delete(String id);
    List<PhoneDto> getAll();

    void alive(String phone);
}
