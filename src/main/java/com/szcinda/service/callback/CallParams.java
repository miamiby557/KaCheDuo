package com.szcinda.service.callback;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class CallParams implements Serializable {
    private String templateId;
    private String phone;
    private List<String> params;
    private String dataId;
}
