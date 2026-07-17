$ErrorActionPreference = 'Stop'

$BaseDir = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$ConfigDir = Join-Path $BaseDir 'config'

function Test-TcpPort {
    param(
        [Parameter(Mandatory = $true)][string]$TargetHost,
        [Parameter(Mandatory = $true)][int]$Port,
        [Parameter(Mandatory = $true)][string]$ServiceName
    )

    $client = [System.Net.Sockets.TcpClient]::new()
    try {
        $task = $client.ConnectAsync($TargetHost, $Port)
        if (-not $task.Wait(1000)) {
            throw 'timeout'
        }
        if (-not $client.Connected) {
            throw 'not connected'
        }
    }
    catch {
        throw "$ServiceName is not reachable at $TargetHost`:$Port. Run 'bin\\windows\\docker-up.bat' first and wait until dependencies are ready."
    }
    finally {
        $client.Dispose()
    }
}

$mysqlPort = if ([string]::IsNullOrWhiteSpace($env:MYSQL_PORT)) { 3306 } else { [int]$env:MYSQL_PORT }
$redisPort = if ([string]::IsNullOrWhiteSpace($env:REDIS_PORT)) { 6379 } else { [int]$env:REDIS_PORT }
$minioPort = if ([string]::IsNullOrWhiteSpace($env:MINIO_API_PORT)) { 9000 } else { [int]$env:MINIO_API_PORT }

Test-TcpPort -TargetHost '127.0.0.1' -Port $mysqlPort -ServiceName 'MySQL'
Test-TcpPort -TargetHost '127.0.0.1' -Port $redisPort -ServiceName 'Redis'
Test-TcpPort -TargetHost '127.0.0.1' -Port $minioPort -ServiceName 'MinIO'

& mvn -f (Join-Path $BaseDir 'pom.xml') -pl application-starter -am -DskipTests install
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

$springRunArguments = '--spring.config.additional-location=file:{0}/' -f ($ConfigDir -replace '\\', '/')
& mvn -f (Join-Path $BaseDir 'application-starter/pom.xml') -DskipTests "-Dspring-boot.run.arguments=$springRunArguments" spring-boot:run @args
exit $LASTEXITCODE
