package com.agent.service;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.agent.common.BusinessException;
import com.agent.dto.AppendMessageRequest;
import com.agent.dto.MessageResponse;
import com.agent.entity.UserApiConfig;
import com.agent.mapper.UserApiConfigMapper;
import com.agent.tool.AgentToolContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.Disposable;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Spring AI 流式对话 Web 服务（SSE）。
 * <p>
 * 核心对话逻辑见 {@link AgentChatRunner}。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private static final ExecutorService STREAM_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    private final ConversationService conversationService;
    private final LlmSettingsService llmSettingsService;
    private final UserApiConfigMapper userApiConfigMapper;
    private final AgentChatRunner agentChatRunner;

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

        SseEmitter emitter = new SseEmitter(300_000L);

        sendStatus(emitter, "正在保存你的消息…");
        AppendMessageRequest userRequest = new AppendMessageRequest();
        userRequest.setContent(prompt);
        userRequest.setRole("user");
        MessageResponse userMessage = conversationService.appendMessage(conversationId, userRequest);

        sendStatus(emitter, "正在加载对话上下文…");
        List<MessageResponse> history = conversationService.listMessages(conversationId);
        List<Message> aiMessages = agentChatRunner.buildPromptFromHistory(history);

        Disposable subscription = agentChatRunner.streamChat(
                new AgentChatRunner.StreamChatRequest(config, userId, conversationId, aiMessages, true),
                new AgentChatRunner.StreamChatCallbacks(
                        status -> sendStatus(emitter, status),
                        delta -> sendDelta(emitter, delta),
                        result -> handleStreamComplete(
                                emitter,
                                conversationId,
                                userId,
                                userMessage,
                                result.reply()
                        ),
                        error -> handleStreamError(emitter, error)
                )
        );

        emitter.onCompletion(subscription::dispose);
        emitter.onTimeout(() -> {
            subscription.dispose();
            AgentToolContext.clear();
            emitter.complete();
        });
        emitter.onError(error -> {
            subscription.dispose();
            AgentToolContext.clear();
            log.warn("SSE client disconnected: {}", error.getMessage());
        });

        return emitter;
    }

    private void sendStatus(SseEmitter emitter, String message) {
        try {
            emitter.send(SseEmitter.event().name("status").data(message));
        } catch (IOException ex) {
            throw new BusinessException("流式推送失败: " + ex.getMessage());
        }
    }

    private void sendDelta(SseEmitter emitter, String delta) {
        try {
            emitter.send(SseEmitter.event().name("delta").data(delta));
        } catch (IOException ex) {
            throw new BusinessException("流式推送失败: " + ex.getMessage());
        }
    }

    private void handleStreamError(SseEmitter emitter, Throwable error) {
        log.warn("Chat stream failed", error);
        AgentToolContext.clear();
        STREAM_EXECUTOR.execute(() -> {
            try {
                emitter.send(SseEmitter.event().name("error").data(error.getMessage()));
            } catch (IOException ignored) {
                // 客户端已断开
            }
            emitter.completeWithError(error);
        });
    }

    private void handleStreamComplete(
            SseEmitter emitter,
            Long conversationId,
            Long userId,
            MessageResponse userMessage,
            String reply) {
        STREAM_EXECUTOR.execute(() -> {
            try {
                AppendMessageRequest assistantRequest = new AppendMessageRequest();
                assistantRequest.setContent(reply);
                assistantRequest.setRole("assistant");
                MessageResponse assistantMessage =
                        conversationService.appendMessage(userId, conversationId, assistantRequest);

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

    private UserApiConfig requireConfig(Long userId) {
        UserApiConfig config = userApiConfigMapper.selectById(userId);
        if (config == null || !llmSettingsService.isApiConfigured(userId)) {
            throw new BusinessException("API 配置不完整");
        }
        return config;
    }
}
