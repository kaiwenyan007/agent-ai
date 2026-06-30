package com.agent.cli;

import cn.hutool.core.util.StrUtil;
import com.agent.dto.LlmSettingsResponse;
import com.agent.dto.ModelsResponse;
import com.agent.dto.UpdateLlmSettingsRequest;
import com.agent.service.LlmSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * CLI 交互式 LLM API 配置向导（对标 Web Settings 页）。
 */
@Component
@RequiredArgsConstructor
public class ConfigWizard {

    private final LlmSettingsService llmSettingsService;
    private final ConsoleIO io;

    /**
     * 若用户尚未完成 API 配置，引导完成配置；已配置则跳过。
     *
     * @return true 表示配置已就绪
     */
    public boolean ensureConfigured(long userId) {
        if (llmSettingsService.isApiConfigured(userId)) {
            return true;
        }
        io.println("");
        io.println("尚未配置 LLM API，请按提示完成配置（对标 Web 端 API CONFIG）。");
        runWizard(userId);
        return llmSettingsService.isApiConfigured(userId);
    }

    /** 强制进入配置向导。 */
    public void runWizard(long userId) {
        LlmSettingsResponse current = llmSettingsService.getSettings(userId);

        io.println("");
        io.println("=== LLM API 配置 ===");
        String baseUrl = io.readLineOrDefault(
                "Base URL [" + defaultBaseUrl(current) + "]: ",
                defaultBaseUrl(current)
        );

        String apiKey;
        if (current.isConfigured() && StrUtil.isNotBlank(current.getApiKeyMasked())) {
            apiKey = io.readLine("API Key（留空保持已保存的 Key）: ");
        } else {
            apiKey = io.readPassword("API Key: ");
            while (StrUtil.isBlank(apiKey)) {
                io.println("API Key 不能为空。");
                apiKey = io.readPassword("API Key: ");
            }
        }

        ModelsResponse models = llmSettingsService.fetchModelsForUser(userId, baseUrl, apiKey);
        List<String> modelList = models.getModels();
        if (modelList.isEmpty()) {
            throw new IllegalStateException("无法获取模型列表，请检查 Base URL 与 API Key。");
        }

        io.println("");
        if (models.isFromRemote()) {
            io.println("已从远程拉取模型列表：");
        } else {
            io.println("远程拉取失败，使用默认模型列表：");
        }
        for (int i = 0; i < modelList.size(); i++) {
            io.println("  " + (i + 1) + ". " + modelList.get(i));
        }

        String defaultModel = StrUtil.blankToDefault(current.getModel(), modelList.getFirst());
        int defaultIndex = Math.max(0, modelList.indexOf(defaultModel));
        int choice = readModelChoice(defaultIndex + 1, modelList.size());
        String model = modelList.get(choice - 1);

        UpdateLlmSettingsRequest request = new UpdateLlmSettingsRequest();
        request.setBaseUrl(baseUrl);
        request.setModel(model);
        request.setApiKey(apiKey);
        LlmSettingsResponse saved = llmSettingsService.saveSettings(userId, request);

        io.println("");
        io.println("配置已保存：");
        io.println("  Base URL: " + saved.getBaseUrl());
        io.println("  Model:    " + saved.getModel());
        io.println("  API Key:  " + saved.getApiKeyMasked());
    }

    private int readModelChoice(int defaultChoice, int max) {
        String line = io.readLine("选择模型编号 [" + defaultChoice + "]: ");
        if (line.isEmpty()) {
            return defaultChoice;
        }
        try {
            int value = Integer.parseInt(line);
            if (value >= 1 && value <= max) {
                return value;
            }
        } catch (NumberFormatException ignored) {
            // fall through
        }
        io.println("无效选择，使用默认 " + defaultChoice + "。");
        return defaultChoice;
    }

    private static String defaultBaseUrl(LlmSettingsResponse current) {
        return StrUtil.blankToDefault(current.getBaseUrl(), "https://api.deepseek.com");
    }
}
