package com.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 对话会话，对应表 conversations */
@Data
@TableName("conversations")
public class Conversation {

    /** 新建会话时的默认标题，首条 user 消息会替换为 prompt 摘要 */
    public static final String DEFAULT_TITLE = "\u65b0\u5bf9\u8bdd";

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    private String title;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
