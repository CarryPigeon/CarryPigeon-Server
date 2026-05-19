# mc-bridge Example

`mc-bridge` is a scenario-specific extension message example.

This directory exists to keep example extension code out of the production `chat-domain` runtime code.

## Directory contents

- `McBridgeMessageTypePluginConfiguration.java`
- `McBridgeMessagePlugin.java`
- `EXPLANATION.md`

## Boundary

- This directory is for example code only.
- Production runtime should keep only generic extension support.
- Concrete scenario extensions should live here or in downstream dedicated modules, not in the main backend runtime code.
