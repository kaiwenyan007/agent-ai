# Agent AI（Java 版）

对标 Python `agent-demo` 的 **Java + Spring Boot + Spring AI + Vue** 实现。

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | Java 21 · Spring Boot 3.4 · **Spring AI**（v0.5 引入）· **MyBatis-Plus** · Hutool |
| 前端 | Vue 3 + Vite（v0.4 起逐步搭建） |
| 数据库 | **MySQL 8**（推荐）/ H2（免安装体验） |
| 向量库 | Chroma 或 pgvector（v0.7） |

## 快速开始（v0.1）

### 选项 A：H2 免安装（30 秒体验）

```powershell
cd C:\Users\12782\dev\agent-ai
mvn -pl agent-server spring-boot:run "-Dspring-boot.run.profiles=h2"
```

访问 http://localhost:8080/api/health

### 选项 B：本地 MySQL（推荐后续开发）

1. 安装 MySQL → [doc/setup/02-mysql-install.md](doc/setup/02-mysql-install.md)
2. 执行 `sql/schema.sql`
3. 配置 `application-dev.yml` 或 `application-local.yml`
4. `mvn -pl agent-server spring-boot:run`

## 学习计划（必读）

**按顺序阅读并实现：**

1. [doc/learning-roadmap.md](doc/learning-roadmap.md) — 总路线图
2. [doc/iterations/](doc/iterations/) — 逐步教程（v0.1 → v0.8）
3. [doc/setup/](doc/setup/) — 环境与中间件安装

| 版本 | 主题 | 文档 |
|------|------|------|
| v0.1 | 工程骨架 + 数据库 + MyBatis-Plus | [iterations/v0.1](doc/iterations/v0.1-project-skeleton.md) |
| v0.2 | 用户注册登录 | [iterations/v0.2](doc/iterations/v0.2-auth.md) |
| v0.3 | API 配置 | [iterations/v0.3](doc/iterations/v0.3-api-config.md) |
| v0.4 | 多会话 + 消息持久化 | [iterations/v0.4](doc/iterations/v0.4-conversation.md) |
| v0.5 | Spring AI 对话 + SSE 流式 | [iterations/v0.5](doc/iterations/v0.5-spring-ai-chat.md) |
| v0.6 | Tool Calling 工具集 | [iterations/v0.6](doc/iterations/v0.6-tools.md) |
| v0.7 | RAG 知识库 | [iterations/v0.7](doc/iterations/v0.7-rag.md) |
| v0.8 | 统计 + 预热 + 前端完善 | [iterations/v0.8](doc/iterations/v0.8-polish.md) |

## 项目结构

```
agent-ai/
├── agent-server/          # Spring Boot 后端
├── frontend/              # Vue 前端（v0.4 起）
├── sql/                   # MySQL 建表脚本
├── doc/                   # 学习计划与运维文档
├── pom.xml
└── README.md
```

## 配置说明

| 文件 | 作用 |
|------|------|
| `application.yml` | 公共配置 |
| `application-dev.yml` | 本地 MySQL 默认连接 |
| `application-h2.yml` | H2 内存库（免安装） |
| `application-local.yml.example` | 个人敏感配置模板（复制为 `application-local.yml`） |

## 对标 Python Demo 的能力清单

详见此前功能清单（用户体系、Tool Calling、RAG、流式、统计等），按 v0.1–v0.8 逐步实现。
