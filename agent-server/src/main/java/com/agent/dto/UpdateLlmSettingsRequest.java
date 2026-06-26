package com.agent.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** PUT /api/settings/llm 请求体 */
@Data
public class UpdateLlmSettingsRequest {

    @NotBlank(message = "baseUrl 不能为空")
    private String baseUrl;

    @NotBlank(message = "model 不能为空")
    private String model;

    /** 留空表示不修改已保存的 Key */
    private String apiKey;
}
