# Runtime plugins

Place reviewed startup plugins directly in this directory. Each plugin JAR must contain
`META-INF/carrypigeon/plugin.yaml` and Spring Boot AutoConfiguration metadata.

Run `../bin/verify.sh` (or `../bin/verify.ps1`) after adding or replacing a plugin. Verification rejects entrypoints
that are not packaged by their declaring JAR, missing Boot imports, missing exact host artifacts, duplicate classes,
forbidden bundled host/shared classes, and conflicting top-level Maven artifact versions.

Files below `disabled/` are not included by the launcher wildcard. The launcher `--safe-mode`
also omits the entire `plugins/*` classpath before starting the JVM.
