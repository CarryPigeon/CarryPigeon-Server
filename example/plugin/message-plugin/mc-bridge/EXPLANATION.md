# mc-bridge Explanation

## Why this example lives here

The main backend runtime should not carry scenario-specific sample code.

`mc-bridge` is a concrete example of how to build an extension message type on top of the production extension infrastructure, so it is stored under the root `example/` folder instead of `chat-domain/src/main/java`.

## What this sample demonstrates

1. how to define a concrete message domain `Example:McBridge:V1`
2. how to register a `CPChannelMessageHandler` bean from an external plugin
3. how to add extension-specific payload validation in `handle(...)`
4. how to return normalized canonical data without touching host persistence

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

The example requires `payload.event` to be a non-blank string, writes its trimmed value into canonical data, and uses `text` as the message preview when present.

## Relationship to production code

The production message feature keeps:

- unified message sending
- message domain dispatch
- payload validation and normalization inside handlers
- persistence and realtime delivery outside handlers

Concrete scenario examples like `mc-bridge` should be kept in `example/` or downstream modules.
