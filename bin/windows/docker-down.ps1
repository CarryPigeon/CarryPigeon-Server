$ErrorActionPreference = 'Stop'

$BaseDir = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$ComposeFile = Join-Path $BaseDir 'docker-compose.yaml'

& docker compose --env-file (Join-Path $BaseDir '.env') -f $ComposeFile down @args
exit $LASTEXITCODE
