package com.agent.config;

import cn.hutool.core.util.StrUtil;
import com.agent.entity.UserApiConfig;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * CLI simple 模式配置（对标 Python {@code main.py} 读 .env）。
 * <p>
 * 配置项：{@code agent.cli.llm.*} 或环境变量 OPENAI_API_KEY / OPENAI_BASE_URL / OPENAI_MODEL。
 */
@Data
@Component
@ConfigurationProperties(prefix = "agent.cli")
public class CliProperties {

    private LlmConfig llm = new LlmConfig();

    @Data
    public static class LlmConfig {
        private String apiKey = "";
        private String baseUrl = "https://api.deepseek.com";
        private String model = "deepseek-chat";
    }

    public boolean isSimpleConfigured() {
        return StrUtil.isAllNotBlank(llm.getApiKey(), llm.getBaseUrl(), llm.getModel());
    }

    public UserApiConfig toApiConfig() {
        UserApiConfig config = new UserApiConfig();
        config.setApiKey(StrUtil.trim(llm.getApiKey()));
        config.setBaseUrl(StrUtil.trim(llm.getBaseUrl()));
        config.setModel(StrUtil.trim(llm.getModel()));
        return config;
    }
}
