@echo off
setlocal enabledelayedexpansion

set "BASE_DIR=%~dp0..\.."
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%BASE_DIR%\bin\windows\docker-reset.ps1" %*
exit /b %errorlevel%
