# Agent AI 接口说明

> 统一响应格式：`{ "code": 0, "message": "ok", "data": ... }`  
> 失败时 `code != 0`，`message` 为错误描述。  
> 除公开接口外，请求头需携带：`Authorization: Bearer <token>`

---

## 健康检查

| 方法 | 路径 | 鉴权 | 说明 |
|------|------|------|------|
| GET | `/api/health` | 否 | 服务存活探测，返回 status / app / profile |

---

## 鉴权 `/api/auth`

| 方法 | 路径 | 鉴权 | 说明 |
|------|------|------|------|
| POST | `/api/auth/register` | 否 | 注册；同时初始化用户 API 配置与知识库配置行 |
| POST | `/api/auth/login` | 否 | 登录，返回 token / username / userId |
| POST | `/api/auth/logout` | 是 | 注销当前 token |
| GET | `/api/auth/me` | 是 | 获取当前登录用户 profile |

### POST `/api/auth/register`

**请求体**

```json
{ "username": "hacker007", "password": "123456" }
```

**响应** `data` 为 `null`。

### POST `/api/auth/login`

**请求体** 同注册。

**响应 data**

```json
{ "token": "...", "username": "hacker007", "userId": 1 }
```

---

## LLM 配置 `/api/settings`

| 方法 | 路径 | 鉴权 | 说明 |
|------|------|------|------|
| GET | `/api/settings/llm` | 是 | 读取当前用户 LLM 配置（Key 脱敏） |
| PUT | `/api/settings/llm` | 是 | 保存 Base URL / Model；apiKey 留空则保持原值 |
| GET | `/api/settings/models` | 是 | 用已保存配置拉取模型列表 |
| POST | `/api/settings/models` | 是 | 用表单中的 baseUrl / apiKey 拉取模型（Key 可空，回退已保存） |

### GET `/api/settings/llm` 响应 data

```json
{
  "baseUrl": "https://api.deepseek.com",
  "model": "deepseek-chat",
  "apiKeyMasked": "sk-a****xyz",
  "configured": true
}
```

### PUT `/api/settings/llm` 请求体

```json
{
  "baseUrl": "https://api.deepseek.com",
  "model": "deepseek-chat",
  "apiKey": "sk-..."
}
```

### POST `/api/settings/models` 请求体

```json
{ "baseUrl": "https://api.deepseek.com", "apiKey": "sk-..." }
```

**响应 data**

```json
{ "models": ["deepseek-chat", "deepseek-reasoner"], "fromRemote": true }
```

---

## 会话 `/api/conversations`

| 方法 | 路径 | 鉴权 | 说明 |
|------|------|------|------|
| GET | `/api/conversations` | 是 | 当前用户会话列表，按更新时间倒序 |
| POST | `/api/conversations` | 是 | 新建会话；title 可选 |
| DELETE | `/api/conversations/{id}` | 是 | 删除会话（逻辑删除） |
| GET | `/api/conversations/{id}/messages` | 是 | 会话消息列表，按时间正序 |
| POST | `/api/conversations/{id}/messages` | 是 | 追加消息；role 默认 user |

> v0.5 起对话主路径为 `/api/chat/stream`；本接口仍可用于手动追加或调试。

---

## 流式对话 `/api/chat`

| 方法 | 路径 | 鉴权 | Content-Type | 说明 |
|------|------|------|--------------|------|
| GET | `/api/chat/stream?conversationId=&prompt=` | 是 | `text/event-stream` | SSE 流式对话（Query 传参） |
| POST | `/api/chat/stream` | 是 | `text/event-stream` | SSE 流式对话（推荐，Body 传参） |

### POST 请求体

```json
{ "conversationId": 1, "prompt": "你好" }
```

### SSE 事件

| event | data | 说明 |
|-------|------|------|
| `delta` | 文本片段 | 模型输出增量 |
| `done` | `{"userMessageId":1,"assistantMessageId":2}` | 流结束，消息已落库 |
| `error` | 错误信息 | 调用失败 |

**流程**：校验 API 配置 → 写入 user 消息 → 加载历史 → Spring AI 流式调用 → 推送 delta → 写入 assistant 消息 → 记录 token_usage → 发送 done。

---

## 统计 `/api/stats`

| 方法 | 路径 | 鉴权 | 说明 |
|------|------|------|------|
| GET | `/api/stats/tokens` | 是 | 当前用户 Token 用量汇总与最近 20 条记录 |

### 响应 data

```json
{
  "totalTokens": 1200,
  "promptTokens": 800,
  "completionTokens": 400,
  "estimatedCost": 0.001200,
  "recent": [
    {
      "id": 1,
      "model": "deepseek-chat",
      "totalTokens": 1200,
      "estimatedCost": 0.001200,
      "createdAt": "2026-06-22T10:00:00"
    }
  ]
}
```

---

## 错误码约定

| HTTP | code | 场景 |
|------|------|------|
| 200 | 0 | 成功 |
| 200 | 1 | 业务错误（见 message） |
| 401 | 1 | 未登录或 token 过期 |
| 400 | 1 | 参数校验失败 |
