package com.agent.cli;

import com.agent.cli.command.AgentCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

/**
 * Picocli 与 Spring Boot 桥接。
 */
@Component
@RequiredArgsConstructor
public class CliCommandRunner implements CommandLineRunner, ExitCodeGenerator {

    private final CommandLine.IFactory factory;
    private final AgentCommand agentCommand;

    private int exitCode;

    @Override
    public void run(String... args) {
        exitCode = new CommandLine(agentCommand, factory).execute(args);
    }

    @Override
    public int getExitCode() {
        return exitCode;
    }
}
