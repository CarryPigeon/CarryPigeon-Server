$ErrorActionPreference = 'Stop'

$BaseDir = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$LogDir = Join-Path $BaseDir 'service-logs'
$LogFile = Join-Path $LogDir 'application-starter-stdout.log'
$PidFile = Join-Path $LogDir 'application-starter.pid'
$AppStartScript = Join-Path $PSScriptRoot 'app-start.ps1'

New-Item -ItemType Directory -Force -Path $LogDir | Out-Null

$process = Start-Process -FilePath 'powershell.exe' -ArgumentList @(
    '-NoProfile',
    '-ExecutionPolicy', 'Bypass',
    '-File', $AppStartScript
) + @args -PassThru -WindowStyle Hidden -RedirectStandardOutput $LogFile -RedirectStandardError $LogFile

Set-Content -LiteralPath $PidFile -Value $process.Id

$deadline = (Get-Date).AddSeconds(60)
while ((Get-Date) -lt $deadline) {
    $logContent = Get-Content -LiteralPath $LogFile -ErrorAction SilentlyContinue
    if ($logContent -match 'Started ApplicationStarter') {
        Write-Host "Started application-starter in background. PID=$($process.Id)"
        Write-Host "Stdout log: $LogFile"
        exit 0
    }

    if ($process.HasExited) {
        Write-Error "application-starter exited before becoming ready. See log: $LogFile"
        exit 1
    }

    Start-Sleep -Seconds 2
}

Write-Error "application-starter did not become ready within timeout. See log: $LogFile"
exit 1
