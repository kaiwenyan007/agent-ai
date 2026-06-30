package com.agent.cli.command;

import com.agent.cli.ChatRepl;
import com.agent.cli.CliAuthHelper;
import com.agent.cli.CliSessionManager;
import com.agent.cli.ConfigWizard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

/**
 * 根命令：默认进入聊天 REPL。
 */
@Component
@RequiredArgsConstructor
@Command(
        name = "agent",
        mixinStandardHelpOptions = true,
        version = "0.1.0",
        description = "Agent AI 命令行客户端",
        subcommands = {
                ChatCommand.class,
                LoginCommand.class,
                RegisterCommand.class,
                ConfigCommand.class,
                LogoutCommand.class
        }
)
public class AgentCommand implements Callable<Integer> {

    private final ChatRepl chatRepl;

    @Option(names = {"--simple", "-s"}, description = "simple 模式：使用环境变量/配置文件，无需登录")
    boolean simple;

    @Override
    public Integer call() {
        return chatRepl.run(simple);
    }
}

@Component
@RequiredArgsConstructor
@Command(name = "chat", description = "进入交互式聊天（默认命令）")
class ChatCommand implements Callable<Integer> {

    private final ChatRepl chatRepl;

    @Option(names = {"--simple", "-s"}, description = "simple 模式")
    boolean simple;

    @Override
    public Integer call() {
        return chatRepl.run(simple);
    }
}

@Component
@RequiredArgsConstructor
@Command(name = "login", description = "登录账号")
class LoginCommand implements Runnable {

    private final CliAuthHelper authHelper;
    private final ConfigWizard configWizard;
    private final CliSessionManager sessionManager;

    @Override
    public void run() {
        authHelper.loginInteractive();
        long userId = sessionManager.requireUserId();
        if (!configWizard.ensureConfigured(userId)) {
            throw new IllegalStateException("API 配置未完成");
        }
    }
}

@Component
@RequiredArgsConstructor
@Command(name = "register", description = "注册新账号")
class RegisterCommand implements Runnable {

    private final CliAuthHelper authHelper;

    @Override
    public void run() {
        authHelper.registerInteractive();
    }
}

@Component
@RequiredArgsConstructor
@Command(name = "config", description = "配置 LLM API（需已登录）")
class ConfigCommand implements Runnable {

    private final CliAuthHelper authHelper;
    private final ConfigWizard configWizard;
    private final CliSessionManager sessionManager;

    @Override
    public void run() {
        authHelper.ensureLoggedIn();
        configWizard.runWizard(sessionManager.requireUserId());
    }
}

@Component
@RequiredArgsConstructor
@Command(name = "logout", description = "退出登录")
class LogoutCommand implements Runnable {

    private final CliAuthHelper authHelper;

    @Override
    public void run() {
        authHelper.logout();
    }
}
