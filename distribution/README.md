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

## Launch commands

### Foreground startup

Unix-like:

```bash
bash distribution/target/full-distribution/full-distribution/bin/start.sh \
  --cp.chat.auth.jwt.secret=YOUR_SECRET \
  --cp.chat.server.id=YOUR_SERVER_ID
```

Windows:

```bat
distribution\target\full-distribution\full-distribution\bin\start.bat --cp.chat.auth.jwt.secret=YOUR_SECRET --cp.chat.server.id=YOUR_SERVER_ID
```

### Background startup

Unix-like:

```bash
bash distribution/target/full-distribution/full-distribution/bin/start-background.sh \
  --cp.chat.auth.jwt.secret=YOUR_SECRET \
  --cp.chat.server.id=YOUR_SERVER_ID
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

## Notes

- The thin jar launch path is the primary distribution mode.
- The application still requires valid runtime configuration and, in normal service mode, reachable external dependencies such as MySQL, Redis, and MinIO.
