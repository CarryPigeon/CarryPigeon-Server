# CarryPigeon Backend API

本文基于当前仓库实际代码整理，对外接口以控制器、DTO、统一错误模型、当前 WebSocket 实现和运行时 OpenAPI 文档为准。

## 1. 总体约定

### 1.1 成功响应

当前仓库的成功响应以控制器契约为准，主要有两类稳定形态：

1. 直接返回资源对象。
2. 返回 `204 No Content`。

### 1.2 协议与字段约定

- HTTP Base：`/api`
- WebSocket 默认入口：`/api/ws`
- 不使用 URL 路径版本，不采用 `/api/{version}/...` 形式。
- 推荐请求头：`Accept: application/vnd.carrypigeon+json; version=1`
- JSON 字段统一使用 `snake_case`。
- 时间字段统一使用 Unix epoch 毫秒。
- 雪花 ID 在 JSON 中统一编码为十进制字符串，避免 JS / TS 客户端精度丢失。
- API 返回的图片、文件地址必须为相对路径，例如 `avatars/u/1001.png` 或 `/api/files/download/server_avatar`；客户端按当前连接服务器 origin 拼接。

### 1.3 错误响应

HTTP 失败统一返回标准错误对象：

```json
{
  "error": {
    "status": 422,
    "reason": "validation_failed",
    "message": "validation failed",
    "request_id": "req-01HXYZ",
    "details": {}
  }
}
```

当前稳定错误 reason 以实际代码和本文错误响应约定为准，已落地常见值包括：

- `validation_failed`
- `unauthorized`
- `token_expired`
- `not_found`
- `internal_error`
- `mail_service_unavailable`
- `email_delivery_failed`
- `required_plugin_missing`
- `password_login_disabled`

建议映射：

| HTTP | reason |
| --- | --- |
| `400` / `422` | `validation_failed` / `schema_invalid` |
| `401` | `unauthorized` / `token_expired` |
| `403` | `forbidden` / `password_login_disabled` / `not_channel_member` / `channel_admin_required` / `channel_owner_required` / `user_muted` |
| `404` | `not_found` |
| `409` | `conflict` / `application_already_processed` |
| `412` | `required_plugin_missing` |
| `429` | `rate_limited` |
| `503` | `mail_service_unavailable` / `email_delivery_failed` |
| `500` | `internal_error` |

客户端应以 `error.reason` 作为业务分支依据，HTTP status 用于通用兜底与日志分组。

### 1.4 分页与游标

分页接口统一使用：

```json
{
  "items": [],
  "next_cursor": "opaque_string_or_null",
  "has_more": true
}
```

约定：

- `cursor` 为不透明字符串，客户端不得解析。
- `cursor` 只能用于同一个端点的后续分页请求，不得跨端点复用。
- cursor 无效或过期时返回标准错误响应，常见 reason 为 `cursor_invalid` 或 `validation_failed`。
- 消息列表按稳定锚点排序，客户端必须按 `mid` 去重，因为 WebSocket 推送与 HTTP 补拉可能重叠。

### 1.5 认证约定

- 受保护 HTTP 接口使用 `Authorization: Bearer <access-token>`
- HTTP 鉴权由 `AuthAccessTokenInterceptor` 处理，保护范围为 `/api/**`
- 当前明确匿名放行的 HTTP 路径：
  - `GET /api/server`
  - `POST /api/gates/required/check`
  - `POST /api/auth/register`
  - `POST /api/auth/login`
  - `POST /api/auth/email_codes`
  - `POST /api/auth/tokens`
  - `POST /api/auth/refresh`
  - `POST /api/auth/revoke`

### 1.6 Swagger / OpenAPI

- Swagger UI：`/swagger-ui/index.html`
- 兼容入口：`/swagger-ui.html`
- OpenAPI JSON：`/v3/api-docs`
- OpenAPI YAML：`/v3/api-docs.yaml`

## 2. 服务发现与 Gate

### 2.1 获取服务发现文档

- **方法**：`GET`
- **路径**：`/api/server`
- **认证**：否

成功响应示例：

```json
{
  "server_id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "CarryPigeonBackend",
  "brief": "A self-hosted chat server",
  "avatar": "/api/files/download/server_avatar",
  "api_version": "1.0",
  "min_supported_api_version": "1.0",
  "ws_url": "ws://127.0.0.1:18080/api/ws",
  "required_plugins": [],
  "capabilities": {
    "message_domains": true,
    "plugin_catalog": true,
    "event_resume": true
  },
  "server_time": 1700000000000
}
```

