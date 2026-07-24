# mc-bridge Example

`mc-bridge` is a scenario-specific extension message example and a minimal startup-classpath plugin.

This directory exists to keep scenario-specific example code out of the production `chat-domain` runtime code. It consumes the SPI and registration support owned by `chat-domain/features/plugin`.

## Directory contents

- `McBridgeMessageTypePluginConfiguration.java`
- `McBridgeMessagePlugin.java`
- `McBridgePluginAutoConfiguration.java`
- `McBridgeSystemPlugin.java`
- `src/main/resources/META-INF/carrypigeon/plugin.yaml`
- `src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- `EXPLANATION.md`

## Boundary

- This directory is for example code only.
- Production plugin runtime and generic extension support belong to `chat-domain/features/plugin`.
- Concrete scenario extensions should live here or in downstream dedicated modules, not in the main backend runtime code.
- Build the independent plugin JAR after the host artifacts are installed:

  ```bash
  mvn -f example/plugin/message-plugin/mc-bridge/pom.xml package
  ```

  The output is `target/mc-bridge-plugin-1.0.0.jar`. Copy it to the distribution `plugins/` directory and configure
  `cp.plugin.configs.com-example-mc-bridge.enabled=true`. Do not bundle Spring, Jackson, Log4j2 or CarryPigeon host
  classes in the plugin JAR.

- The example Manifest declares `CarryPigeon:chat-domain:1.0.0` in `required_host_artifacts`. The distribution verifier
  checks this exact coordinate before Spring starts. After copying the JAR, run `bin/verify.sh` or `bin/verify.ps1`;
  the command performs plugin preflight without requiring external services to be reachable.
