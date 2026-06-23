package com.agent.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class MessageResponse {

    private Long id;
    private Long conversationId;
    private String role;
    private String content;
    private LocalDateTime createdAt;
}
