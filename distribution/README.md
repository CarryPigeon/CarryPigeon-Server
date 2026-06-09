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
- `.env.example` - minimal runtime environment template

Before first launch, copy `.env.example` to `.env` and fill at least:

- `CP_CHAT_AUTH_JWT_SECRET`
- `CP_CHAT_SERVER_ID`
- external dependency addresses or ports when they are not the local defaults

The distribution launchers validate `CP_CHAT_AUTH_JWT_SECRET` from environment variables or `.env` before startup. Command-line Spring arguments do not replace this preflight check.

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

To verify deployment readiness with a completed `.env`, run:

```bash
bash distribution/target/full-distribution/full-distribution/bin/verify.sh --strict-env
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
- The distribution scripts load `.env` from the package root when present.
- The application still requires valid runtime configuration and, in normal service mode, reachable external dependencies such as MySQL, Redis, and MinIO.
- The package launchers now point Spring to `config/application.yaml` and `config/log4j2-spring.xml`, so the packaged `config/` directory is part of the real runtime path.
- The package now includes a verification entrypoint, a `systemd` example, and a minimal release-artifact workflow, but it still does not replace public checksum publishing, artifact signing, or multi-node orchestration.
- GitHub Actions now includes a dedicated `Distribution Release` workflow that builds the package and uploads the zip, checksum, and manifest as workflow artifacts.
