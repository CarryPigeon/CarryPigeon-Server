# mc-bridge Explanation

## Why this example lives here

The main backend runtime should not carry scenario-specific sample code.

`mc-bridge` is a concrete example of how to build an extension message type on top of the production extension infrastructure, so it is stored under the root `example/` folder instead of `chat-domain/src/main/java`.

## What this sample demonstrates

1. how to define a concrete extension `messageType`
2. how to register that type with a dedicated configuration class
3. how to add extension-specific payload validation
4. how to still reuse the generic extension message handler base

## Minimal payload contract

```json
{
  "event": "player_join"
}
```

The example requires `event` to exist and be a non-blank string.

## Relationship to production code

Production code should keep only:

- unified message sending
- extension whitelist control
- extension registration support
- generic extension message handling

Concrete scenario examples like `mc-bridge` should be kept in `example/` or downstream modules.
