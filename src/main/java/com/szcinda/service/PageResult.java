package com.szcinda.service;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class PageResult<T> implements Serializable {
    private List<T> content;
    private int page;
    private int pageSize;
    private long totalElements;


    public static <T> PageResult<T> of(List<T> content, int page, int pageSize, long totalElements) {
        PageResult<T> pageResult = new PageResult<>();
        pageResult.setContent(content);
        pageResult.setPage(page);
        pageResult.setPageSize(pageSize);
        pageResult.setTotalElements(totalElements);
        return pageResult;
    }


}