当前代码口径补充：

- `ws_url` 由 `cp.chat.server.realtime.host`、`port`、`path` 直接拼装，当前内置 Netty discovery 返回 `ws://` scheme。
- 当前内置 Netty realtime 监听链路未装配 TLS handler；直接连接内置端口时是明文 WebSocket。生产环境要使用真实 WSS，需要在前置网关、反向代理或负载均衡层完成 TLS 终止并转发到内置 realtime 端口，或由外部网关对客户端公开 WSS 地址。
- `capabilities` 当前表示协议面最小公开能力，不表示实时监听端口此刻一定可连接。
- `cp.chat.server.realtime.enabled=false` 会阻止 Netty realtime 运行时启动，并使 discovery 返回 `ws_url=null`、`capabilities.websocket=false`。

### 2.2 required gate 预检查

- **方法**：`POST`
- **路径**：`/api/gates/required/check`
- **认证**：否

请求体示例：

```json
{
  "client": {
    "device_id": "a-stable-device-id",
    "installed_plugins": [
      { "plugin_id": "mc-bind", "version": "1.2.0" }
    ]
  }
}
```

成功响应示例：

```json
{ "missing_plugins": [] }
```

### 2.3 插件目录

- **方法**：`GET`
- **路径**：`/api/plugins/catalog`
- **认证**：否

成功响应示例：

```json
{
  "required_plugins": ["mc-bind"],
  "plugins": [
    {
      "plugin_id": "text",
      "name": "Built-in text channel message plugin",
      "version": "1.0.0",
      "min_host_version": "0.1.0",
      "required": false,
      "permissions": ["message:text:send"],
      "provides_domains": [
        { "domain": "text", "domain_version": "1.0.0" }
      ],
      "download": null
    }
  ]
}
```

说明：

- 当前实现会返回公开插件目录与 `required_plugins` 列表；`provides_domains` 与 `download` 字段当前为最小承接结构。
- 客户端侧如存在 repo catalog 概念，通常是客户端配置的第三方仓库 URL，不属于当前服务端 `/api` 路径；其响应结构建议与 `GET /api/plugins/catalog` 的 `plugins[]` item 保持兼容。

### 2.4 Domain 目录

- **方法**：`GET`
- **路径**：`/api/domains/catalog`
- **认证**：否

成功响应示例：

```json
{
  "items": [
    {
      "domain": "Core:Text",
      "supported_versions": ["1.0.0"],
      "recommended_version": "1.0.0",
      "constraints": {
        "max_payload_bytes": 4096,
        "max_depth": 10
      },
      "providers": [
        { "type": "core", "plugin_id": null, "min_plugin_version": null }
      ]
    }
  ]
}
```

## 3. 鉴权接口

基路径：`/api/auth`

### 3.1 发送邮箱验证码

- **方法**：`POST`
- **路径**：`/api/auth/email_codes`
- **认证**：否
- **成功**：`204 No Content`

说明：

- 该接口当前会触发真实邮件发送。
- 运行前提：需要启用 `cp.infrastructure.service.mail.enabled=true`，并提供有效的 `spring.mail.*` 与 `cp.infrastructure.service.mail.from-address` 配置。
- 若邮件服务未启用或投递失败，接口返回 `503 Service Unavailable`，`error.reason` 分别为 `mail_service_unavailable` 或 `email_delivery_failed`。

请求体：

```json
{ "email": "user@example.com" }
```

### 3.2 用户名密码注册

- **方法**：`POST`
- **路径**：`/api/auth/register`
- **认证**：否

请求体：

```json
{ "username": "carry-user", "password": "password123" }
```

成功响应：

```json
{ "uid": "1001", "username": "carry-user" }
```

### 3.3 用户名密码登录

- **方法**：`POST`
- **路径**：`/api/auth/login`
- **认证**：否

请求体：

```json
{ "username": "carry-user", "password": "password123" }
```

成功响应结构与 `POST /api/auth/tokens` 一致，当前推荐生产环境优先使用该入口完成公开认证闭环。

配置说明：

