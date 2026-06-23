package com.agent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChatStreamRequest {

    @NotNull(message = "conversationId 不能为空")
    private Long conversationId;

    @NotBlank(message = "prompt 不能为空")
    private String prompt;
}
