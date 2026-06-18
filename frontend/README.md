# Frontend（Vue 3）

本目录在 **v0.4** 迭代中初始化。

## 计划技术栈

- Vue 3 + TypeScript
- Vite
- Pinia（状态）
- Vue Router
- Axios + EventSource（SSE）

## 初始化命令（v0.4 执行）

```powershell
cd C:\Users\12782\dev\agent-ai\frontend
npm create vite@latest . -- --template vue-ts
npm install axios pinia vue-router
npm run dev
```

默认开发地址：http://localhost:5173  
后端 API：http://localhost:8080

## 页面规划

| 路由 | 版本 |
|------|------|
| `/login` | v0.4 |
| `/chat` | v0.4 / v0.5 流式 |
| `/settings` | v0.4 |
| `/knowledge` | v0.7 |
| `/stats` | v0.8 |
| `/boot` | v0.8（或登录后内嵌） |

详见 [doc/iterations/v0.4-conversation.md](../doc/iterations/v0.4-conversation.md)。
