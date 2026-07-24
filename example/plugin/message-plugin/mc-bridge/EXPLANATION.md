# mc-bridge Explanation

## Why this example lives here

The main backend runtime should not carry scenario-specific sample code.

`mc-bridge` is a concrete example of how to build an extension message type on top of the production extension infrastructure, so it is stored under the root `example/` folder instead of `chat-domain/src/main/java`.

## What this sample demonstrates

1. how to define a concrete extension `messageType`
2. how to register that type with a dedicated configuration class
3. how to add extension-specific payload validation through `validateCanonicalData(...)`
4. how to still reuse the generic extension message handler base

## Minimal payload contract

```json
{
  "plugin_key": "mc-bridge",
  "payload": {
    "event": "player_join"
  },
  "text": "A player joined"
}
```

The generic extension plugin validates `plugin_key`, `payload`, optional `text` and optional `metadata`. The example then requires `payload.event` to be a non-blank string and writes its trimmed value into canonical data.

## Relationship to production code

The production `plugin` feature keeps:

- unified message sending
- extension whitelist control
- extension registration support
- generic extension message handling

Concrete scenario examples like `mc-bridge` should be kept in `example/` or downstream modules.