- 该入口受 `cp.chat.auth.password-login.enabled` 控制。
- 分发包外部使用时可在 `config/application.yaml` 中设置 `cp.chat.auth.password-login.enabled=false` 关闭用户名密码登录。
- 关闭后接口返回 HTTP `403`，`error.reason = "password_login_disabled"`。

### 3.4 创建会话并签发 Token

- **方法**：`POST`
- **路径**：`/api/auth/tokens`
- **认证**：否

请求体示例：

```json
{
  "grant_type": "email_code",
  "email": "user@example.com",
  "code": "123456",
  "client": {
    "device_id": "a-stable-device-id",
    "installed_plugins": []
  }
}
```

成功响应示例：

```json
{
  "token_type": "Bearer",
  "access_token": "xxx",
  "expires_in": 1800,
  "refresh_token": "yyy",
  "uid": "1001",
  "is_new_user": false
}
```

required gate 不满足时返回：

- HTTP `412`
- `error.reason = "required_plugin_missing"`
- `error.details.missing_plugins = [...]`

### 3.5 刷新 Token

- **方法**：`POST`
- **路径**：`/api/auth/refresh`
- **认证**：否

请求体：

```json
{
  "refresh_token": "yyy",
  "client": { "device_id": "a-stable-device-id" }
}
```

成功响应结构与 `POST /api/auth/tokens` 一致。

### 3.6 撤销 Refresh Token

- **方法**：`POST`
- **路径**：`/api/auth/revoke`
- **认证**：否
- **成功**：`204 No Content`

请求体：

```json
{
  "refresh_token": "yyy",
  "client": { "device_id": "a-stable-device-id" }
}
```

## 4. 用户接口

基路径：`/api/users`

### 4.1 获取当前用户

- **方法**：`GET`
- **路径**：`/api/users/me`
- **认证**：是

成功响应：

```json
{
  "uid": "1001",
  "email": "user@example.com",
  "nickname": "carry-user",
  "avatar": "avatars/u/1001.png"
}
```

### 4.2 获取用户公开资料

- **方法**：`GET`
- **路径**：`/api/users/{uid}`
- **认证**：是

成功响应：

```json
{
  "uid": "1001",
  "nickname": "carry-user",
  "avatar": "avatars/u/1001.png"
}
```

### 4.3 批量获取用户公开资料

- **方法**：`GET`
- **路径**：`/api/users?ids=1001,1002`
- **认证**：是

成功响应：

```json
{
  "items": [
    { "uid": "1001", "nickname": "carry-user", "avatar": "avatars/u/1001.png" }
  ]
}
```

### 4.4 更新当前用户邮箱

- **方法**：`PUT`
- **路径**：`/api/users/me/email`
- **认证**：是
- **成功**：`204 No Content`

### 4.5 按 v1 语义更新当前用户资料

- **方法**：`PATCH`
- **路径**：`/api/users/me`
- **认证**：是
- **成功**：`204 No Content`

请求体示例：

```json
{
  "username": "Alice",
  "avatar": "avatars/u/1001.png",
  "brief": "hello",
  "sex": 0,
  "birthday": 0
}
```

### 4.6 更新当前用户背景图

- **方法**：`POST`
- **路径**：`/api/users/me/background`
- **认证**：是
- **Content-Type**：`multipart/form-data`

请求：

- multipart 字段 `background` 为必填。

成功响应示例：

```json
{ "background_url": "/api/files/download/profile_bg_1001" }
```

## 5. 频道接口

基路径：`/api/channels`

### 5.1 获取频道列表

- **方法**：`GET`
- **路径**：`/api/channels`
- **认证**：是

成功响应：

```json
{
  "channels": [
    { "cid": "1", "name": "General", "brief": "", "avatar": "", "owner_uid": "" }
  ]
}
```

### 5.2 获取频道资料

- **方法**：`GET`
- **路径**：`/api/channels/{cid}`
- **认证**：是

成功响应结构与频道列表项一致。

### 5.3 创建频道

- **方法**：`POST`
- **路径**：`/api/channels`
- **认证**：是
- **成功**：`201 Created`

当前内部仍复用 private channel 创建逻辑。

### 5.4 删除频道

- **方法**：`DELETE`
- **路径**：`/api/channels/{cid}`
- **认证**：是
- **成功**：`204 No Content`

删除后，该频道不应再出现在当前用户的频道列表中。

### 5.5 更新频道资料

