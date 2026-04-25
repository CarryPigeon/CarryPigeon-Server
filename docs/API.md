# CarryPigeon Backend API

本文基于当前仓库实际代码整理，对外接口以控制器、DTO、统一响应模型和实时通道实现为准。

## 1. 总体约定

### 1.1 统一响应

所有 HTTP 接口统一返回 `CPResponse<T>`：

```json
{
  "code": 100,
  "message": "success",
  "data": {}
}
```

字段说明：

- `code`：稳定业务响应码，不直接等同于 HTTP 状态码
- `message`：面向调用方的稳定消息
- `data`：业务数据；失败时通常为 `null`

当前稳定业务响应码：

| code | 含义 |
| --- | --- |
| `100` | 成功 |
| `200` | 参数校验失败 / 请求体不合法 |
| `300` | 无权限 / 认证失败 |
| `404` | 资源不存在 |
| `500` | 服务内部错误 |

### 1.2 认证约定

- 受保护 HTTP 接口使用请求头 `Authorization: Bearer <access-token>`
- HTTP 鉴权由 `AuthAccessTokenInterceptor` 处理，保护范围为 `/api/**`
- 当前明确放行的匿名 HTTP 接口：
  - `POST /api/auth/register`
  - `POST /api/auth/login`
  - `POST /api/auth/refresh`
  - `POST /api/auth/logout`
  - `/api/server/**`
  - `GET /.well-known/carrypigeon-server`

### 1.3 错误映射

全局异常由 `GlobalExceptionHandler` 统一转换为 `CPResponse`：

- 参数绑定失败、Bean Validation 失败、JSON 解析失败：返回 `code=200`
- 业务 `FORBIDDEN`：返回 `code=300`
- 业务 `NOT_FOUND`：返回 `code=404`
- 未捕获异常：返回 `code=500`，消息固定为 `internal server error`

## 2. 鉴权接口

基路径：`/api/auth`

### 2.1 注册

- **方法**：`POST`
- **路径**：`/api/auth/register`
- **认证**：否

请求体：

| 字段 | 类型 | 约束 |
| --- | --- | --- |
| `username` | `string` | 必填，长度 `3..32` |
| `password` | `string` | 必填，长度 `8..128` |

成功响应 `data`：

| 字段 | 类型 |
| --- | --- |
| `accountId` | `long` |
| `username` | `string` |

### 2.2 登录

- **方法**：`POST`
- **路径**：`/api/auth/login`
- **认证**：否

请求体：

| 字段 | 类型 | 约束 |
| --- | --- | --- |
| `username` | `string` | 必填 |
| `password` | `string` | 必填 |

成功响应 `data`：

| 字段 | 类型 |
| --- | --- |
| `accountId` | `long` |
| `username` | `string` |
| `accessToken` | `string` |
| `accessTokenExpiresAt` | `instant` |
| `refreshToken` | `string` |
| `refreshTokenExpiresAt` | `instant` |

### 2.3 刷新令牌

- **方法**：`POST`
- **路径**：`/api/auth/refresh`
- **认证**：否

请求体：

| 字段 | 类型 | 约束 |
| --- | --- | --- |
| `refreshToken` | `string` | 必填 |

成功响应结构与“登录”一致。

### 2.4 注销

- **方法**：`POST`
- **路径**：`/api/auth/logout`
- **认证**：否

请求体：

| 字段 | 类型 | 约束 |
| --- | --- | --- |
| `refreshToken` | `string` | 必填 |

成功响应：`data = null`

### 2.5 查询当前登录用户

- **方法**：`GET`
- **路径**：`/api/auth/me`
- **认证**：是

成功响应 `data`：

| 字段 | 类型 |
| --- | --- |
| `accountId` | `long` |
| `username` | `string` |

## 3. 用户资料接口

基路径：`/api/users`

### 3.1 查询当前用户资料

- **方法**：`GET`
- **路径**：`/api/users/me`
- **认证**：是

成功响应 `data`：

| 字段 | 类型 |
| --- | --- |
| `accountId` | `long` |
| `nickname` | `string` |
| `avatarUrl` | `string` |
| `bio` | `string` |
| `createdAt` | `instant` |
| `updatedAt` | `instant` |

### 3.2 更新当前用户资料

- **方法**：`PUT`
- **路径**：`/api/users/me`
- **认证**：是

请求体：

| 字段 | 类型 | 约束 |
| --- | --- | --- |
| `nickname` | `string` | 必填，最大长度 `64` |
| `avatarUrl` | `string` | 必填，可为空串，最大长度 `512` |
| `bio` | `string` | 必填，可为空串，最大长度 `1024` |

成功响应结构与“查询当前用户资料”一致。

## 4. 频道接口

基路径：`/api/channels`

### 4.1 查询默认频道

- `GET /api/channels/default`
- 认证：是

### 4.2 查询系统频道

- `GET /api/channels/system`
- 认证：是

