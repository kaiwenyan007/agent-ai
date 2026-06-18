-- H2 内存库自动初始化（profile=h2 时由 Spring 执行）
-- 语法按 MySQL 模式，供本地免安装快速验证

CREATE TABLE IF NOT EXISTS users (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(64)  NOT NULL,
    password_hash VARCHAR(128) NOT NULL,
    created_at    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    deleted       TINYINT      DEFAULT 0,
    CONSTRAINT uk_users_username UNIQUE (username)
);

CREATE TABLE IF NOT EXISTS user_api_configs (
    user_id    BIGINT PRIMARY KEY,
    api_key    VARCHAR(512) DEFAULT '',
    base_url   VARCHAR(256) DEFAULT 'https://api.deepseek.com',
    model      VARCHAR(128) DEFAULT 'deepseek-chat',
    updated_at TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    deleted    TINYINT      DEFAULT 0
);

CREATE TABLE IF NOT EXISTS user_knowledge_configs (
    user_id         BIGINT PRIMARY KEY,
    knowledge_dir   VARCHAR(1024) DEFAULT '',
    doc_count       INT           DEFAULT 0,
    chunk_count     INT           DEFAULT 0,
    last_indexed_at TIMESTAMP     NULL,
    updated_at      TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    deleted         TINYINT       DEFAULT 0
);

CREATE TABLE IF NOT EXISTS conversations (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT       NOT NULL,
    title      VARCHAR(128) DEFAULT '新对话',
    created_at TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    deleted    TINYINT      DEFAULT 0
);

CREATE TABLE IF NOT EXISTS messages (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id BIGINT       NOT NULL,
    role            VARCHAR(16)  NOT NULL,
    content         CLOB         NOT NULL,
    created_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    deleted         TINYINT      DEFAULT 0
);

CREATE TABLE IF NOT EXISTS token_usage (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id           BIGINT         NOT NULL,
    conversation_id   BIGINT         NULL,
    model             VARCHAR(128)   NOT NULL,
    prompt_tokens     INT            DEFAULT 0,
    completion_tokens INT            DEFAULT 0,
    total_tokens      INT            DEFAULT 0,
    estimated_cost    DECIMAL(12, 6) DEFAULT 0,
    created_at        TIMESTAMP      DEFAULT CURRENT_TIMESTAMP,
    deleted           TINYINT        DEFAULT 0
);

CREATE TABLE IF NOT EXISTS rag_queries (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT        NULL,
    conversation_id BIGINT        NULL,
    query_text      VARCHAR(2048) NOT NULL,
    hit             TINYINT       DEFAULT 0,
    result_count    INT           DEFAULT 0,
    search_mode     VARCHAR(32)   DEFAULT 'vector',
    created_at      TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    deleted         TINYINT       DEFAULT 0
);

CREATE TABLE IF NOT EXISTS chroma_cache_events (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_type VARCHAR(32) NOT NULL,
    created_at TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    deleted    TINYINT     DEFAULT 0
);
