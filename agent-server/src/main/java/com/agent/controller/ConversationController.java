package com.agent.controller;

import com.agent.common.ApiResponse;
import com.agent.dto.AppendMessageRequest;
import com.agent.dto.ConversationResponse;
import com.agent.dto.CreateConversationRequest;
import com.agent.dto.MessagePageResponse;
import com.agent.dto.MessageResponse;
import org.springframework.web.bind.annotation.RequestParam;
import com.agent.service.ConversationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 会话与消息 CRUD。
 * <p>
 * v0.5 起主对话路径为 {@link ChatController} 的 SSE 流式接口；
 * 本 Controller 仍负责会话管理与手动追加消息。
 */
@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;

    /**
     * 列出当前用户的全部会话，按 {@code updated_at} 倒序。
     */
    @GetMapping
    public ApiResponse<List<ConversationResponse>> list() {
        return ApiResponse.ok(conversationService.listCurrentUserConversations());
    }

    /**
     * 创建新会话。
     *
     * @param request 可选 title；为空时使用默认标题「新对话」
     */
    @PostMapping
    public ApiResponse<ConversationResponse> create(@RequestBody(required = false) CreateConversationRequest request) {
        return ApiResponse.ok(conversationService.createConversation(request));
    }

    /**
     * 删除指定会话（逻辑删除，需归属当前用户）。
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        conversationService.deleteConversation(id);
        return ApiResponse.ok(null);
    }

    /**
     * 获取会话消息。
     * <p>
     * 传 {@code limit} 时分页返回；{@code beforeId} 用于上滑加载更早消息。
     */
    @GetMapping("/{id}/messages")
    public ApiResponse<?> listMessages(
            @PathVariable Long id,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Long beforeId) {
        if (limit != null || beforeId != null) {
            return ApiResponse.ok(conversationService.listMessagesPage(id, limit, beforeId));
        }
        return ApiResponse.ok(conversationService.listMessages(id));
    }

    /**
     * 向会话追加一条消息。
     *
     * @param request content 必填；role 默认 user，可选 assistant / system
     */
    @PostMapping("/{id}/messages")
    public ApiResponse<MessageResponse> appendMessage(
            @PathVariable Long id,
            @Valid @RequestBody AppendMessageRequest request) {
        return ApiResponse.ok(conversationService.appendMessage(id, request));
    }
}
