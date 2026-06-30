package com.agent.service;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.agent.common.BusinessException;
import com.agent.dto.FetchModelsRequest;
import com.agent.dto.LlmSettingsResponse;
import com.agent.dto.ModelsResponse;
import com.agent.dto.UpdateLlmSettingsRequest;
import com.agent.entity.UserApiConfig;
import com.agent.mapper.UserApiConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户 LLM API 配置的读写与远程模型列表拉取。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LlmSettingsService {

    /** 远程拉取失败时的本地兜底模型列表。 */
    private static final List<String> DEFAULT_MODELS = List.of("deepseek-chat", "deepseek-reasoner");

    private final UserApiConfigMapper userApiConfigMapper;

    /** 读取当前用户配置（Key 脱敏）。 */
    public LlmSettingsResponse getCurrentUserSettings() {
        UserApiConfig config = requireConfig(currentUserId());
        return toResponse(config);
    }

    /**
     * 保存 Base URL、Model；仅当 apiKey 非空时才更新 Key。
     */
    public LlmSettingsResponse saveCurrentUserSettings(UpdateLlmSettingsRequest request) {
        Long userId = currentUserId();
        UserApiConfig config = requireConfig(userId);

        config.setBaseUrl(StrUtil.trim(request.getBaseUrl()));
        config.setModel(StrUtil.trim(request.getModel()));
        if (StrUtil.isNotBlank(request.getApiKey())) {
            config.setApiKey(StrUtil.trim(request.getApiKey()));
        }

        userApiConfigMapper.updateById(config);
        return toResponse(config);
    }

    /** 用数据库中已保存的配置拉取模型。 */
    public ModelsResponse listModels() {
        UserApiConfig config = requireConfig(currentUserId());
        return fetchModels(config.getBaseUrl(), config.getApiKey());
    }

    /**
     * 用表单参数拉取模型；apiKey 为空时回退到已保存 Key。
     */
    public ModelsResponse fetchModels(FetchModelsRequest request) {
        UserApiConfig config = requireConfig(currentUserId());
        String baseUrl = StrUtil.trim(request.getBaseUrl());
        String apiKey = StrUtil.trim(request.getApiKey());
        if (StrUtil.isBlank(apiKey)) {
            apiKey = config.getApiKey();
        }
        return fetchModels(baseUrl, apiKey);
    }

    /**
     * 为指定用户拉取模型；apiKey 为空时回退到库中已保存 Key。
     */
    public ModelsResponse fetchModelsForUser(Long userId, String baseUrl, String apiKey) {
        UserApiConfig config = requireConfig(userId);
        String resolvedKey = StrUtil.isNotBlank(apiKey) ? StrUtil.trim(apiKey) : config.getApiKey();
        return fetchModels(baseUrl, resolvedKey);
    }

    /**
     * 用指定凭据拉取模型列表（CLI 配置向导使用，不要求 Key 已入库）。
     */
    public ModelsResponse fetchModelsWithCredentials(String baseUrl, String apiKey) {
        return fetchModels(baseUrl, apiKey);
    }

    /** 读取指定用户的 LLM 配置（Key 脱敏）。 */
    public LlmSettingsResponse getSettings(Long userId) {
        UserApiConfig config = requireConfig(userId);
        return toResponse(config);
    }

    /**
     * 保存指定用户的 LLM 配置；apiKey 留空则保持原值。
     */
    public LlmSettingsResponse saveSettings(Long userId, UpdateLlmSettingsRequest request) {
        UserApiConfig config = requireConfig(userId);
        config.setBaseUrl(StrUtil.trim(request.getBaseUrl()));
        config.setModel(StrUtil.trim(request.getModel()));
        if (StrUtil.isNotBlank(request.getApiKey())) {
            config.setApiKey(StrUtil.trim(request.getApiKey()));
        }
        userApiConfigMapper.updateById(config);
        return toResponse(config);
    }

    /**
     * 调用 OpenAI 兼容 {@code GET /models} 接口。
     * Key 或 URL 缺失时返回本地默认列表，{@code fromRemote=false}。
     */
    private ModelsResponse fetchModels(String baseUrl, String apiKey) {
        if (StrUtil.isBlank(baseUrl) || StrUtil.isBlank(apiKey)) {
            return new ModelsResponse(DEFAULT_MODELS, false);
        }
        List<String> remote = fetchRemoteModels(baseUrl, apiKey);
        if (remote.isEmpty()) {
            return new ModelsResponse(DEFAULT_MODELS, false);
        }
        return new ModelsResponse(remote, true);
    }

    /**
     * 判断用户是否已完成 LLM 配置（Key + URL + Model 三者非空）。
     * 聊天接口在调用前会检查此条件。
     */
    public boolean isApiConfigured(Long userId) {
        UserApiConfig config = userApiConfigMapper.selectById(userId);
        return config != null && isApiConfigured(config);
    }

    private boolean isApiConfigured(UserApiConfig config) {
        return StrUtil.isAllNotBlank(config.getApiKey(), config.getBaseUrl(), config.getModel());
    }

    private UserApiConfig requireConfig(Long userId) {
        UserApiConfig config = userApiConfigMapper.selectById(userId);
        if (config == null) {
            throw new BusinessException("API 配置不存在，请重新注册或联系管理员");
        }
        return config;
    }

    private long currentUserId() {
        return StpUtil.getLoginIdAsLong();
    }

    private LlmSettingsResponse toResponse(UserApiConfig config) {
        return new LlmSettingsResponse(
                config.getBaseUrl(),
                config.getModel(),
                maskApiKey(config.getApiKey()),
                isApiConfigured(config)
        );
    }

    /**
     * API Key 脱敏：保留前 4 后 4，中间用 **** 替代。
     */
    static String maskApiKey(String apiKey) {
        if (StrUtil.isBlank(apiKey)) {
            return "";
        }
        if (apiKey.length() <= 8) {
            return "****";
        }
        return apiKey.substring(0, 4) + "****" + apiKey.substring(apiKey.length() - 4);
    }

    /** 请求 {@code {baseUrl}/models}，解析 OpenAI 格式 JSON。 */
    private List<String> fetchRemoteModels(String baseUrl, String apiKey) {
        String url = StrUtil.removeSuffix(baseUrl, "/") + "/models";
        try {
            HttpResponse response = HttpRequest.get(url)
                    .header("Authorization", "Bearer " + apiKey)
                    .timeout(10_000)
                    .execute();
            if (!response.isOk()) {
                log.warn("拉取模型列表失败: status={}, url={}", response.getStatus(), url);
                return List.of();
            }
            return parseModels(response.body());
        } catch (Exception ex) {
            log.warn("拉取模型列表异常: url={}", url, ex);
            return List.of();
        }
    }

    private List<String> parseModels(String body) {
        if (!JSONUtil.isTypeJSON(body)) {
            return List.of();
        }
        JSONObject json = JSONUtil.parseObj(body);
        JSONArray data = json.getJSONArray("data");
        if (data == null || data.isEmpty()) {
            return List.of();
        }
        List<String> models = new ArrayList<>();
        for (int i = 0; i < data.size(); i++) {
            JSONObject item = data.getJSONObject(i);
            if (item == null) {
                continue;
            }
            String id = item.getStr("id");
            if (StrUtil.isNotBlank(id)) {
                models.add(id);
            }
        }
        return models;
    }
}
