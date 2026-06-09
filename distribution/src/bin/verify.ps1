$ErrorActionPreference = 'Stop'

$BaseDir = Split-Path -Parent $PSScriptRoot
$StrictEnv = $false

foreach ($arg in $args) {
    if ($arg -eq '--strict-env') {
        $StrictEnv = $true
        continue
    }

    throw "Unsupported argument: $arg. Usage: verify.ps1 [--strict-env]"
}

function Require-Path {
    param(
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)][string]$Description
    )

    if (-not (Test-Path -LiteralPath $Path)) {
        throw "Missing $Description: $Path"
    }
}

function Require-File {
    param(
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)][string]$Description
    )

    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
        throw "Missing $Description: $Path"
    }
}

function Require-EnvValue {
    param(
        [string]$Value,
        [Parameter(Mandatory = $true)][string]$Name
    )

    if ([string]::IsNullOrWhiteSpace($Value)) {
        throw "Missing required env value in .env: $Name"
    }
}

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

$AppDir = Join-Path $BaseDir 'app'
$SystemdDir = Join-Path (Join-Path $BaseDir 'service') 'systemd'

Require-Path -Path $AppDir -Description 'app directory'
Require-Path -Path (Join-Path $BaseDir 'libs') -Description 'libs directory'
Require-Path -Path (Join-Path $BaseDir 'config') -Description 'config directory'
Require-Path -Path (Join-Path $BaseDir 'bin') -Description 'bin directory'
Require-Path -Path $SystemdDir -Description 'systemd example directory'

Require-File -Path (Join-Path $BaseDir 'README.md') -Description 'distribution README'
Require-File -Path (Join-Path $BaseDir '.env.example') -Description 'environment template'
Require-File -Path (Join-Path $BaseDir 'config/application.yaml') -Description 'application configuration'
Require-File -Path (Join-Path $BaseDir 'config/log4j2-spring.xml') -Description 'logging configuration'
Require-File -Path (Join-Path $SystemdDir 'carrypigeon.service') -Description 'systemd unit example'
Require-File -Path (Join-Path $SystemdDir 'README.md') -Description 'systemd usage guide'
Require-File -Path (Join-Path $PSScriptRoot 'start.ps1') -Description 'foreground launcher'
Require-File -Path (Join-Path $PSScriptRoot 'start-background.ps1') -Description 'background launcher'
Require-File -Path (Join-Path $PSScriptRoot 'stop.ps1') -Description 'stop launcher'
Require-File -Path (Join-Path $PSScriptRoot 'verify.ps1') -Description 'package verification launcher'

$appJar = Get-ChildItem -LiteralPath $AppDir -Filter 'application-starter-*.jar' |
    Where-Object { $_.Name -notlike '*-exec.jar' -and $_.Name -notlike '*.original' } |
    Select-Object -First 1

if ($null -eq $appJar) {
    throw "application-starter thin jar not found under $AppDir"
}

if ($StrictEnv) {
    $envFile = Join-Path $BaseDir '.env'
    if (-not (Test-Path -LiteralPath $envFile)) {
        throw "Strict env verification requested, but $envFile is missing."
    }

    Import-EnvFile -Path $envFile
    Require-EnvValue -Value $env:CP_CHAT_AUTH_JWT_SECRET -Name 'CP_CHAT_AUTH_JWT_SECRET'
    Require-EnvValue -Value $env:CP_CHAT_SERVER_ID -Name 'CP_CHAT_SERVER_ID'

    if ($env:CP_CHAT_AUTH_JWT_SECRET.Length -lt 32) {
        throw 'CP_CHAT_AUTH_JWT_SECRET must be at least 32 characters.'
    }
}

Write-Host 'Distribution package verification passed.'
Write-Host "Base directory: $BaseDir"
Write-Host "Thin jar: $($appJar.FullName)"
if ($StrictEnv) {
    Write-Host 'Environment readiness: strict verification passed'
}
else {
    Write-Host 'Environment readiness: skipped (.env not required without --strict-env)'
}
