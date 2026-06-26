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
import org.springframework.ai.chat.messages.Message;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Spring AI 流式对话核心服务。
 * <p>
 * 流程：校验配置 → 持久化 user 消息 → 加载历史 → 动态构建 ChatClient → SSE 推送 → 持久化 assistant → 记录 Token。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    /** SSE 超时 5 分钟；流结束后的落库在虚拟线程中执行。 */
    private static final ExecutorService STREAM_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    private final ConversationService conversationService;
    private final LlmSettingsService llmSettingsService;
    private final UserApiConfigMapper userApiConfigMapper;
    private final TokenUsageMapper tokenUsageMapper;

    /**
     * 发起一轮流式对话，返回 SSE 连接。
     *
     * @param conversationId 目标会话（须归属当前用户）
     * @param promptText     用户输入
     * @return SseEmitter，事件名 delta / done / error
     */
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
        AtomicReference<StringBuilder> assistantContent = new AtomicReference<>(new StringBuilder());
        AtomicReference<Usage> usageRef = new AtomicReference<>();
        AtomicReference<String> streamedText = new AtomicReference<>("");
        AtomicBoolean firstDelta = new AtomicBoolean(true);

        // 1. 先落库 user 消息，保证刷新后可见
        sendStatus(emitter, "正在保存你的消息…");
        AppendMessageRequest userRequest = new AppendMessageRequest();
        userRequest.setContent(prompt);
        userRequest.setRole("user");
        MessageResponse userMessage = conversationService.appendMessage(conversationId, userRequest);

        // 2. 加载完整历史（含刚写入的 user 消息）供多轮上下文
        sendStatus(emitter, "正在加载对话上下文…");
        List<MessageResponse> history = conversationService.listMessages(conversationId);
        List<Message> aiMessages =
                history.stream().map(this::toAiMessage).toList();
        ChatClient chatClient = buildChatClient(config);

        sendStatus(emitter, "正在连接 " + config.getModel() + "…");

        // 3. 订阅 Spring AI 流式响应，逐 delta 推送 SSE（避免 cumulative 文本重复）
        Disposable subscription = chatClient.prompt(new Prompt(aiMessages))
                .stream()
                .chatResponse()
                .subscribe(
                        response -> handleChunk(emitter, assistantContent, usageRef, streamedText, firstDelta, response),
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

        // 客户端断开或超时时释放 Reactor 订阅
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

    /** 推送阶段状态（ChatGPT 式「正在做什么」提示）。 */
    private void sendStatus(SseEmitter emitter, String message) {
        try {
            emitter.send(SseEmitter.event().name("status").data(message));
        } catch (IOException ex) {
            throw new BusinessException("流式推送失败: " + ex.getMessage());
        }
    }

    /**
     * 推送增量文本片段。
     * Spring AI 流式有时返回 cumulative 文本，此处只取相对上次的 delta。
     */
    private void handleChunk(
            SseEmitter emitter,
            AtomicReference<StringBuilder> assistantContent,
            AtomicReference<Usage> usageRef,
            AtomicReference<String> streamedText,
            AtomicBoolean firstDelta,
            ChatResponse response) {
        try {
            if (response.getResult() != null && response.getResult().getOutput() != null) {
                String text = response.getResult().getOutput().getText();
                if (StrUtil.isNotEmpty(text)) {
                    String prev = streamedText.get();
                    String delta;
                    if (text.startsWith(prev)) {
                        delta = text.substring(prev.length());
                    } else {
                        delta = text;
                    }
                    streamedText.set(text);

                    if (StrUtil.isNotEmpty(delta)) {
                        assistantContent.get().append(delta);
                        if (firstDelta.compareAndSet(true, false)) {
                            sendStatus(emitter, "正在生成回复…");
                        }
                        emitter.send(SseEmitter.event().name("delta").data(delta));
                    }
                }
            }
            // 部分模型在最后一个 chunk 才返回 usage
            Usage usage = response.getMetadata().getUsage();
            if (usage != null && usage.getTotalTokens() > 0) {
                usageRef.set(usage);
            }
        } catch (IOException ex) {
            throw new BusinessException("流式推送失败: " + ex.getMessage());
        }
    }

    /** 模型调用失败时推送 error 事件并结束 SSE。 */
    private void handleStreamError(SseEmitter emitter, Throwable error) {
        log.warn("Chat stream failed", error);
        STREAM_EXECUTOR.execute(() -> {
            try {
                emitter.send(SseEmitter.event().name("error").data(error.getMessage()));
            } catch (IOException ignored) {
                // 客户端已断开
            }
            emitter.completeWithError(error);
        });
    }

    /**
     * 流正常结束：落库 assistant 消息、记录 Token、发送 done 事件。
     * 在虚拟线程执行，避免阻塞 Reactor 线程。
     */
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
                // 虚拟线程无 HttpServletRequest，须传入请求线程已捕获的 userId
                MessageResponse assistantMessage =
                        conversationService.appendMessage(userId, conversationId, assistantRequest);

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

    /**
     * 按用户 DB 配置动态构建 OpenAI 兼容 ChatClient（DeepSeek / 自定义 Base URL）。
     */
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

    /** 将 DB 消息角色映射为 Spring AI Message 类型。 */
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

    /** 写入 token_usage；模型未返回 usage 时跳过。 */
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

    /** 按 DeepSeek 公开单价的粗略估算（reasoner 与 chat 费率不同）。 */
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
