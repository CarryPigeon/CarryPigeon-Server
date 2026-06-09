# systemd Example

This directory contains a minimal `systemd` unit example for non-containerized Linux deployment.

## Intended layout

Place the distribution package under a stable directory such as:

```text
/opt/carrypigeon/
```

Expected runtime files:

- `/opt/carrypigeon/app/`
- `/opt/carrypigeon/libs/`
- `/opt/carrypigeon/config/`
- `/opt/carrypigeon/bin/`
- `/opt/carrypigeon/.env`

## Installation steps

1. Create a dedicated service account, for example `carrypigeon`.
2. Extract the distribution package to `/opt/carrypigeon`.
3. Copy `.env.example` to `.env` and fill required values.
4. Run `bin/verify.sh --strict-env` under the package directory.
5. Copy `carrypigeon.service` to `/etc/systemd/system/`.
6. Adjust `User`, `Group`, `WorkingDirectory`, `EnvironmentFile`, `ExecStart`, and `ExecStop` if your install path is not `/opt/carrypigeon`.
7. Reload and enable the service:

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now carrypigeon.service
```

## Notes

- The packaged `start.sh` already performs dependency and configuration preflight checks.
- `ExecStop` relies on the distribution PID file under `run/application.pid`.
- If you prefer foreground logging into `journalctl`, keep `start.sh` as the entrypoint. If you prefer file-based logs, set `CP_LOG_HOME` in `.env`.
