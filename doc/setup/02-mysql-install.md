# MySQL 本地安装与建库

## 方式一：官方安装包（推荐）

### 1. 下载安装

- [MySQL Community Server 8.0](https://dev.mysql.com/downloads/mysql/)
- Windows 选 MSI Installer，安装类型 **Developer Default** 或 **Server only**
- 设置 root 密码（示例文档默认 `root`，请按实际修改配置）

### 2. 验证服务

```powershell
# 服务应已启动
Get-Service MySQL*
```

### 3. 命令行登录

```powershell
mysql -u root -p
```

---

## 方式二：Docker（可选）

```powershell
docker run -d --name agent-mysql ^
  -e MYSQL_ROOT_PASSWORD=root ^
  -e MYSQL_DATABASE=agent_ai ^
  -p 3306:3306 ^
  mysql:8.0
```

连接串：

```
jdbc:mysql://127.0.0.1:3306/agent_ai
```

---

## 执行建表脚本

在项目根目录：

```powershell
cd C:\Users\12782\dev\agent-ai
mysql -u root -p < sql/schema.sql
```

或在图形工具中执行 `sql/schema.sql` 全文。

### 验证

```sql
USE agent_ai;
SHOW TABLES;
```

应看到 8 张表：`users`、`user_api_configs`、`user_knowledge_configs`、`conversations`、`messages`、`token_usage`、`rag_queries`、`chroma_cache_events`。

---

## 配置 Spring Boot 连接

编辑 `agent-server/src/main/resources/application-dev.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/agent_ai?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
    username: root
    password: 你的密码
```

或复制 `application-local.yml.example` → `application-local.yml` 覆盖敏感项。

启动：

```powershell
mvn -pl agent-server spring-boot:run
```

---

## 远程 / 云数据库

只需修改 `spring.datasource.url` 指向远程主机，**无需改代码**。  
脚本仍用 `sql/schema.sql`，在目标库执行一次即可。

---

## 下一步

→ [03-config-guide.md](./03-config-guide.md)
