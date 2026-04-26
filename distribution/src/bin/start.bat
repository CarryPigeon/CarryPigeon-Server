@echo off
setlocal enabledelayedexpansion

set "BASE_DIR=%~dp0.."
set "APP_JAR="

for %%f in ("%BASE_DIR%\app\application-starter-*.jar") do (
    echo %%~nxf | findstr /C:"-exec.jar" >nul
    if errorlevel 1 (
        set "APP_JAR=%%~ff"
        goto :jar_found
    )
)

:jar_found
if "%APP_JAR%"=="" (
    echo application-starter thin jar not found under %BASE_DIR%\app
    exit /b 1
)

set "CLASSPATH=%APP_JAR%;%BASE_DIR%\libs\*"
java -cp "%CLASSPATH%" team.carrypigeon.backend.starter.ApplicationStarter %*