- **方法**：`PATCH`
- **路径**：`/api/channels/{cid}`
- **认证**：是
- **成功**：`204 No Content`

请求体示例：

```json
{ "name": "New name", "brief": "New brief" }
```

### 5.6 发现频道

- **方法**：`GET`
- **路径**：`/api/channels/discover`
- **认证**：是

当前 `type` 过滤值直接对齐持久化频道类型，只接受：

- `public`
- `private`
- `system`

返回分页结构：`items + next_cursor + has_more`。

### 5.7 获取频道成员

- **方法**：`GET`
- **路径**：`/api/channels/{cid}/members`
- **认证**：是

成功响应：

```json
{
  "items": [
    {
      "uid": "1001",
      "role": "owner",
      "nickname": "carry-owner",
      "avatar": "",
      "join_time": 1700000000000
    }
  ]
}
```

### 5.8 设为管理员

- **方法**：`PUT`
- **路径**：`/api/channels/{cid}/admins/{uid}`
- **认证**：是
- **成功**：`204 No Content`

### 5.9 撤销管理员

- **方法**：`DELETE`
- **路径**：`/api/channels/{cid}/admins/{uid}`
- **认证**：是
- **成功**：`204 No Content`

### 5.10 踢出成员

- **方法**：`DELETE`
- **路径**：`/api/channels/{cid}/members/{uid}`
- **认证**：是
- **成功**：`204 No Content`

### 5.11 申请加入频道

- **方法**：`POST`
- **路径**：`/api/channels/{cid}/applications`
- **认证**：是

请求体示例：

```json
{ "reason": "I want to join" }
```

成功响应示例：

```json
{
  "application_id": "3001",
  "cid": "1",
  "uid": "1002",
  "reason": "I want to join",
  "apply_time": 1700000000000,
  "status": "pending"
}
```

### 5.12 获取入群申请列表

- **方法**：`GET`
- **路径**：`/api/channels/{cid}/applications`
- **认证**：是

成功响应示例：

```json
{
  "items": [
    {
      "application_id": "3001",
      "cid": "1",
      "uid": "1002",
      "reason": "I want to join",
      "apply_time": 1700000000000,
      "status": "pending"
    }
  ]
}
```

### 5.13 审批入群申请

- **方法**：`POST`
- **路径**：`/api/channels/{cid}/applications/{application_id}/decisions`
- **认证**：是

请求体示例：

```json
{ "decision": "approve" }
```

说明：`decision` 当前支持 `approve` / `reject`。

### 5.14 禁言频道成员

- **方法**：`PUT`
- **路径**：`/api/channels/{cid}/bans/{uid}`
- **认证**：是

请求体示例：

```json
{ "reason": "spam", "until": 1700003600000 }
```

成功响应示例：

```json
{
  "cid": "1",
  "uid": "1002",
  "expires_at": 1700003600000,
  "reason": "spam",
  "created_at": 1700000000000
}
```

### 5.15 解除频道成员禁言

- **方法**：`DELETE`
- **路径**：`/api/channels/{cid}/bans/{uid}`
- **认证**：是
- **成功**：`204 No Content`

### 5.16 获取频道禁言列表

- **方法**：`GET`
- **路径**：`/api/channels/{cid}/bans`
- **认证**：是

成功响应示例：

```json
{ "items": [] }
```

### 5.17 更新频道通知偏好

- **方法**：`PUT`
- **路径**：`/api/channels/{cid}/notification_preference`
- **认证**：是
- **成功**：`204 No Content`

请求体示例：

```json
{ "mode": "inherit", "muted_until": null }
```

## 6. 消息接口

基路径：`/api/channels` 与 `/api/messages`

### 6.1 拉取频道消息

- **方法**：`GET`
- **路径**：`/api/channels/{cid}/messages`
- **认证**：是

成功响应：

```json
{
  "items": [
    {
      "mid": "5001",
      "cid": "1",
      "uid": "1001",
      "sender": { "uid": "1001", "nickname": "carry-user", "avatar": "avatars/u/1001.png" },
      "send_time": 1700000000000,
      "domain": "Core:Text",
      "domain_version": "1.0.0",
      "data": { "text": "hello world" },
      "preview": "hello world"
    }
  ],
  "next_cursor": "5001",
  "has_more": true
}
```

### 6.2 搜索频道消息

