$ErrorActionPreference = 'Stop'

$BaseDir = Split-Path -Parent $PSScriptRoot
$EnvFile = Join-Path $BaseDir '.env'
$EnvTemplateFile = Join-Path $BaseDir '.env.example'
$ConfigDir = Join-Path $BaseDir 'config'
$AppDir = Join-Path $BaseDir 'app'
$LibDir = Join-Path $BaseDir 'libs'

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

function Test-TcpPort {
    param(
        [Parameter(Mandatory = $true)][string]$Host,
        [Parameter(Mandatory = $true)][int]$Port,
        [Parameter(Mandatory = $true)][string]$ServiceName
    )

    $client = [System.Net.Sockets.TcpClient]::new()
    try {
        $task = $client.ConnectAsync($Host, $Port)
        if (-not $task.Wait(1000)) {
            throw 'timeout'
        }
        if (-not $client.Connected) {
            throw 'not connected'
        }
    }
    catch {
        throw "$ServiceName is not reachable at $Host`:$Port."
    }
    finally {
        $client.Dispose()
    }
}

function Test-Enabled {
    param([string]$Value, [bool]$Default = $true)

    if ([string]::IsNullOrWhiteSpace($Value)) {
        return $Default
    }

    return $Value -notin @('false', 'FALSE', 'False', '0', 'off', 'OFF', 'Off', 'no', 'NO', 'No')
}

if (Test-Path -LiteralPath $EnvFile) {
    Import-EnvFile -Path $EnvFile
}
elseif (Test-Path -LiteralPath $EnvTemplateFile) {
    Import-EnvFile -Path $EnvTemplateFile
}

$DefaultLogHome = if ([string]::IsNullOrWhiteSpace($env:CP_LOG_HOME)) {
    Join-Path $BaseDir 'service-logs'
} else {
    $env:CP_LOG_HOME
}

if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    throw 'java executable not found in PATH.'
}

if (-not (Test-Path -LiteralPath (Join-Path $ConfigDir 'application.yaml'))) {
    throw "application.yaml not found under $ConfigDir"
}

if (-not (Test-Path -LiteralPath (Join-Path $ConfigDir 'log4j2-spring.xml'))) {
    throw "log4j2-spring.xml not found under $ConfigDir"
}

$appJar = Get-ChildItem -LiteralPath $AppDir -Filter 'application-starter-*.jar' |
    Where-Object { $_.Name -notlike '*-exec.jar' -and $_.Name -notlike '*.original' } |
    Select-Object -First 1

if ($null -eq $appJar) {
    throw "application-starter thin jar not found under $AppDir"
}

if ([string]::IsNullOrWhiteSpace($env:CP_CHAT_AUTH_JWT_SECRET)) {
    throw 'Missing required configuration: CP_CHAT_AUTH_JWT_SECRET. Set it in .env or export it before running the distribution package.'
}

if ($env:CP_CHAT_AUTH_JWT_SECRET.Length -lt 32) {
    throw 'CP_CHAT_AUTH_JWT_SECRET must be at least 32 characters.'
}

$mysqlHost = if ([string]::IsNullOrWhiteSpace($env:MYSQL_HOST)) { '127.0.0.1' } else { $env:MYSQL_HOST }
$mysqlPort = if ([string]::IsNullOrWhiteSpace($env:MYSQL_PORT)) { 3306 } else { [int]$env:MYSQL_PORT }
$redisHost = if ([string]::IsNullOrWhiteSpace($env:REDIS_HOST)) { '127.0.0.1' } else { $env:REDIS_HOST }
$redisPort = if ([string]::IsNullOrWhiteSpace($env:REDIS_PORT)) { 6379 } else { [int]$env:REDIS_PORT }
$minioHost = if ([string]::IsNullOrWhiteSpace($env:MINIO_HOST)) { '127.0.0.1' } else { $env:MINIO_HOST }
$minioPort = if ([string]::IsNullOrWhiteSpace($env:MINIO_API_PORT)) { 9000 } else { [int]$env:MINIO_API_PORT }

if (Test-Enabled -Value $env:CP_INFRASTRUCTURE_SERVICE_DATABASE_ENABLED -Default $true) {
    Test-TcpPort -Host $mysqlHost -Port $mysqlPort -ServiceName 'MySQL'
}

if (Test-Enabled -Value $env:CP_INFRASTRUCTURE_SERVICE_CACHE_ENABLED -Default $true) {
    Test-TcpPort -Host $redisHost -Port $redisPort -ServiceName 'Redis'
}

if (Test-Enabled -Value $env:CP_INFRASTRUCTURE_SERVICE_STORAGE_ENABLED -Default $true) {
    Test-TcpPort -Host $minioHost -Port $minioPort -ServiceName 'MinIO'
}

New-Item -ItemType Directory -Force -Path $DefaultLogHome | Out-Null

$classPath = '{0};{1}\*' -f $appJar.FullName, $LibDir
$springConfigArg = '--spring.config.additional-location=file:{0}/' -f ($ConfigDir -replace '\\', '/')
$loggingArg = '-Dlogging.config=file:{0}' -f ((Join-Path $ConfigDir 'log4j2-spring.xml') -replace '\\', '/')
$logHomeArg = '-Dcp.log.home={0}' -f $DefaultLogHome

& java $loggingArg $logHomeArg -cp $classPath team.carrypigeon.backend.starter.ApplicationStarter $springConfigArg @args
exit $LASTEXITCODE
