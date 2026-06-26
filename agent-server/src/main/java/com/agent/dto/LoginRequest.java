package com.agent.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** POST /api/auth/login 请求体 */
@Data
public class LoginRequest {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;
}
