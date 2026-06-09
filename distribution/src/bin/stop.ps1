$ErrorActionPreference = 'Stop'

$BaseDir = Split-Path -Parent $PSScriptRoot
$PidFile = Join-Path (Join-Path $BaseDir 'run') 'application.pid'

if (-not (Test-Path -LiteralPath $PidFile)) {
    throw "PID file not found: $PidFile"
}

$pidValue = (Get-Content -LiteralPath $PidFile | Select-Object -First 1).Trim()
if ([string]::IsNullOrWhiteSpace($pidValue)) {
    throw "PID file is empty: $PidFile"
}

$process = Get-Process -Id $pidValue -ErrorAction SilentlyContinue
if ($null -eq $process) {
    Write-Host "Process $pidValue is not running"
    Remove-Item -LiteralPath $PidFile -Force -ErrorAction SilentlyContinue
    exit 0
}

Stop-Process -Id $pidValue -ErrorAction SilentlyContinue
$deadline = (Get-Date).AddSeconds(30)
while ((Get-Date) -lt $deadline) {
    $process.Refresh()
    if ($process.HasExited) {
        Write-Host "Application process $pidValue stopped"
        Remove-Item -LiteralPath $PidFile -Force -ErrorAction SilentlyContinue
        exit 0
    }
    Start-Sleep -Seconds 1
}

Stop-Process -Id $pidValue -Force -ErrorAction SilentlyContinue
Write-Host "Application process $pidValue did not stop gracefully and was killed"
Remove-Item -LiteralPath $PidFile -Force -ErrorAction SilentlyContinue
