# Frontend（Vue 3）

Agent AI Web UI，v0.4 起可用。

## 技术栈

- Vue 3 + TypeScript + Vite
- **[MateChat](https://github.com/DevCloudFE/MateChat)** — 华为开源 AI 对话 UI 组件库
- vue-devui + @devui-design/icons
- Pinia + Vue Router + Axios

## 开发

```powershell
cd frontend
npm install
npm run dev
```

默认地址：http://localhost:5173  
后端 API：http://localhost:8080（Vite 已配置 `/api` 代理）

## 页面

| 路由 | 说明 |
|------|------|
| `/login` | 登录 / 注册 |
| `/chat` | 会话列表 + 消息（v0.5 接入流式 AI） |
| `/settings` | LLM API 配置 |
