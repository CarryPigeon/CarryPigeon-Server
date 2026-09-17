$ErrorActionPreference = 'Stop'

$BaseDir = Split-Path -Parent $PSScriptRoot
$PidFile = Join-Path (Join-Path $BaseDir 'run') 'application.pid'
$StartScript = Join-Path $PSScriptRoot 'start.ps1'

function Get-ProcessInfo {
    param([Parameter(Mandatory = $true)][int]$ProcessId)

    return Get-CimInstance Win32_Process -Filter "ProcessId = $ProcessId" -ErrorAction SilentlyContinue
}

function Test-ApplicationLauncherProcess {
    param([Parameter(Mandatory = $true)][int]$ProcessId)

    $processInfo = Get-ProcessInfo -ProcessId $ProcessId
    if ($null -eq $processInfo -or [string]::IsNullOrWhiteSpace([string]$processInfo.CommandLine)) {
        return $false
    }
    return $processInfo.CommandLine.Contains('team.carrypigeon.backend.starter.ApplicationStarter') -or
        $processInfo.CommandLine.Contains($StartScript)
}

function Get-DescendantProcessIds {
    param([Parameter(Mandatory = $true)][int]$ParentProcessId)

    $descendants = [System.Collections.Generic.List[int]]::new()
    foreach ($child in Get-CimInstance Win32_Process -Filter "ParentProcessId = $ParentProcessId" -ErrorAction SilentlyContinue) {
        foreach ($descendantId in Get-DescendantProcessIds -ParentProcessId $child.ProcessId) {
            $descendants.Add($descendantId)
        }
        $descendants.Add([int]$child.ProcessId)
    }
    return $descendants
}

if (-not (Test-Path -LiteralPath $PidFile)) {
    throw "PID file not found: $PidFile"
}

$pidValue = (Get-Content -LiteralPath $PidFile | Select-Object -First 1).Trim()
$applicationPid = 0
if (-not [int]::TryParse($pidValue, [ref]$applicationPid) -or $applicationPid -le 0) {
    throw "PID file does not contain a positive process ID: $PidFile"
}

$process = Get-Process -Id $applicationPid -ErrorAction SilentlyContinue
if ($null -eq $process) {
    Write-Host "Process $pidValue is not running"
    Remove-Item -LiteralPath $PidFile -Force -ErrorAction SilentlyContinue
    exit 0
}

if (-not (Test-ApplicationLauncherProcess -ProcessId $applicationPid)) {
    Remove-Item -LiteralPath $PidFile -Force -ErrorAction SilentlyContinue
    throw "Refusing to stop PID $applicationPid because it is not the CarryPigeon application process"
}

$descendantProcessIds = @(Get-DescendantProcessIds -ParentProcessId $applicationPid)
foreach ($descendantProcessId in $descendantProcessIds) {
    Stop-Process -Id $descendantProcessId -ErrorAction SilentlyContinue
}
Stop-Process -Id $applicationPid -ErrorAction SilentlyContinue
$deadline = (Get-Date).AddSeconds(30)
while ((Get-Date) -lt $deadline) {
    $process.Refresh()
    if ($process.HasExited) {
        Write-Host "Application process $applicationPid stopped"
        Remove-Item -LiteralPath $PidFile -Force -ErrorAction SilentlyContinue
        exit 0
    }
    Start-Sleep -Seconds 1
}

foreach ($descendantProcessId in $descendantProcessIds) {
    Stop-Process -Id $descendantProcessId -Force -ErrorAction SilentlyContinue
}
Stop-Process -Id $applicationPid -Force -ErrorAction SilentlyContinue
Write-Host "Application process $applicationPid did not stop gracefully and was killed"
Remove-Item -LiteralPath $PidFile -Force -ErrorAction SilentlyContinue
