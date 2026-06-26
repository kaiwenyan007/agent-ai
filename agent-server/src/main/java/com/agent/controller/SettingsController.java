package com.agent.controller;

import com.agent.common.ApiResponse;
import com.agent.dto.FetchModelsRequest;
import com.agent.dto.LlmSettingsResponse;
import com.agent.dto.ModelsResponse;
import com.agent.dto.UpdateLlmSettingsRequest;
import com.agent.service.LlmSettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户 LLM API 配置接口。
 * <p>
 * 配置按用户隔离，存储于 {@code user_api_configs} 表。
 * 聊天前需 {@code configured=true}（Key + Base URL + Model 均非空）。
 */
@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final LlmSettingsService llmSettingsService;

    /**
     * 读取当前用户 LLM 配置（API Key 脱敏展示）。
     */
    @GetMapping("/llm")
    public ApiResponse<LlmSettingsResponse> getLlmSettings() {
        return ApiResponse.ok(llmSettingsService.getCurrentUserSettings());
    }

    /**
     * 保存 LLM 配置。
     * <p>
     * apiKey 留空表示不修改已保存的 Key。
     */
    @PutMapping("/llm")
    public ApiResponse<LlmSettingsResponse> updateLlmSettings(
            @Valid @RequestBody UpdateLlmSettingsRequest request) {
        return ApiResponse.ok(llmSettingsService.saveCurrentUserSettings(request));
    }

    /**
     * 使用已保存的 baseUrl / apiKey 拉取远程模型列表。
     */
    @GetMapping("/models")
    public ApiResponse<ModelsResponse> listModels() {
        return ApiResponse.ok(llmSettingsService.listModels());
    }

    /**
     * 使用请求体中的 baseUrl / apiKey 拉取模型列表。
     * <p>
     * 适用于配置页「FETCH MODELS」：Key 可留空，将回退到数据库中已保存的 Key。
     */
    @PostMapping("/models")
    public ApiResponse<ModelsResponse> fetchModels(@Valid @RequestBody FetchModelsRequest request) {
        return ApiResponse.ok(llmSettingsService.fetchModels(request));
    }
}
