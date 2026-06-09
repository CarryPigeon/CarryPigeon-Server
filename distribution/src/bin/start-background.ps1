$ErrorActionPreference = 'Stop'

$BaseDir = Split-Path -Parent $PSScriptRoot
$EnvFile = Join-Path $BaseDir '.env'
$EnvTemplateFile = Join-Path $BaseDir '.env.example'
$RunDir = Join-Path $BaseDir 'run'
$PidFile = Join-Path $RunDir 'application.pid'
$StartScript = Join-Path $PSScriptRoot 'start.ps1'

function Import-EnvFile {
    param([Parameter(Mandatory = $true)][string]$Path)

    if (-not (Test-Path -LiteralPath $Path)) {
        return
    }

    Get-Content -LiteralPath $Path | ForEach-Object {
        $line = $_.Trim()
        if (-not $line -or $line.StartsWith('#')) {
            return
        }

        if ($line -match '^(?<name>[^=]+)=(?<value>.*)$') {
            Set-Item -Path "Env:$($Matches.name.Trim())" -Value $Matches.value
        }
    }
}

if (Test-Path -LiteralPath $EnvFile) {
    Import-EnvFile -Path $EnvFile
}
elseif (Test-Path -LiteralPath $EnvTemplateFile) {
    Import-EnvFile -Path $EnvTemplateFile
}

$LogDir = if ([string]::IsNullOrWhiteSpace($env:CP_LOG_HOME)) {
    Join-Path $BaseDir 'service-logs'
} else {
    $env:CP_LOG_HOME
}
$LogFile = Join-Path $LogDir 'application-stdout.log'

New-Item -ItemType Directory -Force -Path $RunDir | Out-Null
New-Item -ItemType Directory -Force -Path $LogDir | Out-Null

if (Test-Path -LiteralPath $PidFile) {
    $existingPid = (Get-Content -LiteralPath $PidFile -ErrorAction SilentlyContinue | Select-Object -First 1).Trim()
    if ($existingPid) {
        $existingProcess = Get-Process -Id $existingPid -ErrorAction SilentlyContinue
        if ($null -ne $existingProcess) {
            throw "Application is already running with PID $existingPid"
        }
    }
    Remove-Item -LiteralPath $PidFile -Force -ErrorAction SilentlyContinue
}

$process = Start-Process -FilePath 'powershell.exe' -ArgumentList @(
    '-NoProfile',
    '-ExecutionPolicy', 'Bypass',
    '-File', $StartScript
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
        Remove-Item -LiteralPath $PidFile -Force -ErrorAction SilentlyContinue
        throw "application-starter exited before becoming ready. See log: $LogFile"
    }

    Start-Sleep -Seconds 2
}

if (-not $process.HasExited) {
    Stop-Process -Id $process.Id -Force -ErrorAction SilentlyContinue
}
Remove-Item -LiteralPath $PidFile -Force -ErrorAction SilentlyContinue
throw "application-starter did not become ready within timeout. See log: $LogFile"
