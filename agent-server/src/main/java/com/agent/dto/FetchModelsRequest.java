package com.agent.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** POST /api/settings/models 请求体 */
@Data
public class FetchModelsRequest {

    @NotBlank(message = "baseUrl 不能为空")
    private String baseUrl;

    /** 留空时使用数据库中已保存的 Key */
    private String apiKey;
}
