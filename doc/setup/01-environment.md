# 环境准备

> **重要**：本项目需要 **JDK 21+**。Spring Boot 3.4 / MyBatis-Plus 3.5 **不支持 Java 8**。  
> 若 `java -version` 显示 1.8，请先安装 JDK 21 并设置 `JAVA_HOME`。

## 1. 必装

| 软件 | 版本 | 验证命令 |
|------|------|----------|
| JDK | **21+**（必须） | `java -version` 应显示 21.x |
| Maven | **3.9+** | `mvn -version` |
| Git | 任意 | `git --version` |

### Windows 安装 JDK 21

1. 下载 [Eclipse Temurin 21](https://adoptium.net/temurin/releases/?version=21)  
2. 安装到例如 `C:\Program Files\Eclipse Adoptium\jdk-21.x.x`  
3. 设置环境变量：
   - `JAVA_HOME` = JDK 21 安装目录
   - `Path` 最前面加入 `%JAVA_HOME%\bin`（须优先于 Java 8 路径）
4. **新开** PowerShell 验证：

```powershell
java -version   # 应显示 21.x
mvn -version    # Java version 也应为 21
```

### IDEA 配置

File → Project Structure → SDK 选 **21**；Maven Runner JRE 也选 21。

---

## 2. 数据库（二选一）

| 方式 | 说明 | 文档 |
|------|------|------|
| **MySQL 8** | 推荐，数据持久化 | [02-mysql-install.md](./02-mysql-install.md) |
| **H2 内存库** | 免安装，仅 v0.1 快速验证 | `profile=h2` 启动即可 |

---

## 3. v0.4 起需要

| 软件 | 版本 | 验证 |
|------|------|------|
| Node.js | 20 LTS+ | `node -v` |
| npm / pnpm | 随 Node | `npm -v` |

---

## 4. v0.7 起（RAG）

- **Chroma**：以本地目录 `.chroma/users/{userId}/` 存储，**无需单独安装服务**
- **Embedding**：
  - 推荐 API 模式（配置 `agent.llm` 同源或独立 Embedding API）
  - 或本地 Ollama（可选，见 v0.7 文档）

---

## 5. IDE 推荐

- IntelliJ IDEA（已检测到 `.idea` 目录）
- 插件：Lombok、MyBatisX（可选）

---

## 下一步

→ [02-mysql-install.md](./02-mysql-install.md) 或直接进入 [v0.1 工程骨架](../iterations/v0.1-project-skeleton.md)
