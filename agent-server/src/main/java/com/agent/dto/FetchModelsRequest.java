package com.agent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class FetchModelsRequest {

    @NotBlank(message = "Base URL 不能为空")
    @Size(max = 256, message = "Base URL 长度不能超过 256")
    private String baseUrl;

    /** 留空则使用已保存的 API Key */
    @Size(max = 512, message = "API Key 长度不能超过 512")
    private String apiKey;
}
