$ErrorActionPreference = 'Stop'

$BaseDir = Split-Path -Parent $PSScriptRoot
$RunDir = Join-Path $BaseDir 'run'
$PidFile = Join-Path $RunDir 'application.pid'
$StartScript = Join-Path $PSScriptRoot 'start.ps1'

$LogDir = if ([string]::IsNullOrWhiteSpace($env:CP_LOG_HOME)) {
    Join-Path $BaseDir 'service-logs'
} else {
    $env:CP_LOG_HOME
}
$LogFile = Join-Path $LogDir 'application-stdout.log'
$ErrorLogFile = Join-Path $LogDir 'application-stderr.log'
$ReadinessUrl = if ([string]::IsNullOrWhiteSpace($env:CP_READINESS_URL)) {
    $serverPort = if ([string]::IsNullOrWhiteSpace($env:SERVER_PORT)) { '8080' } else { $env:SERVER_PORT }
    "http://127.0.0.1:$serverPort/internal/readiness"
} else {
    $env:CP_READINESS_URL
}

function Get-ProcessCommandLine {
    param([Parameter(Mandatory = $true)][int]$ProcessId)

    $processInfo = Get-CimInstance Win32_Process -Filter "ProcessId = $ProcessId" -ErrorAction SilentlyContinue
    if ($null -eq $processInfo) {
        return ''
    }
    return [string]$processInfo.CommandLine
}

function Test-ApplicationLauncherProcess {
    param([Parameter(Mandatory = $true)][int]$ProcessId)

    $commandLine = Get-ProcessCommandLine -ProcessId $ProcessId
    return -not [string]::IsNullOrWhiteSpace($commandLine) -and
        ($commandLine.Contains('team.carrypigeon.backend.starter.ApplicationStarter') -or
            $commandLine.Contains($StartScript))
}

function Test-ApplicationReadiness {
    try {
        $response = Invoke-WebRequest -Uri $ReadinessUrl -UseBasicParsing -TimeoutSec 2
        return [int]$response.StatusCode -eq 204
    }
    catch {
        return $false
    }
}

New-Item -ItemType Directory -Force -Path $RunDir | Out-Null
New-Item -ItemType Directory -Force -Path $LogDir | Out-Null

if (Test-Path -LiteralPath $PidFile) {
    $existingPid = (Get-Content -LiteralPath $PidFile -ErrorAction SilentlyContinue | Select-Object -First 1).Trim()
    $parsedPid = 0
    if ([int]::TryParse($existingPid, [ref]$parsedPid) -and $parsedPid -gt 0) {
        $existingProcess = Get-Process -Id $parsedPid -ErrorAction SilentlyContinue
        if ($null -ne $existingProcess -and (Test-ApplicationLauncherProcess -ProcessId $parsedPid)) {
            throw "Application is already running with PID $existingPid"
        }
    }
    Remove-Item -LiteralPath $PidFile -Force -ErrorAction SilentlyContinue
}

$process = Start-Process -FilePath 'powershell.exe' -ArgumentList @(
    '-NoProfile',
    '-ExecutionPolicy', 'Bypass',
    '-File', $StartScript
) + @args -PassThru -WindowStyle Hidden -RedirectStandardOutput $LogFile -RedirectStandardError $ErrorLogFile

Set-Content -LiteralPath $PidFile -Value $process.Id

$deadline = (Get-Date).AddSeconds(60)
while ((Get-Date) -lt $deadline) {
    if (Test-ApplicationReadiness) {
        Write-Host "Started application-starter in background. PID=$($process.Id)"
        Write-Host "Stdout log: $LogFile"
        Write-Host "Stderr log: $ErrorLogFile"
        exit 0
    }

    if ($process.HasExited) {
        Remove-Item -LiteralPath $PidFile -Force -ErrorAction SilentlyContinue
        throw "application-starter exited before becoming ready. See log: $LogFile"
    }

    Start-Sleep -Seconds 2
}

if (-not $process.HasExited) {
    Stop-Process -Id $process.Id -Force -ErrorAction SilentlyContinue
}
Remove-Item -LiteralPath $PidFile -Force -ErrorAction SilentlyContinue
throw "application-starter did not become ready within timeout at $ReadinessUrl. See log: $LogFile"
