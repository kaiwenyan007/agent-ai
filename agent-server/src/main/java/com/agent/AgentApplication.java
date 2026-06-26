package com.agent;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Agent AI 后端入口。
 * 需 JDK 21；默认端口 8080。
 */
@SpringBootApplication
@MapperScan("com.agent.**.mapper")
public class AgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgentApplication.class, args);
    }
}
