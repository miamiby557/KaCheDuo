package com.szcinda.service.phone;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class PhoneDto implements Serializable {
    private String id;
    private String phone;
    private LocalDateTime time;
}
