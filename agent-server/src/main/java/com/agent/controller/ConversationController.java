package com.agent.controller;

import com.agent.common.ApiResponse;
import com.agent.dto.AppendMessageRequest;
import com.agent.dto.ConversationResponse;
import com.agent.dto.CreateConversationRequest;
import com.agent.dto.MessageResponse;
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

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;

    @GetMapping
    public ApiResponse<List<ConversationResponse>> list() {
        return ApiResponse.ok(conversationService.listCurrentUserConversations());
    }

    @PostMapping
    public ApiResponse<ConversationResponse> create(@RequestBody(required = false) CreateConversationRequest request) {
        return ApiResponse.ok(conversationService.createConversation(request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        conversationService.deleteConversation(id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/{id}/messages")
    public ApiResponse<List<MessageResponse>> listMessages(@PathVariable Long id) {
        return ApiResponse.ok(conversationService.listMessages(id));
    }

    @PostMapping("/{id}/messages")
    public ApiResponse<MessageResponse> appendMessage(
            @PathVariable Long id,
            @Valid @RequestBody AppendMessageRequest request) {
        return ApiResponse.ok(conversationService.appendMessage(id, request));
    }
}
