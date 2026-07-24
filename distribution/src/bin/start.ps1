$ErrorActionPreference = 'Stop'

$BaseDir = Split-Path -Parent $PSScriptRoot
$ConfigDir = Join-Path $BaseDir 'config'
$AppDir = Join-Path $BaseDir 'app'
$LibDir = Join-Path $BaseDir 'lib'
$PluginDir = Join-Path $BaseDir 'plugins'
$SafeMode = $false
$ForwardArgs = [System.Collections.Generic.List[string]]::new()
foreach ($argument in $args) {
    if ($argument -eq '--safe-mode') {
        $SafeMode = $true
    } else {
        $ForwardArgs.Add($argument)
    }
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

if (-not (Test-Path -LiteralPath $LibDir -PathType Container)) {
    throw "lib directory not found under $BaseDir"
}

if (-not (Test-Path -LiteralPath $PluginDir -PathType Container)) {
    throw "plugins directory not found under $BaseDir"
}

$appJar = Get-ChildItem -LiteralPath $AppDir -Filter 'application-starter-*.jar' |
    Where-Object { $_.Name -notlike '*-exec.jar' -and $_.Name -notlike '*.original' } |
    Select-Object -First 1

if ($null -eq $appJar) {
    throw "application-starter thin jar not found under $AppDir"
}

$appJarCandidates = @(Get-ChildItem -LiteralPath $AppDir -Filter 'application-starter-*.jar' |
    Where-Object { $_.Name -notlike '*-exec.jar' -and $_.Name -notlike '*.original' })
if ($appJarCandidates.Count -ne 1) {
    throw "expected exactly one application-starter thin jar under $AppDir"
}

New-Item -ItemType Directory -Force -Path $DefaultLogHome | Out-Null

$classPath = '{0};{1}\*' -f $appJar.FullName, $LibDir
if (-not $SafeMode) {
    $classPath = '{0};{1}\*' -f $classPath, $PluginDir
}
$springConfigArg = '--spring.config.additional-location=file:{0}/' -f ($ConfigDir -replace '\\', '/')
$loggingArg = '-Dlogging.config=file:{0}' -f ((Join-Path $ConfigDir 'log4j2-spring.xml') -replace '\\', '/')
$logHomeArg = '-Dcp.log.home={0}' -f $DefaultLogHome

& java $loggingArg $logHomeArg -cp $classPath team.carrypigeon.backend.starter.ApplicationStarter $springConfigArg @ForwardArgs
exit $LASTEXITCODE
