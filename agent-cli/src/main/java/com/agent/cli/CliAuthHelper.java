package com.agent.cli;

import com.agent.dto.LoginRequest;
import com.agent.dto.LoginResponse;
import com.agent.dto.RegisterRequest;
import com.agent.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * CLI 登录 / 注册交互。
 */
@Component
@RequiredArgsConstructor
public class CliAuthHelper {

    private final AuthService authService;
    private final CliSessionManager sessionManager;
    private final ConsoleIO io;

    public LoginResponse loginInteractive() {
        io.println("");
        io.println("=== 登录 Agent AI ===");
        String username = io.readLine("用户名: ");
        String password = io.readPassword("密码: ");
        LoginRequest request = new LoginRequest();
        request.setUsername(username);
        request.setPassword(password);
        LoginResponse response = authService.login(request);
        sessionManager.saveSession(response);
        io.println("登录成功，欢迎 " + response.getUsername() + "。");
        return response;
    }

    public void registerInteractive() {
        io.println("");
        io.println("=== 注册 Agent AI ===");
        String username = io.readLine("用户名: ");
        String password = io.readPassword("密码: ");
        String confirm = io.readPassword("确认密码: ");
        if (!password.equals(confirm)) {
            throw new IllegalArgumentException("两次密码不一致");
        }
        RegisterRequest request = new RegisterRequest();
        request.setUsername(username);
        request.setPassword(password);
        authService.register(request);
        io.println("注册成功，请登录。");
    }

    /**
     * 确保已登录；未登录时进入交互式登录。
     */
    public LoginResponse ensureLoggedIn() {
        return sessionManager.currentSession().orElseGet(this::loginInteractive);
    }

    public void logout() {
        if (sessionManager.isLoggedIn()) {
            authService.logout();
        }
        sessionManager.clearSession();
        io.println("已退出登录。");
    }
}
