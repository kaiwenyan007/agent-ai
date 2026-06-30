package com.agent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;

/** Agent 运行时配置（项目根路径等）。 */
@Data
@Component
@ConfigurationProperties(prefix = "agent")
public class AgentProperties {

    /** 项目根目录，用于 readProjectFile / listProjectFiles 路径沙箱。 */
    private String projectRoot = ".";

    public Path resolvedProjectRoot() {
        return Paths.get(projectRoot).toAbsolutePath().normalize();
    }
}
