package com.agent.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

/** 会话列表项 / 创建会话响应 */
@Data
@AllArgsConstructor
public class ConversationResponse {

    private Long id;

    private String title;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
