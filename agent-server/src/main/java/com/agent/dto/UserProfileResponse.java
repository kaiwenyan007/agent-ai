package com.agent.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/** GET /api/auth/me 响应 data */
@Data
@AllArgsConstructor
public class UserProfileResponse {

    private Long id;

    private String username;
}
