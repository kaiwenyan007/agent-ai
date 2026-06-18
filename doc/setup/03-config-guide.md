# 配置指南（多环境）

## Profile 一览

| Profile | 文件 | 用途 |
|---------|------|------|
| `dev`（默认） | `application-dev.yml` | 本地 MySQL |
| `h2` | `application-h2.yml` | H2 内存库，免安装 |
| `local` | `application-local.yml` | 个人覆盖（**不提交 Git**） |

`application.yml` 中默认：`spring.profiles.active: dev`

---

## 启动命令

```powershell
# 默认 MySQL（dev）
mvn -pl agent-server spring-boot:run

# H2 内存库
mvn -pl agent-server spring-boot:run "-Dspring-boot.run.profiles=h2"

# dev + 个人 local 覆盖
mvn -pl agent-server spring-boot:run "-Dspring-boot.run.profiles=dev,local"
```

---

## application-local.yml 模板

复制 `agent-server/src/main/resources/application-local.yml.example` 为 `application-local.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/agent_ai?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: 你的密码

agent:
  llm:
    api-key: sk-xxx
    base-url: https://api.deepseek.com
    model: deepseek-chat
```

`agent.llm` 在 **v0.5** 启用；v0.1–v0.4 可暂不填。

---

## MyBatis-Plus 配置

已在 `application.yml` 中配置：

- 下划线转驼峰
- 逻辑删除字段 `deleted`
- 主键自增 `id-type: auto`

开发期 SQL 日志：`log-impl: StdOutImpl`（上线前关闭）。

---

## 端口与跨域

- 后端默认：`8080`
- 前端开发（v0.4）：`5173`，需在 `WebMvcConfig` 配置 CORS（该步在 v0.4 实现）

---

## 下一步

→ 开始 [v0.1 工程骨架](../iterations/v0.1-project-skeleton.md)
