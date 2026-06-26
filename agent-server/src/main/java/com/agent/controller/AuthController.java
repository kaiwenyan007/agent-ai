package com.agent.controller;

import com.agent.common.ApiResponse;
import com.agent.dto.LoginRequest;
import com.agent.dto.LoginResponse;
import com.agent.dto.RegisterRequest;
import com.agent.dto.UserProfileResponse;
import com.agent.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户鉴权接口。
 * <p>
 * 公开接口：register、login；其余需 Bearer Token。
 * 详见 {@code doc/api-reference.md}。
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 用户注册。
     *
     * @param request 用户名（≥2 位）、密码（≥6 位）
     * @return 成功时 data 为 null
     */
    @PostMapping("/register")
    public ApiResponse<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ApiResponse.ok(null);
    }

    /**
     * 用户登录。
     *
     * @param request 用户名、密码
     * @return token（Sa-Token）、username、userId
     */
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    /**
     * 注销当前登录会话。
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        authService.logout();
        return ApiResponse.ok(null);
    }

    /**
     * 获取当前登录用户基本信息。
     */
    @GetMapping("/me")
    public ApiResponse<UserProfileResponse> me() {
        return ApiResponse.ok(authService.currentUser());
    }
}
