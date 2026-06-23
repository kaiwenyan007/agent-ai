package com.agent.dto;

import lombok.Data;

@Data
public class CreateConversationRequest {

    /** 留空则使用默认标题「新对话」 */
    private String title;
}
