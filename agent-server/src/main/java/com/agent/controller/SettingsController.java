package com.agent.controller;

import com.agent.common.ApiResponse;
import com.agent.dto.LlmSettingsResponse;
import com.agent.dto.ModelsResponse;
import com.agent.dto.UpdateLlmSettingsRequest;
import com.agent.service.LlmSettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final LlmSettingsService llmSettingsService;

    @GetMapping("/llm")
    public ApiResponse<LlmSettingsResponse> getLlmSettings() {
        return ApiResponse.ok(llmSettingsService.getCurrentUserSettings());
    }

    @PutMapping("/llm")
    public ApiResponse<LlmSettingsResponse> updateLlmSettings(
            @Valid @RequestBody UpdateLlmSettingsRequest request) {
        return ApiResponse.ok(llmSettingsService.saveCurrentUserSettings(request));
    }

    @GetMapping("/models")
    public ApiResponse<ModelsResponse> listModels() {
        return ApiResponse.ok(llmSettingsService.listModels());
    }
}
