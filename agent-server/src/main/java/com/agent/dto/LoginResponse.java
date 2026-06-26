package com.agent.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/** POST /api/auth/login 响应 data */
@Data
@AllArgsConstructor
public class LoginResponse {

    /** Sa-Token，请求头 Authorization: Bearer {token} */
    private String token;

    private String username;

    private Long userId;
}
