# 机器人端开发指南（Bot Client）

> Bot 本质是“自动化客户端”。协议与普通客户端一致，差异在运行方式与工程治理。

## 1. 必读文档

- 客户端主文档：`doc/audience/client-developer-guide.md`
- HTTP 端点：`doc/api/11-HTTP端点清单.md`
- WS 事件：`doc/api/12-WebSocket事件清单.md`
- 错误模型：`doc/api/13-错误模型与Reason枚举.md`

## 2. Bot 工程建议

- 独立账号：每个 Bot 使用独立用户，避免与人工账号混用。
- 状态机拆分：连接层（HTTP/WS）与业务层（命令处理）分离。
- 幂等处理：对消息与事件做去重（基于 `mid/event_id`）。
- 断线恢复：优先 `resume`，失败后立即补拉。
- 安全日志：避免记录 token、邮箱、原始敏感 payload。

## 3. 常用接口清单

- 登录：`POST /api/auth/tokens`
- 当前用户：`GET /api/users/me`
- 频道消息：`GET/POST /api/channels/{cid}/messages`
- 未读与读状态：`GET /api/unreads`、`POST /api/channels/{cid}/read_state`

## 4. 上线前检查

- [ ] 正确处理 `401/403/412/422/429`
- [ ] 以 `error.reason` 做业务分支
- [ ] WS 事件消费可重放、可幂等
- [ ] 多实例运行无重复执行风险
