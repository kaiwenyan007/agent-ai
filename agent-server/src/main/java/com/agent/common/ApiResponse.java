package com.agent.common;

import lombok.Getter;
import lombok.Setter;

/**
 * 统一 REST 响应包装。
 * <p>
 * 成功：{@code code=0, message="ok", data=...}；失败：{@code code=1, message=错误描述}。
 */
@Getter
@Setter
public class ApiResponse<T> {

    /** 0 成功，非 0 失败 */
    private int code;

    /** 提示信息 */
    private String message;

    /** 业务数据，失败时可为 null */
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
