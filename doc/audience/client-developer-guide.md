# 客户端开发者文档（HTTP API + WebSocket）

> 本文是客户端对接入口。对外协议以 `doc/api/*` 为唯一事实来源（SSOT）。

## 1. 快速开始（推荐顺序）

1. 调用 `GET /api/server` 获取服务端信息与能力声明。
2. 若服务端启用 required gate，调用 `POST /api/gates/required/check`。
3. 登录获取 token：`POST /api/auth/tokens`。
4. 建立 WebSocket：`wss://{host}/api/ws`，发送 `auth`。
5. 收到事件后按 `event_type` 分发处理，并记录 `event_id` 用于断线恢复。

## 2. 你必须遵守的协议约定

- HTTP Base：`/api`
- WebSocket URL：`/api/ws`
- JSON 字段：`snake_case`
- 实体 ID（`uid/cid/mid/event_id`）：十进制字符串
- 鉴权：`Authorization: Bearer <access_token>`
- 错误处理：以 `error.reason` 为主分支条件，`status` 仅做兜底

## 3. 文档分工（避免重复）

- 协议总览：`doc/api/10-HTTP+WebSocket协议.md`
- HTTP 端点：`doc/api/11-HTTP端点清单.md`
- WebSocket 事件：`doc/api/12-WebSocket事件清单.md`
- 错误模型：`doc/api/13-错误模型与Reason枚举.md`
- 分页与游标：`doc/api/14-分页与游标规范.md`
- 插件目录与合约：`doc/api/15-插件包扫描与Manifest规范.md`

## 4. 常见实现建议

- 把 HTTP 请求层与 WS 事件层分离，避免互相耦合。
- 本地维护 `last_event_id`，重连时通过 `resume` 申请补发。
- 收到 `resume.failed` 后立即走 HTTP 补拉（频道、消息、未读）。
- 对 `429 rate_limited` 实现退避重试，优先读取 `retry_after_ms`。

## 5. 兼容性声明

- 当前对外协议仅保证 `doc/api/*` 所述的 HTTP + WebSocket 规范。
- 旧 TCP/Netty 对外路由不再作为客户端接入标准。
