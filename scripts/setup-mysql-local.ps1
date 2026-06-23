# 本地 MySQL 8.4 初始化（MSI 仅安装二进制，需手动初始化数据目录与服务）
# 需以管理员身份运行

$ErrorActionPreference = "Stop"
$mysqlBase = "C:\Program Files\MySQL\MySQL Server 8.4"
$mysqlBin = Join-Path $mysqlBase "bin"
$dataDir = "C:\ProgramData\MySQL\MySQL Server 8.4\Data"
$iniPath = "C:\ProgramData\MySQL\MySQL Server 8.4\my.ini"
$serviceName = "MySQL84"
$rootPassword = "root"

if (-not (Test-Path (Join-Path $mysqlBin "mysqld.exe"))) {
    Write-Error "未找到 MySQL，请先安装 MSI：winget install Oracle.MySQL"
}

New-Item -ItemType Directory -Force -Path (Split-Path $dataDir) | Out-Null

$iniContent = @"
[mysqld]
basedir=C:/Program Files/MySQL/MySQL Server 8.4
datadir=C:/ProgramData/MySQL/MySQL Server 8.4/Data
port=3306
character-set-server=utf8mb4
collation-server=utf8mb4_unicode_ci

[client]
port=3306
default-character-set=utf8mb4
"@
if (-not (Test-Path $iniPath)) {
    [System.IO.File]::WriteAllText($iniPath, $iniContent, (New-Object System.Text.UTF8Encoding $false))
    Write-Host "已创建 $iniPath"
} else {
    # 修复 PowerShell BOM 导致的解析错误
    [System.IO.File]::WriteAllText($iniPath, $iniContent, (New-Object System.Text.UTF8Encoding $false))
    Write-Host "已更新 $iniPath"
}

if (-not (Test-Path (Join-Path $dataDir "mysql"))) {
    Write-Host "正在初始化数据目录..."
    & (Join-Path $mysqlBin "mysqld.exe") --defaults-file="$iniPath" --initialize-insecure --console
    if ($LASTEXITCODE -ne 0) { throw "mysqld --initialize 失败，退出码 $LASTEXITCODE" }
}

$svc = Get-Service -Name $serviceName -ErrorAction SilentlyContinue
if (-not $svc) {
    Write-Host "正在注册 Windows 服务 $serviceName ..."
    & (Join-Path $mysqlBin "mysqld.exe") --install $serviceName --defaults-file="$iniPath"
    if ($LASTEXITCODE -ne 0) { throw "mysqld --install 失败" }
}

# 手动启动，不开机自启
Set-Service -Name $serviceName -StartupType Manual
Write-Host "已设置 $serviceName 为手动启动（开机不自启）"

if ((Get-Service $serviceName).Status -ne "Running") {
    Write-Host "正在启动服务 $serviceName ..."
    Start-Service $serviceName
    Start-Sleep -Seconds 3
}

$mysql = Join-Path $mysqlBin "mysql.exe"
& $mysql -u root -e "ALTER USER 'root'@'localhost' IDENTIFIED BY '$rootPassword'; FLUSH PRIVILEGES;" 2>$null
if ($LASTEXITCODE -ne 0) {
    # 可能已设过密码，尝试用 root/root 连接
    & $mysql -u root -p$rootPassword -e "SELECT 1" 2>$null
    if ($LASTEXITCODE -ne 0) { throw "无法设置或验证 root 密码" }
}

Write-Host "MySQL 已就绪：127.0.0.1:3306，用户 root，密码 $rootPassword"
