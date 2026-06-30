package com.agent.service;

import cn.hutool.core.util.StrUtil;
import com.agent.dto.MessageResponse;
import com.agent.entity.TokenUsage;
import com.agent.entity.UserApiConfig;
import com.agent.mapper.TokenUsageMapper;
import com.agent.tool.AgentToolContext;
import com.agent.tool.AgentTools;
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
import reactor.core.Disposable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Agent 对话核心：构建 ChatClient、流式调用、Token 记录。
 * <p>
 * Web {@link ChatService} 与 CLI 共用此逻辑。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentChatRunner {

    /** 对标 Python langchain_agent.py 系统提示词。 */
    public static final String AGENT_SYSTEM_PROMPT =
            "你是一个能使用工具的 AI 助手。"
                    + "需要查时间、做计算、读文件、列目录时请调用相应工具。"
                    + "回答日期、星期几时，必须原样使用 getCurrentTime 工具返回的星期，禁止自行推算。"
                    + "用户问天气时，必须调用 getTodayWeather 并传入中国城市名；若用户未说明城市，请先追问要查哪座城市，禁止编造天气。"
                    + "用户问个人知识库、笔记目录有哪些文件时，必须调用 listKnowledgeFiles，禁止用 listProjectFiles 代替。"
                    + "回答文档内容检索时，优先调用 queryKnowledgeBase 检索用户配置的知识库，不要编造结果。"
                    + "请用中文回答。";

    private final AgentTools agentTools;
    private final TokenUsageMapper tokenUsageMapper;

    /**
     * 流式对话请求参数。
     *
     * @param config            LLM 配置
     * @param userId            用户 ID（simple 模式可为 null）
     * @param conversationId    会话 ID（simple 模式可为 null）
     * @param aiMessages        完整 Prompt（含 system + 历史）
     * @param recordTokenUsage  是否写入 token_usage 表
     */
    public record StreamChatRequest(
            UserApiConfig config,
            Long userId,
            Long conversationId,
            List<Message> aiMessages,
            boolean recordTokenUsage
    ) {}

    public record StreamChatResult(String reply, Usage usage) {}

    /**
     * 流式回调；{@code onDelta} 仅推送增量文本。
     */
    public record StreamChatCallbacks(
            Consumer<String> onStatus,
            Consumer<String> onDelta,
            Consumer<StreamChatResult> onComplete,
            Consumer<Throwable> onError
    ) {}

    /**
     * 发起流式对话，返回 Reactor 订阅（调用方负责 dispose）。
     */
    public Disposable streamChat(StreamChatRequest request, StreamChatCallbacks callbacks) {
        AtomicReference<StringBuilder> assistantContent = new AtomicReference<>(new StringBuilder());
        AtomicReference<Usage> usageRef = new AtomicReference<>();
        AtomicReference<String> streamedText = new AtomicReference<>("");
        AtomicBoolean firstDelta = new AtomicBoolean(true);

        ChatClient chatClient = buildChatClient(request.config());
        AgentToolContext.set(request.userId(), request.conversationId());

        if (callbacks.onStatus() != null) {
            callbacks.onStatus().accept("正在连接 " + request.config().getModel() + "…");
        }

        return chatClient.prompt(new Prompt(request.aiMessages()))
                .stream()
                .chatResponse()
                .subscribe(
                        response -> handleChunk(
                                callbacks,
                                assistantContent,
                                usageRef,
                                streamedText,
                                firstDelta,
                                response
                        ),
                        error -> {
                            AgentToolContext.clear();
                            if (callbacks.onError() != null) {
                                callbacks.onError().accept(error);
                            }
                        },
                        () -> {
                            AgentToolContext.clear();
                            String reply = assistantContent.get().toString().trim();
                            if (StrUtil.isBlank(reply)) {
                                reply = "[FAIL] 模型未返回内容";
                            }
                            if (request.recordTokenUsage()) {
                                recordTokenUsage(
                                        request.userId(),
                                        request.conversationId(),
                                        request.config().getModel(),
                                        usageRef.get()
                                );
                            }
                            if (callbacks.onComplete() != null) {
                                callbacks.onComplete().accept(new StreamChatResult(reply, usageRef.get()));
                            }
                        }
                );
    }

    /**
     * 阻塞直到流式对话结束（CLI 使用）。
     */
    public StreamChatResult streamChatBlocking(StreamChatRequest request, StreamChatCallbacks callbacks)
            throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<StreamChatResult> resultRef = new AtomicReference<>();
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        Disposable subscription = streamChat(
                request,
                new StreamChatCallbacks(
                        callbacks.onStatus(),
                        callbacks.onDelta(),
                        result -> {
                            resultRef.set(result);
                            latch.countDown();
                        },
                        error -> {
                            errorRef.set(error);
                            latch.countDown();
                        }
                )
        );

        boolean finished = latch.await(5, TimeUnit.MINUTES);
        subscription.dispose();
        if (!finished) {
            throw new IllegalStateException("对话超时（5 分钟）");
        }
        if (errorRef.get() != null) {
            Throwable error = errorRef.get();
            if (error instanceof RuntimeException) {
                throw (RuntimeException) error;
            }
            throw new IllegalStateException(error.getMessage(), error);
        }
        return resultRef.get();
    }

    /** 由 DB 历史构建 Spring AI 消息列表（含 system）。 */
    public List<Message> buildPromptFromHistory(List<MessageResponse> history) {
        List<Message> aiMessages = new ArrayList<>();
        aiMessages.add(new SystemMessage(AGENT_SYSTEM_PROMPT));
        history.stream().map(this::toAiMessage).forEach(aiMessages::add);
        return aiMessages;
    }

    /** 由内存历史构建 Prompt（simple 模式）。 */
    public List<Message> buildPromptFromMemory(List<Message> memory) {
        List<Message> aiMessages = new ArrayList<>();
        aiMessages.add(new SystemMessage(AGENT_SYSTEM_PROMPT));
        aiMessages.addAll(memory);
        return aiMessages;
    }

    public ChatClient buildChatClient(UserApiConfig config) {
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
        return ChatClient.builder(chatModel)
                .defaultTools(agentTools)
                .build();
    }

    public Message toAiMessage(MessageResponse message) {
        String role = message.getRole();
        if ("assistant".equals(role)) {
            return new AssistantMessage(message.getContent());
        }
        if ("system".equals(role)) {
            return new SystemMessage(message.getContent());
        }
        return new UserMessage(message.getContent());
    }

    private void handleChunk(
            StreamChatCallbacks callbacks,
            AtomicReference<StringBuilder> assistantContent,
            AtomicReference<Usage> usageRef,
            AtomicReference<String> streamedText,
            AtomicBoolean firstDelta,
            ChatResponse response) {
        if (response.getResult() != null && response.getResult().getOutput() != null) {
            String text = response.getResult().getOutput().getText();
            if (StrUtil.isNotEmpty(text)) {
                String prev = streamedText.get();
                String delta = text.startsWith(prev) ? text.substring(prev.length()) : text;
                streamedText.set(text);

                if (StrUtil.isNotEmpty(delta)) {
                    assistantContent.get().append(delta);
                    if (firstDelta.compareAndSet(true, false) && callbacks.onStatus() != null) {
                        callbacks.onStatus().accept("正在生成回复…");
                    }
                    if (callbacks.onDelta() != null) {
                        callbacks.onDelta().accept(delta);
                    }
                }
            }
        }
        Usage usage = response.getMetadata().getUsage();
        if (usage != null && usage.getTotalTokens() > 0) {
            usageRef.set(usage);
        }
    }

    private void recordTokenUsage(Long userId, Long conversationId, String model, Usage usage) {
        if (userId == null || usage == null || usage.getTotalTokens() <= 0) {
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
