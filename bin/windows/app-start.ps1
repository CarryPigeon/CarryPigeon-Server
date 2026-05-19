$ErrorActionPreference = 'Stop'

$BaseDir = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$EnvFile = Join-Path $BaseDir '.env'
$EnvTemplateFile = Join-Path $BaseDir '.env.example'

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
            $name = $Matches.name.Trim()
            $value = $Matches.value
            Set-Item -Path "Env:$name" -Value $value
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
        throw "$ServiceName is not reachable at $Host`:$Port. Run 'bin\\windows\\docker-up.bat' first and wait until dependencies are ready."
    }
    finally {
        $client.Dispose()
    }
}

if (Test-Path -LiteralPath $EnvFile) {
    Import-EnvFile -Path $EnvFile
}
elseif (Test-Path -LiteralPath $EnvTemplateFile) {
    Import-EnvFile -Path $EnvTemplateFile
}

if ([string]::IsNullOrWhiteSpace($env:CP_CHAT_AUTH_JWT_SECRET)) {
    throw 'Missing required configuration: CP_CHAT_AUTH_JWT_SECRET. Set it in .env or export it before running bin\\windows\\app-start.ps1.'
}

if ($env:CP_CHAT_AUTH_JWT_SECRET.Length -lt 32) {
    throw 'CP_CHAT_AUTH_JWT_SECRET must be at least 32 characters.'
}

$mysqlPort = if ([string]::IsNullOrWhiteSpace($env:MYSQL_PORT)) { 3306 } else { [int]$env:MYSQL_PORT }
$redisPort = if ([string]::IsNullOrWhiteSpace($env:REDIS_PORT)) { 6379 } else { [int]$env:REDIS_PORT }
$minioPort = if ([string]::IsNullOrWhiteSpace($env:MINIO_API_PORT)) { 9000 } else { [int]$env:MINIO_API_PORT }

Test-TcpPort -Host '127.0.0.1' -Port $mysqlPort -ServiceName 'MySQL'
Test-TcpPort -Host '127.0.0.1' -Port $redisPort -ServiceName 'Redis'
Test-TcpPort -Host '127.0.0.1' -Port $minioPort -ServiceName 'MinIO'

& mvn -f (Join-Path $BaseDir 'pom.xml') -pl application-starter -am -DskipTests install
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

& mvn -f (Join-Path $BaseDir 'application-starter/pom.xml') -DskipTests spring-boot:run @args
exit $LASTEXITCODE
