package com.agent.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

/** 单条聊天消息 */
@Data
@AllArgsConstructor
public class MessageResponse {

    private Long id;

    private Long conversationId;

    /** user / assistant / system */
    private String role;

    private String content;

    private LocalDateTime createdAt;
}
