#!/usr/bin/env bash
# 使用 Docker 启动 MySQL（Git Bash / Linux / macOS）
# 用法：./scripts/start-mysql-docker.sh

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$PROJECT_ROOT"

if ! command -v docker >/dev/null 2>&1; then
  echo "未检测到 Docker 命令。请先安装 Docker Desktop。"
  exit 1
fi

if ! docker info >/dev/null 2>&1; then
  echo "Docker 引擎未运行。请先启动 Docker Desktop，等到左下角显示 Engine running。"
  echo "  Windows: 开始菜单搜索 Docker Desktop 并打开"
  exit 1
fi

if [ ! -f "docker-compose.yml" ]; then
  echo "未找到 docker-compose.yml，当前目录：$(pwd)"
  exit 1
fi

if [ ! -f ".env" ] && [ -f ".env.example" ]; then
  cp .env.example .env
  echo "已从 .env.example 创建 .env（使用国内镜像源）"
fi

echo "拉取镜像并启动 MySQL 容器..."
echo "镜像: ${MYSQL_IMAGE:-docker.1ms.run/library/mysql:8.0}"
docker compose up -d

echo "等待 MySQL 就绪..."
ready=false
for _ in $(seq 1 30); do
  status="$(docker inspect -f '{{.State.Health.Status}}' agent-mysql 2>/dev/null || true)"
  if [ "$status" = "healthy" ]; then
    ready=true
    break
  fi
  sleep 2
done

echo ""
if [ "$ready" = true ]; then
  echo "MySQL 已就绪"
else
  echo "容器已启动，健康检查尚未通过。可执行：docker compose logs -f mysql"
fi
echo "  主机: 127.0.0.1:3306"
echo "  库名: agent_ai"
echo "  用户: root"
echo "  密码: root"
echo ""
echo "Spring Boot 启动："
echo "  mvn -pl agent-server spring-boot:run"
