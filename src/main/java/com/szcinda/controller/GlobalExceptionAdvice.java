package com.szcinda.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionAdvice {


    @ExceptionHandler(Exception.class)
    public Result handleException(Exception ex) {
        log.error("应用程序错误", ex);
        return Result.fail(ex.getMessage());
    }
}
