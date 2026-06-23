package com.agent.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AppendMessageRequest {

    @NotBlank(message = "消息内容不能为空")
    private String content;

    /** 默认 user */
    private String role;
}
