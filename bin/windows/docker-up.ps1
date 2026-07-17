$ErrorActionPreference = 'Stop'

$BaseDir = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$ComposeFile = Join-Path $BaseDir 'docker-compose.yaml'
$env:COMPOSE_DISABLE_ENV_FILE = '1'

& docker compose -f $ComposeFile up -d --remove-orphans
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

function Wait-ContainerHealth {
    param(
        [Parameter(Mandatory = $true)][string]$ContainerName,
        [Parameter(Mandatory = $true)][string]$ExpectedStatus,
        [Parameter(Mandatory = $true)][int]$TimeoutSeconds
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        $status = & docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' $ContainerName 2>$null
        if ($status -eq $ExpectedStatus) {
            return
        }
        Start-Sleep -Seconds 2
    }

    throw "Timed out waiting for $ContainerName to become $ExpectedStatus"
}

function Wait-ContainerExitCode {
    param(
        [Parameter(Mandatory = $true)][string]$ContainerName,
        [Parameter(Mandatory = $true)][int]$ExpectedExitCode,
        [Parameter(Mandatory = $true)][int]$TimeoutSeconds
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        $status = & docker inspect -f '{{.State.Status}}' $ContainerName 2>$null
        $exitCode = & docker inspect -f '{{.State.ExitCode}}' $ContainerName 2>$null
        if ($status -eq 'exited' -and [int]$exitCode -eq $ExpectedExitCode) {
            return
        }
        Start-Sleep -Seconds 2
    }

    throw "Timed out waiting for $ContainerName to exit with code $ExpectedExitCode"
}

Wait-ContainerHealth -ContainerName 'carrypigeon-mysql' -ExpectedStatus 'healthy' -TimeoutSeconds 120
Wait-ContainerHealth -ContainerName 'carrypigeon-redis' -ExpectedStatus 'healthy' -TimeoutSeconds 60
Wait-ContainerHealth -ContainerName 'carrypigeon-minio' -ExpectedStatus 'healthy' -TimeoutSeconds 120
Wait-ContainerExitCode -ContainerName 'carrypigeon-minio-init' -ExpectedExitCode 0 -TimeoutSeconds 120

Write-Host 'Docker dependencies are ready.'
