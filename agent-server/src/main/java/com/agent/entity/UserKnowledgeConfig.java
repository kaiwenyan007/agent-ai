package com.agent.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 用户知识库路径配置，v0.7 RAG 使用 */
@Data
@TableName("user_knowledge_configs")
public class UserKnowledgeConfig {

    @TableId("user_id")
    private Long userId;

    @TableField("knowledge_dir")
    private String knowledgeDir;

    @TableField("doc_count")
    private Integer docCount;

    @TableField("chunk_count")
    private Integer chunkCount;

    @TableField("last_indexed_at")
    private LocalDateTime lastIndexedAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
