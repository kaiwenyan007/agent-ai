# Agent AI 学习计划（Java 版）

> 目标：用 **Spring Boot + Spring AI + Vue + MyBatis-Plus** 实现与 Python `agent-demo` 等价的 Agent 系统。  
> 参考原型：`C:\Users\12782\dev\agent-demo`

---

## 最终交付物

| 能力 | 说明 |
|------|------|
| 用户体系 | 注册 / 登录 / 每人数据隔离 |
| API 配置 | 每人独立 Key / URL / Model |
| 多轮对话 | 多会话、消息持久化、SSE 流式 |
| Tool Calling | 时间、计算、天气、知识库列表、RAG 检索 |
| 个人知识库 | 用户自配本机 md 目录、REBUILD 进度条 |
| 统计 | Token、RAG 命中率 |
| 前端 | Vue 3 暗色主题 Web UI |

---

## 版本迭代路线

```
v0.1 工程骨架 + MySQL + MyBatis-Plus
  ↓
v0.2 用户注册登录（Hutool BCrypt）
  ↓
v0.3 每人 API 配置
  ↓
v0.4 多会话 + 消息表 + Vue 聊天骨架
  ↓
v0.5 Spring AI 基础对话 + SSE 流式
  ↓
v0.6 Tool Calling 工具集
  ↓
v0.7 RAG 知识库（用户自配 md）
  ↓
v0.8 统计 + 启动预热 + 体验打磨
```

---

## 每日 / 每步安排

| 步骤 | 版本 | 主题 | 预计时间 | 文档 |
|------|------|------|----------|------|
| 1 | v0.1 | 工程骨架 + 数据库 | 2–3h | [v0.1](./iterations/v0.1-project-skeleton.md) |
| 2 | v0.2 | 注册登录 | 3–4h | [v0.2](./iterations/v0.2-auth.md) |
| 3 | v0.3 | API 配置 | 2–3h | [v0.3](./iterations/v0.3-api-config.md) |
| 4 | v0.4 | 会话与消息 | 3–4h | [v0.4](./iterations/v0.4-conversation.md) |
| 5 | v0.5 | Spring AI 流式对话 | 4–5h | [v0.5](./iterations/v0.5-spring-ai-chat.md) |
| 6 | v0.6 | 工具调用 | 4–5h | [v0.6](./iterations/v0.6-tools.md) |
| 7 | v0.7 | RAG 知识库 | 5–6h | [v0.7](./iterations/v0.7-rag.md) |
| 8 | v0.8 | 统计与打磨 | 3–4h | [v0.8](./iterations/v0.8-polish.md) |

---

## 技术选型

| 项目 | 选择 | 理由 |
|------|------|------|
| 语言 | Java 21 | Spring Boot 3.4+ 推荐 |
| Web | Spring Boot 3.4 | 稳定；Spring AI 2.0 可在 v0.5 评估升级 Boot 4.x |
| ORM | **MyBatis-Plus** | 按要求；SQL 可控 |
| 工具库 | **Hutool** | 文件/HTTP/加密/JSON |
| AI | Spring AI | ChatClient + ToolCalling + VectorStore |
| 数据库 | **MySQL 8** / H2 | 本地安装或免安装 |
| 前端 | Vue 3 + Vite | 前后端分离 |
| 向量库 | Chroma（本地目录） | 与 Python 版一致，易对比 |

---

## 中间件与配置策略

| 中间件 | 是否必须 | 本地安装 | 免安装替代 |
|--------|----------|----------|------------|
| MySQL | 推荐 | [setup/02-mysql-install.md](./setup/02-mysql-install.md) | `profile=h2` |
| JDK 21 | 必须 | [setup/01-environment.md](./setup/01-environment.md) | — |
| Maven | 必须 | 同上 | — |
| Node.js | v0.4 起 | 同上 | — |
| Chroma | v0.7 | 嵌入式目录 `.chroma/` | 无需单独安装服务 |
| Redis | 可选 | 暂不引入 | — |

**原则**：每个中间件都支持 `application-*.yml` 覆盖；敏感项放 `application-local.yml`（不提交 Git）。

---

## 目标项目结构（v0.8 完成时）

```
agent-ai/
├── agent-server/
│   └── src/main/java/com/agent/
│       ├── config/          # MyBatis-Plus、Security、AI
│       ├── controller/        # REST + SSE
│       ├── service/
│       ├── mapper/            # MyBatis-Plus Mapper
│       ├── entity/
│       ├── dto/
│       ├── tool/              # @Tool 工具
│       └── rag/               # 知识库与向量
├── frontend/                  # Vue 3
├── sql/schema.sql
└── doc/iterations/
```

---

## 开始学习

1. 阅读 [setup/01-environment.md](./setup/01-environment.md) 准备环境  
2. 完成 [v0.1-project-skeleton.md](./iterations/v0.1-project-skeleton.md)  
3. 每完成一版，在文档末尾勾选验收项，再进入下一版  

**在 Cursor 中请切换到工作区：** `C:\Users\12782\dev\agent-ai`