### 4.3 创建私有频道

- `POST /api/channels/private`
- 认证：是

请求体：

| 字段 | 类型 | 约束 |
| --- | --- | --- |
| `name` | `string` | 必填，最大长度 `128` |

### 4.4 邀请成员加入频道

- `POST /api/channels/{channelId}/invites`
- 认证：是

路径参数：

- `channelId`：正整数

请求体：

| 字段 | 类型 | 约束 |
| --- | --- | --- |
| `inviteeAccountId` | `long` | 正整数 |

成功响应 `data`：

| 字段 | 类型 |
| --- | --- |
| `channelId` | `long` |
| `inviteeAccountId` | `long` |
| `inviterAccountId` | `long` |
| `status` | `string` |
| `createdAt` | `instant` |
| `respondedAt` | `instant / null` |

### 4.5 接受频道邀请

- `POST /api/channels/{channelId}/invites/accept`
- 认证：是

路径参数：

- `channelId`：正整数

成功响应结构与“邀请成员加入频道”一致。

### 4.6 查询频道成员列表

- `GET /api/channels/{channelId}/members`
- 认证：是

路径参数：

- `channelId`：正整数

成功响应 `data` 为数组，每项字段：

| 字段 | 类型 |
| --- | --- |
| `accountId` | `long` |
| `nickname` | `string` |
| `avatarUrl` | `string` |
| `role` | `string` |
| `joinedAt` | `instant` |
| `mutedUntil` | `instant / null` |

### 4.7 提升成员为管理员

- `POST /api/channels/{channelId}/members/{targetAccountId}/admin`
- 认证：是

### 4.8 取消管理员身份

- `DELETE /api/channels/{channelId}/members/{targetAccountId}/admin`
- 认证：是

### 4.9 转移频道所有权

- `POST /api/channels/{channelId}/ownership-transfer`
- 认证：是

请求体：

| 字段 | 类型 | 约束 |
| --- | --- | --- |
| `targetAccountId` | `long` | 正整数 |

成功响应 `data`：

| 字段 | 类型 |
| --- | --- |
| `channelId` | `long` |
| `previousOwnerAccountId` | `long` |
| `previousOwnerRole` | `string` |
| `newOwnerAccountId` | `long` |
| `newOwnerRole` | `string` |

### 4.10 禁言频道成员

- `POST /api/channels/{channelId}/members/{targetAccountId}/mute`
- 认证：是

请求体：

| 字段 | 类型 | 约束 |
| --- | --- | --- |
| `durationSeconds` | `long` | 正整数 |

### 4.11 解除频道成员禁言

- `DELETE /api/channels/{channelId}/members/{targetAccountId}/mute`
- 认证：是

### 4.12 踢出频道成员

- `DELETE /api/channels/{channelId}/members/{targetAccountId}`
- 认证：是
- 成功响应：`data = null`

### 4.13 封禁频道成员

- `POST /api/channels/{channelId}/bans`
- 认证：是

请求体：

| 字段 | 类型 | 约束 |
| --- | --- | --- |
| `targetAccountId` | `long` | 正整数 |
| `reason` | `string` | 可选，最大长度 `256` |
| `durationSeconds` | `long / null` | 可选；如传则必须为正整数 |

成功响应 `data`：

| 字段 | 类型 |
| --- | --- |
| `channelId` | `long` |
| `bannedAccountId` | `long` |
| `operatorAccountId` | `long` |
| `reason` | `string / null` |
| `expiresAt` | `instant / null` |
| `createdAt` | `instant` |
| `revokedAt` | `instant / null` |

### 4.14 解除频道封禁

- `DELETE /api/channels/{channelId}/bans/{targetAccountId}`
- 认证：是

成功响应结构与“封禁频道成员”一致。

### 4.15 频道基础响应字段

以下接口返回 `ChannelResponse`：

- `GET /api/channels/default`
- `GET /api/channels/system`
- `POST /api/channels/private`

字段如下：

| 字段 | 类型 |
| --- | --- |
| `channelId` | `long` |
| `conversationId` | `long` |
| `name` | `string` |
| `type` | `string` |
| `defaultChannel` | `boolean` |
| `createdAt` | `instant` |
| `updatedAt` | `instant` |

除返回 `null` 的删除接口外，频道成员治理相关接口成功时通常返回最新 `ChannelMemberResponse` 或治理结果对象。

## 5. 频道消息接口

基路径：`/api/channels`

### 5.1 查询历史消息

- **方法**：`GET`
- **路径**：`/api/channels/{channelId}/messages`
- **认证**：是

查询参数：

| 参数 | 类型 | 约束 |
| --- | --- | --- |
| `cursor` | `long / null` | 可选；如传则必须为正整数 |
| `limit` | `int` | 默认 `20`，范围 `1..100` |

成功响应 `data`：

| 字段 | 类型 |
| --- | --- |
| `messages` | `ChannelMessageResponse[]` |
| `nextCursor` | `long / null` |

