# 启动本地 MySQL（手动启动，开机不自启）
# 用法：.\scripts\start-mysql-local.ps1

$ErrorActionPreference = "Stop"
$serviceName = "MySQL84"
$mysqlBin = "C:\Program Files\MySQL\MySQL Server 8.4\bin"

if (-not (Test-Path (Join-Path $mysqlBin "mysql.exe"))) {
    Write-Host "未找到本地 MySQL，请先以管理员运行: .\scripts\setup-mysql-local.ps1" -ForegroundColor Yellow
    exit 1
}

$svc = Get-Service -Name $serviceName -ErrorAction SilentlyContinue
if (-not $svc) {
    Write-Host "未找到服务 $serviceName，请先以管理员运行: .\scripts\setup-mysql-local.ps1" -ForegroundColor Yellow
    exit 1
}

if ($svc.Status -eq "Running") {
    Write-Host "MySQL 已在运行 ($serviceName)" -ForegroundColor Green
} else {
    Write-Host "正在启动 $serviceName ..." -ForegroundColor Green
    try {
        Start-Service $serviceName
    } catch {
        Write-Host "启动失败，请右键「以管理员身份运行」本脚本。" -ForegroundColor Yellow
        exit 1
    }
}

Write-Host "等待 MySQL 就绪..." -ForegroundColor Green
$ready = $false
for ($i = 0; $i -lt 30; $i++) {
    $tcp = Test-NetConnection -ComputerName 127.0.0.1 -Port 3306 -WarningAction SilentlyContinue
    if ($tcp.TcpTestSucceeded) {
        $ready = $true
        break
    }
    Start-Sleep -Seconds 1
}

if (-not $ready) {
    Write-Host "服务已启动但连接失败，请检查: Get-Service $serviceName" -ForegroundColor Yellow
    exit 1
}

Write-Host ""
Write-Host "MySQL 已就绪" -ForegroundColor Green
Write-Host "  主机: 127.0.0.1:3306"
Write-Host "  库名: agent_ai"
Write-Host "  用户: root"
Write-Host "  密码: root"
Write-Host ""
Write-Host "停止: .\scripts\stop-mysql-local.ps1"
Write-Host "后端: mvn -pl agent-server spring-boot:run"
