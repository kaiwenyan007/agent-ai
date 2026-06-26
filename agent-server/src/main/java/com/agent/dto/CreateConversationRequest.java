package com.agent.dto;

import lombok.Data;

/** POST /api/conversations 请求体（可选） */
@Data
public class CreateConversationRequest {

    /** 为空时使用默认标题「新对话」 */
    private String title;
}
