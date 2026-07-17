$ErrorActionPreference = 'Stop'

$BaseDir = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$ComposeFile = Join-Path $BaseDir 'docker-compose.yaml'
$CertDir = Join-Path $BaseDir 'deploy/nginx/certs'
$CertFile = Join-Path $CertDir 'fullchain.pem'
$KeyFile = Join-Path $CertDir 'privkey.pem'
$Domain = 'localhost'
$env:COMPOSE_DISABLE_ENV_FILE = '1'

function New-NginxDevCertificateWithOpenSsl {
    param(
        [Parameter(Mandatory = $true)][string]$OpenSslCommand,
        [Parameter(Mandatory = $true)][string]$ServerName,
        [Parameter(Mandatory = $true)][string]$CertificatePath,
        [Parameter(Mandatory = $true)][string]$KeyPath
    )

    & $OpenSslCommand req `
        -x509 `
        -newkey rsa:2048 `
        -sha256 `
        -days 825 `
        -nodes `
        -keyout $KeyPath `
        -out $CertificatePath `
        -subj "/CN=$ServerName" `
        -addext "subjectAltName=DNS:$ServerName,DNS:localhost,IP:127.0.0.1"
    return $LASTEXITCODE
}

function New-NginxDevCertificateWithDocker {
    param(
        [Parameter(Mandatory = $true)][string]$ServerName,
        [Parameter(Mandatory = $true)][string]$CertificateDirectory
    )

    $mount = "${CertificateDirectory}:/certs"
    & docker run `
        --rm `
        -e "CERT_DOMAIN=$ServerName" `
        -v $mount `
        nginx:1.27-alpine `
        /bin/sh `
        -c 'openssl req -x509 -newkey rsa:2048 -sha256 -days 825 -nodes -keyout /certs/privkey.pem -out /certs/fullchain.pem -subj "/CN=${CERT_DOMAIN}" -addext "subjectAltName=DNS:${CERT_DOMAIN},DNS:localhost,IP:127.0.0.1"'
    return $LASTEXITCODE
}

if (-not (Test-Path -LiteralPath $CertDir)) {
    New-Item -ItemType Directory -Path $CertDir | Out-Null
}

if (-not ((Test-Path -LiteralPath $CertFile) -and (Test-Path -LiteralPath $KeyFile))) {
    $openssl = Get-Command openssl -ErrorAction SilentlyContinue
    if ($openssl) {
        $exitCode = New-NginxDevCertificateWithOpenSsl `
            -OpenSslCommand $openssl.Source `
            -ServerName $Domain `
            -CertificatePath $CertFile `
            -KeyPath $KeyFile
    } else {
        Write-Host 'openssl not found on host; generating certificate with Docker image nginx:1.27-alpine'
        $exitCode = New-NginxDevCertificateWithDocker -ServerName $Domain -CertificateDirectory $CertDir
    }

    if ($exitCode -ne 0) {
        exit $exitCode
    }
    if (-not ((Test-Path -LiteralPath $CertFile) -and (Test-Path -LiteralPath $KeyFile))) {
        throw "Failed to generate Nginx development certificate under $CertDir"
    }
    Write-Host "Generated Nginx development certificate: $CertFile"
}

& docker compose -f $ComposeFile --profile edge up -d --remove-orphans nginx
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

$deadline = (Get-Date).AddSeconds(60)
while ((Get-Date) -lt $deadline) {
    $status = & docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' carrypigeon-nginx 2>$null
    if ($status -eq 'healthy') {
        Write-Host "Nginx reverse proxy is ready: https://$Domain"
        exit 0
    }
    Start-Sleep -Seconds 2
}

throw 'Timed out waiting for carrypigeon-nginx to become healthy'
