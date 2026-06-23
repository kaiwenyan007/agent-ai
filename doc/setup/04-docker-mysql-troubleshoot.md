# Docker 拉镜像失败排查（Windows）

## 现象

```
connectex: An attempt was made to access a socket in a way forbidden by its access permissions
```

多为 **Hyper-V / winnat 占用出站端口** 或 **防火墙/VPN** 导致，与项目代码无关。

---

## 方案 A：Docker Desktop 内置 Hub 代理（优先试）

项目 `.env` 已默认：

```
MYSQL_IMAGE=hubproxy.docker.internal:5555/library/mysql:8.0
```

```powershell
cd C:\Users\12782\dev\agent-ai
docker pull hubproxy.docker.internal:5555/library/mysql:8.0
docker compose up -d
```

---

## 方案 B：重置 Windows NAT（需管理员 PowerShell）

```powershell
net stop winnat
net start winnat
```

然后重启 Docker Desktop，再执行 `docker compose up -d`。

若仍失败，可调整动态端口范围后**重启电脑**：

```powershell
netsh int ipv4 set dynamic tcp start=49152 num=16384
netsh int ipv6 set dynamic tcp start=49152 num=16384
```

---

## 方案 C：Docker Desktop 镜像加速

Settings → Docker Engine，合并 `docker/daemon.json.example` 中的 `registry-mirrors`，Apply & Restart。

`.env` 改回：

```
MYSQL_IMAGE=mysql:8.0
```

---

## 方案 D：暂不装 MySQL，继续用 H2

学习 v0.1～v0.2 足够：

```powershell
mvn -pl agent-server spring-boot:run "-Dspring-boot.run.profiles=h2"
```

等网络/端口问题解决后再切 MySQL。

---

## 验证 MySQL 容器

```powershell
docker ps
docker exec agent-mysql mysql -uroot -proot -e "USE agent_ai; SHOW TABLES;"
```
