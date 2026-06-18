-- ============================================================
-- Agent AI 数据库初始化脚本（MySQL 8.0+）
-- 使用方式见：sql/README.md
-- ============================================================

CREATE DATABASE IF NOT EXISTS agent_ai
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE agent_ai;

-- ------------------------------------------------------------
-- 用户
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    username      VARCHAR(64)     NOT NULL COMMENT '登录名',
    password_hash VARCHAR(128)    NOT NULL COMMENT 'BCrypt 哈希',
    created_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted       TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_username (username)
) ENGINE=InnoDB COMMENT='用户表';

-- ------------------------------------------------------------
-- 每人独立 LLM API 配置
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS user_api_configs (
    user_id    BIGINT UNSIGNED NOT NULL COMMENT '用户 ID',
    api_key    VARCHAR(512)    NOT NULL DEFAULT '' COMMENT 'API Key',
    base_url   VARCHAR(256)    NOT NULL DEFAULT 'https://api.deepseek.com',
    model      VARCHAR(128)    NOT NULL DEFAULT 'deepseek-chat',
    updated_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted    TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (user_id),
    CONSTRAINT fk_api_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB COMMENT='用户 API 配置';

-- ------------------------------------------------------------
-- 用户知识库配置（仅用户自配本机目录）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS user_knowledge_configs (
    user_id         BIGINT UNSIGNED NOT NULL COMMENT '用户 ID',
    knowledge_dir   VARCHAR(1024)   NOT NULL DEFAULT '' COMMENT '本机 md 目录绝对路径',
    doc_count       INT             NOT NULL DEFAULT 0,
    chunk_count     INT             NOT NULL DEFAULT 0,
    last_indexed_at DATETIME        NULL COMMENT '上次索引时间',
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (user_id),
    CONSTRAINT fk_knowledge_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB COMMENT='用户知识库配置';

-- ------------------------------------------------------------
-- 会话
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS conversations (
    id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id    BIGINT UNSIGNED NOT NULL,
    title      VARCHAR(128)    NOT NULL DEFAULT '新对话',
    created_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted    TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_conversations_user (user_id),
    CONSTRAINT fk_conv_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB COMMENT='对话会话';

-- ------------------------------------------------------------
-- 消息
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS messages (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    conversation_id BIGINT UNSIGNED NOT NULL,
    role            VARCHAR(16)     NOT NULL COMMENT 'user / assistant / system',
    content         MEDIUMTEXT      NOT NULL,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_messages_conversation (conversation_id),
    CONSTRAINT fk_msg_conv FOREIGN KEY (conversation_id) REFERENCES conversations (id)
) ENGINE=InnoDB COMMENT='对话消息';

-- ------------------------------------------------------------
-- Token 用量统计
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS token_usage (
    id                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id           BIGINT UNSIGNED NOT NULL,
    conversation_id   BIGINT UNSIGNED NULL,
    model             VARCHAR(128)    NOT NULL,
    prompt_tokens     INT             NOT NULL DEFAULT 0,
    completion_tokens INT             NOT NULL DEFAULT 0,
    total_tokens      INT             NOT NULL DEFAULT 0,
    estimated_cost    DECIMAL(12, 6)  NOT NULL DEFAULT 0,
    created_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted           TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_token_usage_user (user_id),
    CONSTRAINT fk_token_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_token_conv FOREIGN KEY (conversation_id) REFERENCES conversations (id)
) ENGINE=InnoDB COMMENT='Token 统计';

-- ------------------------------------------------------------
-- RAG 查询日志
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS rag_queries (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id         BIGINT UNSIGNED NULL,
    conversation_id BIGINT UNSIGNED NULL,
    query_text      VARCHAR(2048)   NOT NULL,
    hit             TINYINT         NOT NULL DEFAULT 0,
    result_count    INT             NOT NULL DEFAULT 0,
    search_mode     VARCHAR(32)     NOT NULL DEFAULT 'vector' COMMENT 'vector / keyword',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_rag_queries_user (user_id)
) ENGINE=InnoDB COMMENT='RAG 查询日志';

-- ------------------------------------------------------------
-- Chroma 缓存事件
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS chroma_cache_events (
    id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    event_type VARCHAR(32)     NOT NULL COMMENT 'memory_hit / disk_hit / rebuild',
    created_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted    TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
) ENGINE=InnoDB COMMENT='向量库缓存事件';
