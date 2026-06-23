package com.agent.service;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.agent.common.BusinessException;
import com.agent.dto.LoginRequest;
import com.agent.dto.LoginResponse;
import com.agent.dto.RegisterRequest;
import com.agent.dto.UserProfileResponse;
import com.agent.entity.User;
import com.agent.entity.UserApiConfig;
import com.agent.entity.UserKnowledgeConfig;
import com.agent.mapper.UserApiConfigMapper;
import com.agent.mapper.UserKnowledgeConfigMapper;
import com.agent.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final UserApiConfigMapper userApiConfigMapper;
    private final UserKnowledgeConfigMapper userKnowledgeConfigMapper;

    @Transactional
    public void register(RegisterRequest request) {
        String username = StrUtil.trim(request.getUsername());
        String password = request.getPassword();

        if (StrUtil.isBlank(username) || StrUtil.isBlank(password)) {
            throw new BusinessException("用户名和密码不能为空");
        }
        if (username.length() < 2) {
            throw new BusinessException("用户名长度不能少于 2 位");
        }
        if (password.length() < 6) {
            throw new BusinessException("密码长度不能少于 6 位");
        }

        Long exists = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
        if (exists > 0) {
            throw new BusinessException("用户名已存在");
        }

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(BCrypt.hashpw(password));
        userMapper.insert(user);

        UserApiConfig apiConfig = new UserApiConfig();
        apiConfig.setUserId(user.getId());
        apiConfig.setApiKey("");
        apiConfig.setBaseUrl("https://api.deepseek.com");
        apiConfig.setModel("deepseek-chat");
        userApiConfigMapper.insert(apiConfig);

        UserKnowledgeConfig knowledgeConfig = new UserKnowledgeConfig();
        knowledgeConfig.setUserId(user.getId());
        userKnowledgeConfigMapper.insert(knowledgeConfig);
    }

    public LoginResponse login(LoginRequest request) {
        String username = StrUtil.trim(request.getUsername());
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
        if (user == null || !BCrypt.checkpw(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException("用户名或密码错误");
        }

        StpUtil.login(user.getId());
        return new LoginResponse(StpUtil.getTokenValue(), user.getUsername(), user.getId());
    }

    public void logout() {
        StpUtil.logout();
    }

    public UserProfileResponse currentUser() {
        long userId = StpUtil.getLoginIdAsLong();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return new UserProfileResponse(user.getId(), user.getUsername());
    }
}
