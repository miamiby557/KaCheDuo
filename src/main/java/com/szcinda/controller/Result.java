package com.szcinda.controller;

import lombok.Data;

import java.io.Serializable;

@Data
public class Result<T> implements Serializable {
    private boolean success;
    private String message;
    private T content;

    private Result() {
    }

    public static Result<String> fail(String message) {
        Result<String> result = new Result<>();
        result.setMessage(message);
        result.setSuccess(false);
        return result;
    }

    public static Result<String> success() {
        Result<String> result = new Result<>();
        result.setSuccess(true);
        return result;
    }

    public static<T> Result<T> success(T content) {
        Result<T> result = new Result<T>();
        result.setSuccess(true);
        result.setContent(content);
        return result;
    }
}
