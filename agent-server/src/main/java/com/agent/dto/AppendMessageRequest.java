package com.agent.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** POST /api/conversations/{id}/messages 请求体 */
@Data
public class AppendMessageRequest {

    @NotBlank(message = "消息内容不能为空")
    private String content;

    /** user / assistant / system，默认 user */
    private String role;
}
