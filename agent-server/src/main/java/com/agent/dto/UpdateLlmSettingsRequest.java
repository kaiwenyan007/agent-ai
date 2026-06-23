package com.agent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateLlmSettingsRequest {

    /** 留空表示不修改已有 Key */
    @Size(max = 512, message = "API Key 长度不能超过 512")
    private String apiKey;

    @NotBlank(message = "Base URL 不能为空")
    @Size(max = 256, message = "Base URL 长度不能超过 256")
    private String baseUrl;

    @NotBlank(message = "Model 不能为空")
    @Size(max = 128, message = "Model 长度不能超过 128")
    private String model;
}
