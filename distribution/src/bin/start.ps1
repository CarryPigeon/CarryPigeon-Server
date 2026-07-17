$ErrorActionPreference = 'Stop'

$BaseDir = Split-Path -Parent $PSScriptRoot
$ConfigDir = Join-Path $BaseDir 'config'
$AppDir = Join-Path $BaseDir 'app'
$LibDir = Join-Path $BaseDir 'libs'

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

New-Item -ItemType Directory -Force -Path $DefaultLogHome | Out-Null

$classPath = '{0};{1}\*' -f $appJar.FullName, $LibDir
$springConfigArg = '--spring.config.additional-location=file:{0}/' -f ($ConfigDir -replace '\\', '/')
$loggingArg = '-Dlogging.config=file:{0}' -f ((Join-Path $ConfigDir 'log4j2-spring.xml') -replace '\\', '/')
$logHomeArg = '-Dcp.log.home={0}' -f $DefaultLogHome

& java $loggingArg $logHomeArg -cp $classPath team.carrypigeon.backend.starter.ApplicationStarter $springConfigArg @args
exit $LASTEXITCODE
