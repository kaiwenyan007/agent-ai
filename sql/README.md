# SQL 脚本说明

## 文件列表

| 文件 | 用途 |
|------|------|
| `schema.sql` | **MySQL 全量建库建表**（开发/生产推荐） |
| `data-init.sql` | 可选演示数据（默认不执行） |
| `../agent-server/src/main/resources/schema-h2.sql` | H2 内存库自动建表（`profile=h2`） |

## 方式一：本地 MySQL（推荐）

### 1. 安装 MySQL

见 [doc/setup/02-mysql-install.md](../doc/setup/02-mysql-install.md)。

### 2. 执行建表脚本

```powershell
# 命令行登录 MySQL 后
mysql -u root -p < sql/schema.sql
```

或在 Navicat / DBeaver 中打开 `schema.sql` 执行。

### 3. 配置连接

编辑 `agent-server/src/main/resources/application-dev.yml`，或复制 `application-local.yml.example` 为 `application-local.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/agent_ai?...
    username: root
    password: 你的密码
```

### 4. 启动验证

```powershell
cd C:\Users\12782\dev\agent-ai
mvn -pl agent-server spring-boot:run
```

访问：http://localhost:8080/api/health

---

## 方式二：H2 内存库（免安装 MySQL）

适合快速验证 **v0.1** 工程能否跑通，**重启后数据丢失**。

```powershell
mvn -pl agent-server spring-boot:run "-Dspring-boot.run.profiles=h2"
```

- 健康检查：http://localhost:8080/api/health  
- H2 控制台：http://localhost:8080/h2-console（JDBC URL 见 `application-h2.yml`）

---

## 表结构一览

| 表名 | 说明 | 对应版本 |
|------|------|----------|
| `users` | 用户 | v0.2 |
| `user_api_configs` | 每人 API 配置 | v0.3 |
| `user_knowledge_configs` | 知识库路径 | v0.7 |
| `conversations` | 会话 | v0.4 |
| `messages` | 消息 | v0.4 |
| `token_usage` | Token 统计 | v0.8 |
| `rag_queries` | RAG 日志 | v0.8 |
| `chroma_cache_events` | 向量库缓存事件 | v0.8 |

所有表含 `deleted` 字段，配合 MyBatis-Plus 逻辑删除。