### 5.2 按关键字搜索消息

- **方法**：`GET`
- **路径**：`/api/channels/{channelId}/messages/search`
- **认证**：是

查询参数：

| 参数 | 类型 | 约束 |
| --- | --- | --- |
| `keyword` | `string` | 当前实现未加 Bean Validation 注解，但作为业务搜索词使用 |
| `limit` | `int` | 默认 `20`，范围 `1..100` |

成功响应 `data`：

| 字段 | 类型 |
| --- | --- |
| `messages` | `ChannelMessageResponse[]` |

### 5.3 上传消息附件

- **方法**：`POST`
- **路径**：`/api/channels/{channelId}/messages/attachments`
- **认证**：是
- **Content-Type**：`multipart/form-data`

表单字段：

| 字段 | 类型 | 约束 |
| --- | --- | --- |
| `messageType` | `string` | 必填；当前设计用于 `file` / `voice` |
| `file` | `file` | 必填；文件名不能为空 |

成功响应 `data`：

| 字段 | 类型 |
| --- | --- |
| `objectKey` | `string` |
| `filename` | `string` |
| `mimeType` | `string / null` |
| `size` | `long` |

### 5.4 撤回消息

- **方法**：`POST`
- **路径**：`/api/channels/{channelId}/messages/{messageId}/recall`
- **认证**：是

成功响应 `data` 为 `ChannelMessageResponse`。

### 5.5 消息响应字段

`ChannelMessageResponse` 字段如下：

| 字段 | 类型 |
| --- | --- |
| `messageId` | `long` |
| `serverId` | `string` |
| `conversationId` | `long` |
| `channelId` | `long` |
| `senderId` | `long` |
| `messageType` | `string` |
| `body` | `string / null` |
| `previewText` | `string / null` |
| `payload` | `string / null` |
| `metadata` | `string / null` |
| `status` | `string` |
| `createdAt` | `instant` |

## 6. 服务基础接口

基路径：`/api/server`

### 6.1 Echo

- **方法**：`POST`
- **路径**：`/api/server/echo`
- **认证**：否

请求体：

| 字段 | 类型 | 约束 |
| --- | --- | --- |
| `content` | `string` | 必填 |

成功响应 `data`：原始 `content` 字符串。

### 6.2 查询当前节点 presence

- **方法**：`GET`
- **路径**：`/api/server/presence/me`
- **认证**：是

成功响应 `data`：

| 字段 | 类型 |
| --- | --- |
| `accountId` | `long` |
| `status` | `enum` |
| `onlineSessionCount` | `int` |

## 7. 服务发现接口

### 7.1 Well-known 服务文档

- **方法**：`GET`
- **路径**：`/.well-known/carrypigeon-server`
- **认证**：否

成功响应 `data`：

| 字段 | 类型 |
| --- | --- |
| `serverId` | `string` |
| `serverName` | `string` |
| `registerEnabled` | `boolean` |
| `loginMethods` | `string[]` |
| `publicCapabilities` | `string[]` |
| `publicPlugins` | `string[]` |

## 8. 实时 WebSocket 接口

当前仓库存在独立于 Spring MVC 的 Netty WebSocket 通道。

- **默认路径**：`/ws`
- **配置来源**：`cp.chat.server.realtime.path`
- **默认监听端口**：`18080`
- **默认开关**：`cp.chat.server.realtime.enabled=false`

### 8.1 握手认证

- 握手前要求请求头：`Authorization: Bearer <access-token>`
- 缺失或非法时直接返回 HTTP `401`，响应体为纯文本错误消息，不升级到 WebSocket

### 8.2 客户端入站消息结构

`RealtimeClientMessage`：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `type` | `string` | 命令类型 |
| `channelId` | `long / null` | 频道 ID |
| `messageType` | `string / null` | 消息类型 |
| `body` | `string / null` | 文本正文 |
| `payload` | `object / null` | 结构化载荷 |
| `metadata` | `object / null` | 元数据 |

### 8.3 服务端出站消息结构

`RealtimeServerMessage`：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `type` | `string` | 事件类型 |
| `sessionId` | `string / null` | 会话标识 |
| `timestamp` | `long` | 毫秒时间戳 |
| `data` | `object / null` | 事件载荷 |

当前已确认的出站事件类型：

- `welcome`
- `heartbeat`
- `problem`
- `channel_message`
- `channel_message_updated`

其中 `channel_message` / `channel_message_updated` 的 `data` 载荷字段与 HTTP `ChannelMessageResponse` 基本对应，并包含已解析的附件访问载荷。

## 9. 当前未发现的接口形态

基于当前仓库代码，未发现以下对外协议形式：

- Swagger / OpenAPI 注解或生成文件
- `springdoc` 相关配置
- SSE 接口
- Spring STOMP `@MessageMapping` 实时协议

因此，当前对外接口事实来源仍以源码为准。
