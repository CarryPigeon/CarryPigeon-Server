# Windows Scripts

This directory contains the Windows PowerShell and batch startup scripts.

## Scripts

- `app-start.ps1` / `app-start.bat` - preflight checks and start `application-starter` in foreground
- `app-start-background.ps1` / `app-start-background.bat` - start `application-starter` in background
- `docker-up.ps1` / `docker-up.bat` - start external services and wait for health checks
- `docker-down.ps1` / `docker-down.bat` - stop external services
- `docker-reset.ps1` / `docker-reset.bat` - stop external services and remove volumes
- `docker-logs.ps1` / `docker-logs.bat` - follow external service logs

## Notes

- These scripts expect to be executed from the repository.
- Runtime startup reads application settings from `config/application.yaml`; MySQL / Redis / MinIO should be reachable for the default local profile.
