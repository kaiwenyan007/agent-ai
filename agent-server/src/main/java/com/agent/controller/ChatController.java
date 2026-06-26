package com.agent.controller;

import com.agent.dto.ChatStreamRequest;
import com.agent.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Spring AI 流式对话接口（SSE）。
 * <p>
 * SSE 事件约定：
 * <ul>
 *   <li>{@code delta} — 模型输出文本片段</li>
 *   <li>{@code done} — 流结束，assistant 消息已落库</li>
 *   <li>{@code error} — 调用失败</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /**
     * SSE 流式对话（Query 传参，适合简单调试）。
     *
     * @param conversationId 会话 ID
     * @param prompt         用户输入
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamGet(
            @RequestParam Long conversationId,
            @RequestParam String prompt) {
        return chatService.streamChat(conversationId, prompt);
    }

    /**
     * SSE 流式对话（Body 传参，前端推荐用法）。
     *
     * @param request conversationId + prompt
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamPost(@Valid @RequestBody ChatStreamRequest request) {
        return chatService.streamChat(request.getConversationId(), request.getPrompt());
    }
}
