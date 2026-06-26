package com.agent.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/** GET/PUT /api/settings/llm 响应 data */
@Data
@AllArgsConstructor
public class LlmSettingsResponse {

    private String baseUrl;

    private String model;

    /** 脱敏后的 API Key，如 sk-a****xyz */
    private String apiKeyMasked;

    /** Key + URL + Model 均已配置时为 true */
    private boolean configured;
}
