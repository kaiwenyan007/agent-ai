package com.agent.cli;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONUtil;
import com.agent.dto.LoginResponse;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * CLI 登录态持久化（~/.agent-ai/cli-session.json）。
 */
@Component
public class CliSessionManager {

    private static final Path SESSION_FILE =
            Paths.get(System.getProperty("user.home"), ".agent-ai", "cli-session.json");

    private LoginResponse current;

    public boolean isLoggedIn() {
        return current != null || tryRestoreFromDisk();
    }

    public Optional<LoginResponse> currentSession() {
        if (current != null) {
            return Optional.of(current);
        }
        if (tryRestoreFromDisk()) {
            return Optional.ofNullable(current);
        }
        return Optional.empty();
    }

    public void saveSession(LoginResponse login) {
        current = login;
        FileUtil.mkdir(SESSION_FILE.getParent().toString());
        FileUtil.writeString(JSONUtil.toJsonPrettyStr(toStored(login)), SESSION_FILE.toString(), StandardCharsets.UTF_8);
    }

    public void clearSession() {
        current = null;
        if (StpUtil.isLogin()) {
            StpUtil.logout();
        }
        if (FileUtil.exist(SESSION_FILE.toFile())) {
            FileUtil.del(SESSION_FILE.toFile());
        }
    }

    public long requireUserId() {
        if (!isLoggedIn()) {
            throw new IllegalStateException("未登录");
        }
        return StpUtil.getLoginIdAsLong();
    }

    private boolean tryRestoreFromDisk() {
        if (!FileUtil.exist(SESSION_FILE.toFile())) {
            return false;
        }
        try {
            StoredSession stored = JSONUtil.toBean(
                    FileUtil.readString(SESSION_FILE.toFile(), StandardCharsets.UTF_8),
                    StoredSession.class
            );
            if (stored == null || stored.getToken() == null || stored.getUserId() == null) {
                return false;
            }
            StpUtil.setTokenValue(stored.getToken());
            StpUtil.checkLogin();
            current = new LoginResponse(stored.getToken(), stored.getUsername(), stored.getUserId());
            return true;
        } catch (Exception ex) {
            FileUtil.del(SESSION_FILE.toFile());
            return false;
        }
    }

    private static StoredSession toStored(LoginResponse login) {
        StoredSession stored = new StoredSession();
        stored.setToken(login.getToken());
        stored.setUsername(login.getUsername());
        stored.setUserId(login.getUserId());
        return stored;
    }

    @Data
    private static class StoredSession {
        private String token;
        private String username;
        private Long userId;
    }
}
