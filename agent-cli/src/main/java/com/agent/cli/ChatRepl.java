package com.agent.cli;

import cn.hutool.core.util.StrUtil;
import com.agent.common.BusinessException;
import com.agent.config.CliProperties;
import com.agent.dto.AppendMessageRequest;
import com.agent.dto.ConversationResponse;
import com.agent.dto.CreateConversationRequest;
import com.agent.dto.MessageResponse;
import com.agent.entity.UserApiConfig;
import com.agent.mapper.UserApiConfigMapper;
import com.agent.service.AgentChatRunner;
import com.agent.service.ConversationService;
import com.agent.service.LlmSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 交互式聊天 REPL（simple / full 双模式）。
 */
@Component
@RequiredArgsConstructor
public class ChatRepl {

    private final CliProperties cliProperties;
    private final CliAuthHelper authHelper;
    private final CliSessionManager sessionManager;
    private final ConfigWizard configWizard;
    private final ConversationService conversationService;
    private final LlmSettingsService llmSettingsService;
    private final UserApiConfigMapper userApiConfigMapper;
    private final AgentChatRunner agentChatRunner;
    private final ConsoleIO io;

    public int run(boolean simpleMode) {
        try {
            if (simpleMode) {
                runSimpleMode();
            } else {
                runFullMode();
            }
            return 0;
        } catch (BusinessException ex) {
            io.println("[错误] " + ex.getMessage());
            return 1;
        } catch (Exception ex) {
            io.println("[错误] " + ex.getMessage());
            ex.printStackTrace(System.err);
            return 1;
        }
    }

    private void runSimpleMode() {
        if (!cliProperties.isSimpleConfigured()) {
            io.println("simple 模式需要配置 LLM API，任选其一：");
            io.println("  1. 环境变量 OPENAI_API_KEY / OPENAI_BASE_URL / OPENAI_MODEL");
            io.println("  2. application-local.yml 中 agent.cli.llm.*");
            throw new IllegalStateException("simple 模式 API 未配置");
        }

        UserApiConfig config = cliProperties.toApiConfig();
        List<Message> memory = new ArrayList<>();

        io.println("");
        io.println("Agent AI 聊天 [simple 模式]");
        io.println("配置来源: agent.cli.llm / 环境变量（无需登录）");
        io.println("quit 退出 | /clear 清空历史");
        io.println("");

        replLoop(null, null, config, memory, false);
    }

    private void runFullMode() {
        var login = authHelper.ensureLoggedIn();
        long userId = login.getUserId();

        if (!configWizard.ensureConfigured(userId)) {
            throw new IllegalStateException("API 配置未完成，无法开始聊天");
        }

        ConversationResponse conversation = conversationService.createConversation(new CreateConversationRequest());
        UserApiConfig config = userApiConfigMapper.selectById(userId);

        io.println("");
        io.println("Agent AI 聊天 [full 模式]");
        io.println("用户: " + login.getUsername() + " | 会话 #" + conversation.getId());
        io.println("quit 退出 | /clear 清空历史 | /config 重新配置 API | /logout 退出登录");
        io.println("");

        replLoop(userId, conversation.getId(), config, null, true);
    }

    private void replLoop(
            Long userId,
            Long conversationId,
            UserApiConfig config,
            List<Message> memory,
            boolean fullMode) {
        while (true) {
            String input = io.readLine("你> ");
            if (input.isEmpty()) {
                continue;
            }
            String lower = input.trim().toLowerCase();
            if ("quit".equals(lower) || "exit".equals(lower) || "q".equals(lower)) {
                break;
            }
            if ("/clear".equals(input.trim())) {
                if (fullMode) {
                    ConversationResponse fresh = conversationService.createConversation(new CreateConversationRequest());
                    conversationId = fresh.getId();
                    io.println("已新建会话 #" + conversationId + "。");
                } else {
                    memory.clear();
                    io.println("对话历史已清空。");
                }
                io.println("");
                continue;
            }
            if (fullMode && "/config".equals(input.trim())) {
                configWizard.runWizard(sessionManager.requireUserId());
                config = userApiConfigMapper.selectById(userId);
                io.println("");
                continue;
            }
            if (fullMode && "/logout".equals(input.trim())) {
                authHelper.logout();
                io.println("请重新运行以登录。");
                break;
            }

            try {
                sendTurn(userId, conversationId, config, memory, fullMode, input);
                io.println("");
            } catch (Exception ex) {
                io.println("");
                io.println("[错误] " + ex.getMessage());
                io.println("");
            }
        }
    }

    private void sendTurn(
            Long userId,
            Long conversationId,
            UserApiConfig config,
            List<Message> memory,
            boolean fullMode,
            String prompt) throws InterruptedException {
        List<Message> aiMessages;
        if (fullMode) {
            if (!llmSettingsService.isApiConfigured(userId)) {
                configWizard.ensureConfigured(userId);
                config = userApiConfigMapper.selectById(userId);
            }
            AppendMessageRequest userRequest = new AppendMessageRequest();
            userRequest.setContent(prompt);
            userRequest.setRole("user");
            conversationService.appendMessage(userId, conversationId, userRequest);

            List<MessageResponse> history = conversationService.listMessages(conversationId);
            aiMessages = agentChatRunner.buildPromptFromHistory(history);
        } else {
            memory.add(new UserMessage(prompt));
            aiMessages = agentChatRunner.buildPromptFromMemory(memory);
        }

        io.print("助手> ");
        AgentChatRunner.StreamChatResult result = agentChatRunner.streamChatBlocking(
                new AgentChatRunner.StreamChatRequest(config, userId, conversationId, aiMessages, fullMode),
                new AgentChatRunner.StreamChatCallbacks(
                        status -> { },
                        io::print,
                        completed -> { },
                        error -> { }
                )
        );
        io.println();

        if (fullMode) {
            AppendMessageRequest assistantRequest = new AppendMessageRequest();
            assistantRequest.setContent(result.reply());
            assistantRequest.setRole("assistant");
            conversationService.appendMessage(userId, conversationId, assistantRequest);
        } else {
            memory.add(new AssistantMessage(result.reply()));
        }
    }
}
