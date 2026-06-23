package com.agent.service;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.agent.common.BusinessException;
import com.agent.dto.AppendMessageRequest;
import com.agent.dto.MessageResponse;
import com.agent.entity.TokenUsage;
import com.agent.entity.UserApiConfig;
import com.agent.mapper.TokenUsageMapper;
import com.agent.mapper.UserApiConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.Disposable;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private static final ExecutorService STREAM_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    private final ConversationService conversationService;
    private final LlmSettingsService llmSettingsService;
    private final UserApiConfigMapper userApiConfigMapper;
    private final TokenUsageMapper tokenUsageMapper;

    public SseEmitter streamChat(Long conversationId, String promptText) {
        Long userId = StpUtil.getLoginIdAsLong();
        if (!llmSettingsService.isApiConfigured(userId)) {
            throw new BusinessException("请先在 API CONFIG 中配置 Key / URL / Model");
        }

        String prompt = StrUtil.trim(promptText);
        if (StrUtil.isBlank(prompt)) {
            throw new BusinessException("消息内容不能为空");
        }

        UserApiConfig config = requireConfig(userId);
        AppendMessageRequest userRequest = new AppendMessageRequest();
        userRequest.setContent(prompt);
        userRequest.setRole("user");
        MessageResponse userMessage = conversationService.appendMessage(conversationId, userRequest);

        List<MessageResponse> history = conversationService.listMessages(conversationId);
        List<org.springframework.ai.chat.messages.Message> aiMessages =
                history.stream().map(this::toAiMessage).toList();
        ChatClient chatClient = buildChatClient(config);

        SseEmitter emitter = new SseEmitter(300_000L);
        AtomicReference<StringBuilder> assistantContent = new AtomicReference<>(new StringBuilder());
        AtomicReference<Usage> usageRef = new AtomicReference<>();

        Disposable subscription = chatClient.prompt(new Prompt(aiMessages))
                .stream()
                .chatResponse()
                .subscribe(
                        response -> handleChunk(emitter, assistantContent, usageRef, response),
                        error -> handleStreamError(emitter, error),
                        () -> handleStreamComplete(
                                emitter,
                                conversationId,
                                userId,
                                config,
                                userMessage,
                                assistantContent,
                                usageRef
                        )
                );

        emitter.onCompletion(subscription::dispose);
        emitter.onTimeout(() -> {
            subscription.dispose();
            emitter.complete();
        });
        emitter.onError(error -> {
            subscription.dispose();
            log.warn("SSE client disconnected: {}", error.getMessage());
        });

        return emitter;
    }

    private void handleChunk(
            SseEmitter emitter,
            AtomicReference<StringBuilder> assistantContent,
            AtomicReference<Usage> usageRef,
            ChatResponse response) {
        try {
            if (response.getResult() != null && response.getResult().getOutput() != null) {
                String chunk = response.getResult().getOutput().getText();
                if (StrUtil.isNotEmpty(chunk)) {
                    assistantContent.get().append(chunk);
                    emitter.send(SseEmitter.event().name("delta").data(chunk));
                }
            }
            Usage usage = response.getMetadata().getUsage();
            if (usage != null && usage.getTotalTokens() > 0) {
                usageRef.set(usage);
            }
        } catch (IOException ex) {
            throw new BusinessException("流式推送失败: " + ex.getMessage());
        }
    }

    private void handleStreamError(SseEmitter emitter, Throwable error) {
        log.warn("Chat stream failed", error);
        STREAM_EXECUTOR.execute(() -> {
            try {
                emitter.send(SseEmitter.event().name("error").data(error.getMessage()));
            } catch (IOException ignored) {
                // client gone
            }
            emitter.completeWithError(error);
        });
    }

    private void handleStreamComplete(
            SseEmitter emitter,
            Long conversationId,
            Long userId,
            UserApiConfig config,
            MessageResponse userMessage,
            AtomicReference<StringBuilder> assistantContent,
            AtomicReference<Usage> usageRef) {
        STREAM_EXECUTOR.execute(() -> {
            try {
                String reply = assistantContent.get().toString().trim();
                if (StrUtil.isBlank(reply)) {
                    reply = "[FAIL] 模型未返回内容";
                }

                AppendMessageRequest assistantRequest = new AppendMessageRequest();
                assistantRequest.setContent(reply);
                assistantRequest.setRole("assistant");
                MessageResponse assistantMessage = conversationService.appendMessage(conversationId, assistantRequest);

                recordTokenUsage(userId, conversationId, config.getModel(), usageRef.get());

                String payload = JSONUtil.toJsonStr(Map.of(
                        "userMessageId", userMessage.getId(),
                        "assistantMessageId", assistantMessage.getId()
                ));
                emitter.send(SseEmitter.event().name("done").data(payload));
                emitter.complete();
            } catch (Exception ex) {
                log.error("Finalize chat stream failed", ex);
                emitter.completeWithError(ex);
            }
        });
    }

    private ChatClient buildChatClient(UserApiConfig config) {
        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(StrUtil.removeSuffix(config.getBaseUrl(), "/"))
                .apiKey(config.getApiKey())
                .build();
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(config.getModel())
                .build();
        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(options)
                .build();
        return ChatClient.builder(chatModel).build();
    }

    private org.springframework.ai.chat.messages.Message toAiMessage(MessageResponse message) {
        String role = message.getRole();
        if ("assistant".equals(role)) {
            return new AssistantMessage(message.getContent());
        }
        if ("system".equals(role)) {
            return new SystemMessage(message.getContent());
        }
        return new UserMessage(message.getContent());
    }

    private UserApiConfig requireConfig(Long userId) {
        UserApiConfig config = userApiConfigMapper.selectById(userId);
        if (config == null || !llmSettingsService.isApiConfigured(userId)) {
            throw new BusinessException("API 配置不完整");
        }
        return config;
    }

    private void recordTokenUsage(Long userId, Long conversationId, String model, Usage usage) {
        if (usage == null || usage.getTotalTokens() <= 0) {
            return;
        }
        TokenUsage record = new TokenUsage();
        record.setUserId(userId);
        record.setConversationId(conversationId);
        record.setModel(model);
        record.setPromptTokens(safeInt(usage.getPromptTokens()));
        record.setCompletionTokens(safeInt(usage.getCompletionTokens()));
        record.setTotalTokens(safeInt(usage.getTotalTokens()));
        record.setEstimatedCost(estimateCost(model, usage));
        record.setCreatedAt(LocalDateTime.now());
        tokenUsageMapper.insert(record);
    }

    private static int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private static BigDecimal estimateCost(String model, Usage usage) {
        int prompt = safeInt(usage.getPromptTokens());
        int completion = safeInt(usage.getCompletionTokens());
        BigDecimal promptRate = model != null && model.contains("reasoner")
                ? new BigDecimal("0.000004")
                : new BigDecimal("0.000001");
        BigDecimal completionRate = model != null && model.contains("reasoner")
                ? new BigDecimal("0.000016")
                : new BigDecimal("0.000002");
        BigDecimal cost = promptRate.multiply(BigDecimal.valueOf(prompt))
                .add(completionRate.multiply(BigDecimal.valueOf(completion)));
        return cost.setScale(6, RoundingMode.HALF_UP);
    }
}
