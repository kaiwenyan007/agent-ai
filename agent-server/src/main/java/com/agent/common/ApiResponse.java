package com.agent.common;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApiResponse<T> {

    private int code;
    private String message;
    private T data;

    public static <T> ApiResponse<T> ok(T data) {
        ApiResponse<T> resp = new ApiResponse<>();
        resp.code = 0;
        resp.message = "ok";
        resp.data = data;
        return resp;
    }

    public static <T> ApiResponse<T> fail(String message) {
        ApiResponse<T> resp = new ApiResponse<>();
        resp.code = 1;
        resp.message = message;
        return resp;
    }
}
