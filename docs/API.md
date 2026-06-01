# CarryPigeon Backend API

本文基于当前仓库实际代码整理，对外接口以控制器、DTO、统一错误模型、当前 WebSocket 实现和运行时 OpenAPI 文档为准。

## 1. 总体约定

### 1.1 成功响应

当前仓库的成功响应**并不完全统一**，存在两类稳定形态：

1. 仍保留 `CPResponse<T>` 成功包装的过渡接口
2. 已切到直接返回资源对象 / `204 No Content` 的 v1 风格接口

当前仍使用 `CPResponse<T>` 的成功示例：

```json
{
  "code": 100,
  "message": "success",
  "data": {}
}
```

当前稳定业务成功码：

| code | 含义 |
| --- | --- |
| `100` | 成功 |

### 1.2 错误响应

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

当前稳定错误 reason 以实际代码和 `docs/t/13-error-model-and-reasons-v1.md` 对齐为准，已落地常见值包括：

- `validation_failed`
- `unauthorized`
- `token_expired`
- `not_found`
- `internal_error`
- `required_plugin_missing`

### 1.3 认证约定

- 受保护 HTTP 接口使用 `Authorization: Bearer <access-token>`
- HTTP 鉴权由 `AuthAccessTokenInterceptor` 处理，保护范围为 `/api/**`
- 当前明确匿名放行的 HTTP 路径：
  - `GET /api/server`
  - `POST /api/gates/required/check`
  - `POST /api/auth/email_codes`
  - `POST /api/auth/tokens`
  - `POST /api/auth/refresh`
  - `POST /api/auth/revoke`
  - `GET /.well-known/carrypigeon-server`

### 1.4 Swagger / OpenAPI

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
  "avatar": "api/files/download/server_avatar",
  "api_version": "1.0",
  "min_supported_api_version": "1.0",
  "ws_url": "wss://127.0.0.1:18080/api/ws",
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

- `ws_url` 由 `cp.chat.server.realtime.host`、`port`、`path` 直接拼装，当前固定使用 `wss://` scheme。
- `capabilities` 当前表示协议面最小公开能力，不表示实时监听端口此刻一定可连接。
- `cp.chat.server.realtime.enabled=false` 只控制 Netty realtime 运行时是否启动，不会抑制 discovery 中的 `ws_url` 或 capability 字段输出。

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

### 2.3 Well-known 服务文档

- **方法**：`GET`
- **路径**：`/.well-known/carrypigeon-server`
- **认证**：否

该接口当前仍保留，作为历史兼容发现补充入口。

### 2.4 插件目录

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

### 2.4 插件目录

- **方法**：`GET`
- **路径**：`/api/plugins/catalog`
- **认证**：否

当前实现会返回公开插件目录与 required_plugins 列表；`provides_domains` 与 `download` 字段当前为最小承接结构。

### 2.5 Domain 目录

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

请求体：

```json
{ "email": "user@example.com" }
```

### 3.2 创建会话并签发 Token

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

### 3.3 刷新 Token

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

### 3.4 撤销 Refresh Token

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

### 3.5 查询当前登录用户（过渡接口）

- **方法**：`GET`
- **路径**：`/api/auth/me`
- **认证**：是

该接口当前仍保留旧成功 envelope，作为过渡期受保护身份查询入口：

```json
{
  "code": 100,
  "message": "success",
  "data": {
    "account_id": "1001",
    "username": "user@example.com"
  }
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

### 4.6 过渡接口

以下旧接口当前仍保留，用于平滑承接现有内部能力：

- `GET /api/users/page`
- `GET /api/users/search`
- `PUT /api/users/me`

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

### 5.4 发现频道

- **方法**：`GET`
- **路径**：`/api/channels/discover`
- **认证**：是

当前 `type` 过滤值直接对齐持久化频道类型，只接受：

- `public`
- `private`
- `system`

### 5.5 获取频道成员

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

### 5.6 设为管理员

- **方法**：`PUT`
- **路径**：`/api/channels/{cid}/admins/{uid}`
- **认证**：是
- **成功**：`204 No Content`

### 5.7 撤销管理员

- **方法**：`DELETE`
- **路径**：`/api/channels/{cid}/admins/{uid}`
- **认证**：是
- **成功**：`204 No Content`

### 5.8 过渡接口

以下旧接口当前仍保留：

- `GET /api/channels/default`
- `GET /api/channels/system`
- `POST /api/channels/private`
- `POST /api/channels/{cid}/invites`
- `POST /api/channels/{cid}/invites/accept`
- `POST /api/channels/{cid}/ownership-transfer`
- `POST /api/channels/{cid}/members/{uid}/mute`
- `DELETE /api/channels/{cid}/members/{uid}/mute`
- `DELETE /api/channels/{cid}/members/{uid}`
- `POST /api/channels/{cid}/bans`
- `DELETE /api/channels/{cid}/bans/{uid}`

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
- `Core:File` 使用 `data.share_key`、`data.filename`
- `Core:Voice` 使用 `data.share_key`、`data.filename`、`data.duration_millis`

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

成功响应当前仍保留过渡成功 envelope：

```json
{
  "code": 100,
  "message": "success",
  "data": {
    "object_key": "channels/1/messages/file/accounts/1001/5001-demo.pdf",
    "filename": "demo.pdf",
    "mime_type": "application/pdf",
    "size": 123
  }
}
```

### 6.6 撤回消息（过渡接口）

- **方法**：`POST`
- **路径**：`/api/channels/{cid}/messages/{mid}/recall`
- **认证**：是

该接口当前仍保留旧成功 envelope，作为过渡能力存在。

## 7. 服务基础接口

### 7.1 当前节点 Presence

- **方法**：`GET`
- **路径**：`/api/server/presence/me`
- **认证**：是

成功响应当前仍保留过渡成功 envelope。

## 8. 实时 WebSocket 接口

当前仓库存在独立于 Spring MVC 的 Netty WebSocket 通道。

- **默认路径**：`/api/ws`
- **配置来源**：`cp.chat.server.realtime.path`
- **默认监听端口**：`18080`
- **默认开关**：`cp.chat.server.realtime.enabled=false`
- **公开发现语义**：即使 realtime 开关关闭，`GET /api/server` 当前仍会返回按配置拼装的 `ws_url`
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

当前 `resume` 为单节点、按账户隔离的内存事件窗口实现：

- 找不到 `last_event_id` 或该锚点已被当前账户窗口淘汰时回写 `resume.failed`
- 事件窗口按认证账户分别维护，不再被其他账户高频事件直接挤占
- 只会回放当前认证账户可见的事件
- 跨实例 / 长窗口 / 持久化回放尚未覆盖

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

基于当前仓库代码，以下能力仍未完全收口或仍为过渡状态：

- 用户旧分页/搜索/旧 `PUT /api/users/me` 过渡接口仍保留
- 频道旧动作式路径仍保留
- 消息 HTTP 发送当前仅最小支持 `Core:Text@1.0.0`
- WS `resume` 仍是单节点内存实现
- discovery 当前不会根据 realtime 开关动态隐藏 `ws_url` 或 capability
- system channel 当前由数据库迁移种子保证存在，但数据库层未建立唯一约束

当前对外接口事实来源仍以源码、测试和运行时 OpenAPI 文档为准。
