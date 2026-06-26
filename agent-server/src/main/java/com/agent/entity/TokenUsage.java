package com.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 单次对话 Token 用量记录，流式对话结束后写入 */
@Data
@TableName("token_usage")
public class TokenUsage {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("conversation_id")
    private Long conversationId;

    private String model;

    @TableField("prompt_tokens")
    private Integer promptTokens;

    @TableField("completion_tokens")
    private Integer completionTokens;

    @TableField("total_tokens")
    private Integer totalTokens;

    @TableField("estimated_cost")
    private BigDecimal estimatedCost;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableLogic
    private Integer deleted;
}
