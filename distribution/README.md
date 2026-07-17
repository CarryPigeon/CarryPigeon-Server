# Distribution Module

This module assembles the runtime delivery package in **thin jar + libs** mode.

## Output layout

After:

```bash
mvn -pl distribution -am package
```

the package is created under:

```text
distribution/target/full-distribution/full-distribution/
```

with the following structure:

- `app/` - application thin jar
- `libs/` - internal module jars and third-party dependencies
- `config/` - runtime configuration files
- `bin/` - startup and shutdown scripts
- `service/` - service-manager examples for non-containerized deployment

Before first launch, edit `config/application.yaml` and fill at least:

- `cp.chat.auth.jwt.secret`
- `cp.chat.server.id`
- external dependency addresses or ports when they are not the local defaults, such as `spring.datasource.url`, `spring.data.redis.*`, and `cp.infrastructure.service.storage.*`

Structured runtime switches are configured through `config/application.yaml`, including:

```yaml
cp:
  chat:
    auth:
      password-login:
        enabled: false
```

This disables `POST /api/auth/login` while keeping email-code token creation, refresh, revoke, and Bearer authentication available.

Future plugin configuration should use the same external YAML file with an isolated namespace, for example:

```yaml
cp:
  plugin:
    configs:
      plugin-id:
        enabled: true
        options:
          api-key: ""
```

Only add plugin entries once the corresponding plugin implementation reads them. Secrets should be supplied by editing the deployment YAML or by a deployment secret manager that materializes YAML before startup.

The distribution launchers no longer load application settings from `.env`; Spring reads `config/application.yaml` as the runtime configuration source.

## Release artifacts

To generate the release-side checksum and manifest after packaging:

```bash
bash bin/linux/dist-release-bundle.sh
```

This generates:

- `distribution/target/release/full-distribution.sha256`
- `distribution/target/release/full-distribution-manifest.json`

The checksum file is intended for artifact integrity verification, and the manifest records version, commit, timestamp, packaged thin jar, and verification commands.

## Verify before launch

Unix-like:

```bash
bash distribution/target/full-distribution/full-distribution/bin/verify.sh
```

Windows:

```bat
distribution\target\full-distribution\full-distribution\bin\verify.bat
```

To verify deployment readiness with required YAML values filled, run:

```bash
bash distribution/target/full-distribution/full-distribution/bin/verify.sh --strict-config
```

## Launch commands

### Foreground startup

Unix-like:

```bash
bash distribution/target/full-distribution/full-distribution/bin/start.sh
```

Windows:

```bat
distribution\target\full-distribution\full-distribution\bin\start.bat
```

### Background startup

Unix-like:

```bash
bash distribution/target/full-distribution/full-distribution/bin/start-background.sh
```

Windows:

```bat
distribution\target\full-distribution\full-distribution\bin\start-background.bat
```

Background startup writes:

- PID file: `run/application.pid`
- stdout log: `service-logs/application-stdout.log` (unless `CP_LOG_HOME` overrides it)

When using the repository-level wrapper `bash bin/linux/dist-start-background.sh`, these files are located under:

- `distribution/target/full-distribution/full-distribution/run/application.pid`
- `distribution/target/full-distribution/full-distribution/service-logs/application-stdout.log`

### Stop command

Unix-like:

```bash
bash distribution/target/full-distribution/full-distribution/bin/stop.sh
```

Windows:

```bat
distribution\target\full-distribution\full-distribution\bin\stop.bat
```

## Service manager example

For non-containerized Linux production-style deployment, the package includes:

- `service/systemd/carrypigeon.service`
- `service/systemd/README.md`

The unit file is only a template. Adjust install paths, user, and group before enabling it on the target host.

## Notes

- The thin jar launch path is the primary distribution mode.
- Application runtime configuration lives in `config/application.yaml`.
- The application still requires valid runtime configuration and, in normal service mode, reachable external dependencies such as MySQL, Redis, and MinIO.
- The package launchers now point Spring to `config/application.yaml` and `config/log4j2-spring.xml`, so the packaged `config/` directory is part of the real runtime path. The packaged `config/application.yaml` is an external override file; development defaults remain inside the application jar.
- The package now includes a verification entrypoint, a `systemd` example, and a minimal release-artifact workflow, but it still does not replace public checksum publishing, artifact signing, or multi-node orchestration.
- GitHub Actions now includes a dedicated `Distribution Release` workflow that builds the package and uploads the zip, checksum, and manifest as workflow artifacts.
