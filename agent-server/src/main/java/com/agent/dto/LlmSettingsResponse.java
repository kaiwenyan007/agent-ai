package com.agent.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LlmSettingsResponse {

    private String baseUrl;
    private String model;
    /** 脱敏后的 API Key，未配置时为空 */
    private String apiKeyMasked;
    private boolean configured;
}
