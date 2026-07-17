# Linux Scripts

This directory contains the Linux / macOS startup and dependency scripts.

## Scripts

- `app-start.sh` - preflight checks and start `application-starter` in foreground
- `app-start-background.sh` - start `application-starter` in background
- `docker-up.sh` - start external services and wait for health checks
- `docker-down.sh` - stop external services
- `docker-reset.sh` - stop external services and remove volumes
- `docker-logs.sh` - follow external service logs
- `dist-package.sh` - build the thin-jar distribution package
- `dist-start.sh` - start the packaged distribution in foreground
- `dist-start-background.sh` - start the packaged distribution in background
- `dist-stop.sh` - stop the packaged distribution

## Notes

- These scripts expect to be executed from the repository.
- Runtime startup reads application settings from `config/application.yaml`; MySQL / Redis / MinIO should be reachable for the default local profile.
