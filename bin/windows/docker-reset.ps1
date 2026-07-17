$ErrorActionPreference = 'Stop'

$BaseDir = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$ComposeFile = Join-Path $BaseDir 'docker-compose.yaml'
$env:COMPOSE_DISABLE_ENV_FILE = '1'

& docker compose -f $ComposeFile down -v @args
exit $LASTEXITCODE
