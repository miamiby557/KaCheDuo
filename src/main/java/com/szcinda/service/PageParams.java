package com.szcinda.service;

import lombok.Data;

import java.io.Serializable;

@Data
public class PageParams implements Serializable {
    private int page = 1;
    private int pageSize = 20;
    private String sort;
}