- **方法**：`GET`
- **路径**：`/api/channels/{cid}/messages/search`
- **认证**：是

查询参数：

| 参数 | 类型 | 约束 |
| --- | --- | --- |
| `q` | `string` | 推荐使用；搜索关键字 |
| `keyword` | `string` | 过渡兼容参数；当 `q` 缺失时回退使用 |
| `cursor` | `string / null` | 当前已接受，但仍为过渡语义 |
| `sender_uid` | `string / null` | 当前已接受，但尚未生效 |
| `domain` | `string / null` | 当前已接受，但尚未生效 |
| `before_mid` | `string / null` | 当前已接受，但尚未生效 |
| `after_mid` | `string / null` | 当前已接受，但尚未生效 |
| `limit` | `int` | 默认 `20`，范围 `1..100` |

当前实现返回 `items + next_cursor + has_more`；`next_cursor` 当前按最后一条命中消息的 `mid` 派生，尚不是完全不透明游标。

### 6.3 发送消息

- **方法**：`POST`
- **路径**：`/api/channels/{cid}/messages`
- **认证**：是
- **成功**：`201 Created`

当前最小实现只支持：

- `domain = Core:Text`
- `domain = Core:File`
- `domain = Core:Voice`
- `domain_version = 1.0.0`
- `Core:Text` 使用 `data.text`
- `Core:File` 使用 `data.object_key` 或附件 `data.share_key`，并要求 `data.filename`
- `Core:Voice` 使用 `data.object_key` 或附件 `data.share_key`，并要求 `data.filename`、`data.duration_millis`

### 6.4 删除消息

- **方法**：`DELETE`
- **路径**：`/api/messages/{mid}`
- **认证**：是
- **成功**：`204 No Content`

### 6.5 上传消息附件

- **方法**：`POST`
- **路径**：`/api/channels/{cid}/messages/attachments`
- **认证**：是
- **Content-Type**：`multipart/form-data`

成功响应直接返回附件上传结果：

```json
{
  "object_key": "channels/1/messages/file/accounts/1001/5001-demo.pdf",
  "share_key": "shr_att_xxx",
  "filename": "demo.pdf",
  "mime_type": "application/pdf",
  "size": 123
}
```

说明：

- multipart 字段 `file` 为必填。
- `message_type` 可选，允许值为 `file` 或 `voice`，默认 `file`。

### 6.6 撤回消息

- **方法**：`POST`
- **路径**：`/api/channels/{cid}/messages/{mid}/recall`
- **认证**：是

成功响应返回撤回后的 v1 消息对象；撤回后消息正文按撤回语义脱敏，并通过实时通道推送 `message.recalled` 事件。

### 6.7 编辑消息

- **方法**：`PATCH`
- **路径**：`/api/messages/{mid}`
- **认证**：是

请求体示例：

```json
{
  "domain": "Core:Text",
  "domain_version": "1.0.0",
  "data": { "text": "edited text" },
  "mentions": [],
  "expected_edit_version": 1
}
```

成功响应返回编辑后的 v1 消息对象。

### 6.8 转发消息

- **方法**：`POST`
- **路径**：`/api/messages/{mid}/forward`
- **认证**：是

请求体示例：

```json
{ "target_cid": "2", "comment": "please check" }
```

成功响应返回新创建的转发消息对象。

### 6.9 置顶频道消息

- **方法**：`POST`
- **路径**：`/api/channels/{cid}/pins/{mid}`
- **认证**：是

请求体可选：

```json
{ "note": "important" }
```

成功响应示例：

```json
{
  "cid": "1",
  "mid": "5001",
  "pinned_by_uid": "1001",
  "pinned_at": 1700000000000,
  "note": "important"
}
```

### 6.10 取消置顶频道消息

- **方法**：`DELETE`
- **路径**：`/api/channels/{cid}/pins/{mid}`
- **认证**：是
- **成功**：`204 No Content`

### 6.11 获取频道置顶列表

- **方法**：`GET`
- **路径**：`/api/channels/{cid}/pins`
- **认证**：是

查询参数：

| 参数 | 类型 | 约束 |
| --- | --- | --- |
| `cursor` | `string / null` | 不透明游标 |
| `limit` | `int` | 默认 `20`，范围 `1..50` |

成功响应为分页结构：`items + next_cursor + has_more`。

### 6.12 获取提及收件箱

