# WebSocket 推送样例（`/api/ws`）

> 本文仅保留常用事件示例。完整协议以 `doc/api/12-WebSocket事件清单.md` 为准。

## 1. 统一事件 envelope

```json
{
  "type": "event",
  "data": {
    "event_id": "723155640365318144",
    "event_type": "message.created",
    "server_time": 1700000000000,
    "payload": {}
  }
}
```

## 2. 新消息（`message.created`）

```json
{
  "type": "event",
  "data": {
    "event_id": "723155640365318145",
    "event_type": "message.created",
    "server_time": 1700000000001,
    "payload": {
      "cid": "12345",
      "message": {
        "mid": "1",
        "cid": "12345",
        "uid": "67890",
        "send_time": 1700000000000,
        "domain": "Core:Text",
        "domain_version": "1.0.0",
        "data": { "text": "hello" },
        "preview": "hello"
      }
    }
  }
}
```

## 3. 删除消息（`message.deleted`）

```json
{
  "type": "event",
  "data": {
    "event_id": "723155640365318146",
    "event_type": "message.deleted",
    "server_time": 1700000000100,
    "payload": {
      "cid": "12345",
      "mid": "1",
      "delete_time": 1700000000100
    }
  }
}
```

## 4. 读状态更新（`read_state.updated`）

```json
{
  "type": "event",
  "data": {
    "event_id": "723155640365318147",
    "event_type": "read_state.updated",
    "server_time": 1700000000200,
    "payload": {
      "cid": "12345",
      "uid": "67890",
      "last_read_mid": "100",
      "last_read_time": 1700000000200
    }
  }
}
```

## 5. 断线恢复建议

- 客户端持久化最后处理成功的 `event_id`。
- 重连后在 `auth.data.resume.last_event_id` 提交该值。
- 若收到 `resume.failed`，走 HTTP 补拉恢复状态。
