@echo off
setlocal enabledelayedexpansion

set "BASE_DIR=%~dp0..\.."
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%BASE_DIR%\bin\windows\docker-down.ps1" %*
exit /b %errorlevel%
