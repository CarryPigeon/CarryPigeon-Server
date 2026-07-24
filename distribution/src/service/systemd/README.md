# systemd Example

This directory contains a minimal `systemd` unit example for non-containerized Linux deployment.

## Intended layout

Place the distribution package under a stable directory such as:

```text
/opt/carrypigeon/
```

Expected runtime files:

- `/opt/carrypigeon/app/`
- `/opt/carrypigeon/lib/` and `/opt/carrypigeon/plugins/`
- `/opt/carrypigeon/config/`
- `/opt/carrypigeon/bin/`

## Installation steps

1. Create a dedicated service account, for example `carrypigeon`.
2. Extract the distribution package to `/opt/carrypigeon`.
3. Edit `config/application.yaml` and fill required values such as `cp.chat.auth.jwt.secret`.
4. Run `bin/verify.sh --strict-config` under the package directory.
5. Copy `carrypigeon.service` to `/etc/systemd/system/`.
6. Adjust `User`, `Group`, `WorkingDirectory`, `ExecStart`, and `ExecStop` if your install path is not `/opt/carrypigeon`.
7. Reload and enable the service:

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now carrypigeon.service
```

## Notes

- The packaged `start.sh` reads runtime configuration from `config/application.yaml`.
- `ExecStop` relies on the distribution PID file under `run/application.pid`.
- If you prefer foreground logging into `journalctl`, keep `start.sh` as the entrypoint. If you prefer a custom file-based log directory, set `CP_LOG_HOME` as a systemd environment override.
