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

/**
 * 用户注册、登录与 Profile 查询。
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final UserApiConfigMapper userApiConfigMapper;
    private final UserKnowledgeConfigMapper userKnowledgeConfigMapper;

    /**
     * 注册新用户，并初始化 per-user 配置行。
     * <p>
     * 同一事务内创建：users、user_api_configs（DeepSeek 默认）、user_knowledge_configs。
     */
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

        // 默认 LLM 配置：DeepSeek，Key 留空待用户在设置页填写
        UserApiConfig apiConfig = new UserApiConfig();
        apiConfig.setUserId(user.getId());
        apiConfig.setApiKey("");
        apiConfig.setBaseUrl("https://api.deepseek.com");
        apiConfig.setModel("deepseek-chat");
        userApiConfigMapper.insert(apiConfig);

        // 知识库配置占位，v0.7 启用 RAG 时使用
        UserKnowledgeConfig knowledgeConfig = new UserKnowledgeConfig();
        knowledgeConfig.setUserId(user.getId());
        userKnowledgeConfigMapper.insert(knowledgeConfig);
    }

    /**
     * 校验用户名密码，签发 Sa-Token。
     *
     * @return token 字符串及用户基本信息
     */
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

    /** 注销当前 token 会话。 */
    public void logout() {
        StpUtil.logout();
    }

    /**
     * 根据 Sa-Token 登录 ID 查询用户 Profile。
     */
    public UserProfileResponse currentUser() {
        long userId = StpUtil.getLoginIdAsLong();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return new UserProfileResponse(user.getId(), user.getUsername());
    }
}