- **方法**：`GET`
- **路径**：`/api/mentions`
- **认证**：是

查询参数：

| 参数 | 类型 | 约束 |
| --- | --- | --- |
| `cursor` | `string / null` | 不透明游标 |
| `limit` | `int` | 默认 `20`，范围 `1..50` |
| `unread_only` | `boolean` | 默认 `false` |
| `cid` | `string / null` | 可选频道 ID |

成功响应为分页结构：`items + next_cursor + has_more`。

### 6.13 标记单条提及已读

- **方法**：`PUT`
- **路径**：`/api/mentions/{mention_id}/read`
- **认证**：是
- **成功**：`204 No Content`

### 6.14 批量标记提及已读

- **方法**：`PUT`
- **路径**：`/api/mentions/read_state`
- **认证**：是
- **成功**：`204 No Content`

请求体可选：

```json
{ "before_mention_id": "7001", "cid": "1" }
```

## 7. 读状态、文件、通知与审计

### 7.1 更新频道已读状态

- **方法**：`PUT`
- **路径**：`/api/channels/{cid}/read_state`
- **认证**：是

请求体示例：

```json
{ "last_read_mid": "5001", "last_read_time": 1700000000000 }
```

成功响应示例：

```json
{ "cid": "1", "uid": "1001", "last_read_mid": "5001", "last_read_time": 1700000000000 }
```

### 7.2 获取当前用户未读聚合

- **方法**：`GET`
- **路径**：`/api/unreads`
- **认证**：是

成功响应示例：

```json
{
  "items": [
    { "cid": "1", "unread_count": 3, "last_read_time": 1700000000000 }
  ]
}
```

### 7.3 申请文件上传

- **方法**：`POST`
- **路径**：`/api/files/uploads`
- **认证**：是

请求体示例：

```json
{ "filename": "demo.pdf", "mime_type": "application/pdf", "size_bytes": 123 }
```

成功响应示例：

```json
{
  "file_id": "9001",
  "share_key": "shr_xxx",
  "upload": {
    "method": "PUT",
    "url": "/api/files/uploads/shr_xxx",
    "headers": {},
    "expires_at": 1700000300000
  }
}
```

### 7.4 写入文件内容

- **方法**：`PUT`
- **路径**：`/api/files/uploads/{share_key}`
- **认证**：是
- **Content-Type**：任意文件 MIME
- **成功**：`204 No Content`

### 7.5 下载文件

- **方法**：`GET`
- **路径**：`/api/files/download/{share_key}`
- **认证**：普通文件需要登录；`server_avatar` 保留值允许匿名访问。

成功响应：

- 对象内容可直接读取时返回 `200` 与二进制内容。
- 对象服务提供预签名 URL 时返回 `302`，`Location` 指向对象下载地址。
- 文件不存在返回 `404 not_found`。

### 7.6 查询通知偏好

- **方法**：`GET`
- **路径**：`/api/notification_preferences`
- **认证**：是

成功响应示例：

```json
{
  "server": { "mode": "all", "muted_until": null },
  "channels": [
    { "cid": "1", "mode": "inherit", "muted_until": null }
  ]
}
```

### 7.7 更新服务级通知偏好

- **方法**：`PUT`
- **路径**：`/api/notification_preferences/server`
- **认证**：是
- **成功**：`204 No Content`

请求体示例：

```json
{ "mode": "all", "muted_until": null }
```

### 7.8 查询审计日志

- **方法**：`GET`
- **路径**：`/api/audit_logs`
- **认证**：是

查询参数：

| 参数 | 类型 | 约束 |
| --- | --- | --- |
| `cursor` | `string / null` | 不透明游标 |
| `limit` | `int` | 默认 `50` |
| `cid` | `string / null` | 可选频道 ID |
| `actor_uid` | `string / null` | 可选操作者 UID |
| `action` | `string / null` | 可选审计动作 |
| `from_time` | `long / null` | 可选起始时间 |
| `to_time` | `long / null` | 可选结束时间 |

成功响应为分页结构：`items + next_cursor + has_more`。

## 8. 实时 WebSocket 接口

当前仓库存在独立于 Spring MVC 的 Netty WebSocket 通道。

