# 停止本地 MySQL
# 用法：.\scripts\stop-mysql-local.ps1

$ErrorActionPreference = "Stop"
$serviceName = "MySQL84"

$svc = Get-Service -Name $serviceName -ErrorAction SilentlyContinue
if (-not $svc) {
    Write-Host "未找到服务 $serviceName" -ForegroundColor Yellow
    exit 0
}

if ($svc.Status -eq "Stopped") {
    Write-Host "MySQL 已停止 ($serviceName)" -ForegroundColor Green
    exit 0
}

Write-Host "正在停止 $serviceName ..." -ForegroundColor Green
try {
    Stop-Service $serviceName -Force
    Write-Host "MySQL 已停止" -ForegroundColor Green
} catch {
    Write-Host "停止失败，正在请求管理员权限..." -ForegroundColor Yellow
    Start-Process powershell.exe -ArgumentList "-NoProfile -ExecutionPolicy Bypass -Command `"Stop-Service -Name $serviceName -Force`"" -Verb RunAs -Wait
    $svc.Refresh()
    if ($svc.Status -eq "Stopped") {
        Write-Host "MySQL 已停止" -ForegroundColor Green
    } else {
        Write-Host "停止失败，请手动以管理员运行: Stop-Service $serviceName" -ForegroundColor Yellow
        exit 1
    }
}
