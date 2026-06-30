package com.agent.cli;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Agent AI CLI 入口（非 Web 模式，复用 agent-server 服务层）。
 */
@SpringBootApplication(scanBasePackages = "com.agent")
@MapperScan("com.agent.**.mapper")
public class AgentCliApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(AgentCliApplication.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        app.run(args);
    }
}
