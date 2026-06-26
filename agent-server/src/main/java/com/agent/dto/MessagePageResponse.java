package com.agent.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/** GET /api/conversations/{id}/messages?limit=&beforeId= 分页响应 */
@Data
@AllArgsConstructor
public class MessagePageResponse {

    /** 当前页消息，按时间正序 */
    private List<MessageResponse> messages;

    /** 是否还有更早的消息可加载 */
    private boolean hasMore;
}
