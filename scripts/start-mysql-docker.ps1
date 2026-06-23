# 使用 Docker 启动 MySQL（需先安装 Docker Desktop）
# 用法：.\scripts\start-mysql-docker.ps1

$ErrorActionPreference = "Stop"
$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $projectRoot

if (-not (Test-Path ".env") -and (Test-Path ".env.example")) {
    Copy-Item ".env.example" ".env"
    Write-Host "已从 .env.example 创建 .env（使用国内镜像源）" -ForegroundColor Yellow
}

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    Write-Host "未检测到 Docker 命令。请先安装 Docker Desktop。" -ForegroundColor Yellow
    exit 1
}

docker info *>$null
if ($LASTEXITCODE -ne 0) {
    Write-Host "Docker 引擎未运行。请先启动 Docker Desktop，等到左下角显示 Engine running。" -ForegroundColor Yellow
    exit 1
}

Write-Host "拉取镜像并启动 MySQL 容器..." -ForegroundColor Green
docker compose up -d

Write-Host "等待 MySQL 就绪..." -ForegroundColor Green
$ready = $false
for ($i = 0; $i -lt 30; $i++) {
    $status = docker inspect -f "{{.State.Health.Status}}" agent-mysql 2>$null
    if ($status -eq "healthy") {
        $ready = $true
        break
    }
    Start-Sleep -Seconds 2
}

if (-not $ready) {
    Write-Host "容器已启动，健康检查尚未通过。可执行：docker compose logs -f mysql" -ForegroundColor Yellow
    exit 0
}

Write-Host ""
Write-Host "MySQL 已就绪" -ForegroundColor Green
Write-Host "  主机: 127.0.0.1:3306"
Write-Host "  库名: agent_ai"
Write-Host "  用户: root"
Write-Host "  密码: root"
Write-Host ""
Write-Host "Spring Boot 启动（dev profile）："
Write-Host "  mvn -pl agent-server spring-boot:run"