- **默认路径**：`/api/ws`
- **配置来源**：`cp.chat.server.realtime.path`
- **默认监听端口**：`18080`
- **默认开关**：`cp.chat.server.realtime.enabled=true`
- **公开发现语义**：realtime 开关关闭时，`GET /api/server` 当前返回 `ws_url=null`，并将 `capabilities.websocket=false`
- **广播退化语义**：当 realtime 未装配时，消息与频道 realtime 发布器退化为空实现，主业务链路继续运行但不会产生实际推送

### 8.1 连接与认证

- 握手阶段当前只准备请求上下文
- 鉴权改为首帧 `auth`
- 刷新当前会话令牌使用 `reauth`

首帧 `auth` 示例：

```json
{
  "type": "auth",
  "id": "1",
  "data": {
    "access_token": "xxx",
    "device_id": "device-1",
    "resume": { "last_event_id": "9001" }
  }
}
```

成功响应示例：

```json
{
  "type": "auth.ok",
  "id": "1",
  "data": {
    "uid": "1001",
    "expires_at": 1700000300000,
    "server_id": "550e8400-e29b-41d4-a716-446655440000"
  },
  "error": null
}
```

### 8.2 心跳

- 客户端发送：`{"type":"ping"}`
- 服务端回写：`{"type":"pong"}`

### 8.3 事件 envelope

服务端推送统一使用：

```json
{
  "type": "event",
  "id": null,
  "data": {
    "event_id": "9001",
    "event_type": "message.created",
    "server_time": 1700000000000,
    "payload": {}
  },
  "error": null
}
```

当前已落地最小事件类型：

- `message.created`
- `message.deleted`
- `message.updated`
- `message.pinned`
- `message.unpinned`
- `mention.created`
- `channel.changed`
- `channels.changed`
- `read_state.updated`

事件 payload 摘要：

| event_type | payload |
| --- | --- |
| `channels.changed` | `{ "hint": "refresh" }` |
| `message.created` | `{ "cid": "...", "message": { ...v1 消息对象... } }` |
| `message.deleted` | `{ "cid": "...", "mid": "...", "delete_time": 1700000000000 }` |
| `message.recalled` | `{ "cid": "...", "mid": "...", "recall_time": 1700000000000 }` |
| `message.updated` | `{ "cid": "...", "message": { ...v1 消息对象... } }` |
| `message.pinned` / `message.unpinned` | `{ "cid": "...", "mid": "..." }` |
| `mention.created` | `{ "mention": { ...提及对象... } }` |
| `read_state.updated` | `{ "cid": "...", "uid": "...", "last_read_mid": "...", "last_read_time": 1700000000000 }` |
| `channel.changed` | `{ "cid": "...", "scope": "members", "hint": "refresh" }` |

`channel.changed.scope` 建议值：`profile`、`members`、`applications`、`bans`、`messages`。

当前 `resume` 为单节点、按账户隔离的内存事件窗口实现：

- 找不到 `last_event_id` 或该锚点已被当前账户窗口淘汰时回写 `resume.failed`
- 事件窗口按认证账户分别维护，不再被其他账户高频事件直接挤占
- 只会回放当前认证账户可见的事件
- 跨实例 / 长窗口 / 持久化回放尚未覆盖
- 客户端收到 `resume.failed` 后，应通过 HTTP 补拉频道列表、最近消息、未读聚合等状态。

### 8.4 历史兼容说明

当前仍保留对旧 `send_channel_message` 入站命令的最小兼容承接，便于现有业务链路继续运行。

当前兼容命令会直接落到消息应用服务，已支持：

- `message_type = text`
- `message_type = file`
- `message_type = voice`
- `message_type = custom`
- 已注册扩展消息类型（按 `message_type` 透传为 plugin-style draft）

`message_type = system` 当前不会通过该 WS 命令对外开放。

## 9. 当前未实现 / 未完全收口项

基于当前仓库代码，以下能力仍未完全收口：

- 消息 HTTP 发送当前内置支持 `Core:Text`、`Core:File`、`Core:Voice` 的 `1.0.0` 最小语义，其他插件 domain 仍按注册表与 payload 约束逐步扩展。
- WS `resume` 仍是单节点内存实现
- discovery 当前会根据 realtime 开关输出 `ws_url` 与 `capabilities.websocket`
- system channel 当前由数据库迁移种子保证存在，但数据库层未建立唯一约束

当前对外接口事实来源仍以源码、测试和运行时 OpenAPI 文档为准。
