$ErrorActionPreference = 'Stop'

$BaseDir = Split-Path -Parent $PSScriptRoot
$StrictConfig = $false

foreach ($arg in $args) {
    if ($arg -eq '--strict-config') {
        $StrictConfig = $true
        continue
    }

    throw "Unsupported argument: $arg. Usage: verify.ps1 [--strict-config]"
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

function Require-ConfigValue {
    param(
        [string]$Value,
        [Parameter(Mandatory = $true)][string]$Name
    )

    if ([string]::IsNullOrWhiteSpace($Value)) {
        throw "Missing required config value in config/application.yaml: $Name"
    }
}

function Get-YamlScalar {
    param(
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)][string]$PropertyPath
    )

    $target = $PropertyPath.Split('.')
    $stack = New-Object System.Collections.Generic.List[object]

    foreach ($rawLine in Get-Content -LiteralPath $Path -Encoding UTF8) {
        if ([string]::IsNullOrWhiteSpace($rawLine) -or $rawLine.TrimStart().StartsWith('#')) {
            continue
        }
        if ($rawLine -notmatch ':') {
            continue
        }

        $indent = $rawLine.Length - $rawLine.TrimStart(' ').Length
        $parts = $rawLine.Trim().Split(':', 2)
        $key = $parts[0].Trim()
        $value = $parts[1].Trim()

        while ($stack.Count -gt 0 -and $stack[$stack.Count - 1].Indent -ge $indent) {
            $stack.RemoveAt($stack.Count - 1)
        }
        $stack.Add([pscustomobject]@{ Indent = $indent; Key = $key })

        $current = @($stack | ForEach-Object { $_.Key })
        if (($current -join '.') -eq ($target -join '.')) {
            $value = ($value -replace '\s+#.*$', '').Trim()
            if ($value.Length -ge 2 -and (($value.StartsWith('"') -and $value.EndsWith('"')) -or ($value.StartsWith("'") -and $value.EndsWith("'")))) {
                $value = $value.Substring(1, $value.Length - 2)
            }
            return $value
        }
    }

    return ''
}

$AppDir = Join-Path $BaseDir 'app'
$SystemdDir = Join-Path (Join-Path $BaseDir 'service') 'systemd'

Require-Path -Path $AppDir -Description 'app directory'
Require-Path -Path (Join-Path $BaseDir 'libs') -Description 'libs directory'
Require-Path -Path (Join-Path $BaseDir 'config') -Description 'config directory'
Require-Path -Path (Join-Path $BaseDir 'bin') -Description 'bin directory'
Require-Path -Path $SystemdDir -Description 'systemd example directory'

Require-File -Path (Join-Path $BaseDir 'README.md') -Description 'distribution README'
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

if ($StrictConfig) {
    $configFile = Join-Path $BaseDir 'config/application.yaml'
    $jwtSecret = Get-YamlScalar -Path $configFile -PropertyPath 'cp.chat.auth.jwt.secret'
    $serverId = Get-YamlScalar -Path $configFile -PropertyPath 'cp.chat.server.id'

    Require-ConfigValue -Value $jwtSecret -Name 'cp.chat.auth.jwt.secret'
    Require-ConfigValue -Value $serverId -Name 'cp.chat.server.id'

    if ($jwtSecret.Length -lt 32) {
        throw 'cp.chat.auth.jwt.secret must be at least 32 characters.'
    }
}

Write-Host 'Distribution package verification passed.'
Write-Host "Base directory: $BaseDir"
Write-Host "Thin jar: $($appJar.FullName)"
if ($StrictConfig) {
    Write-Host 'Configuration readiness: strict verification passed'
}
else {
    Write-Host 'Configuration readiness: skipped (use --strict-config to validate required YAML values)'
}
