# HTTP 接口

**状态：草稿（待评审）**
**读者：** 客户端开发者、联调与测试人员
**边界：** 本文只列端点、鉴权、参数与失败语义。字段级完整定义以运行时的 OpenAPI（`/v3/api-docs`、`/swagger-ui`）为准；错误结构见 `docs/standards/异常与错误码.md`。

---

## 1. 统一约定

| 项 | 约定 |
|---|---|
| 基路径 | `/api` |
| 鉴权 | `Authorization: Bearer <access_token>`；未携带或无效 → `401 unauthorized` |
| 权限注解 | 需要登录的接口标 `@PreAuthorize("isAuthenticated()")`；公开接口标 `@PermitAll` |
| 请求体 | JSON（`application/json`），上传接口用 `multipart/form-data` |
| 成功响应 | 业务 DTO；创建类返回 `201`，删除/更新类可返回空体（`204`/`200`） |
| 列表分页 | 游标分页，`CursorPageResponse{items, next_cursor, has_more}` |
| 错误响应 | `ApiErrorResponse{error: ApiError}`，`ApiError{status, reason, message, requestId?, details?}`，空值不输出 |
| 参数校验 | Bean Validation；路径 ID 用 `@Positive`；违规统一 `422 validation_failed` |

游标规则：

- `cursor` 是不透明字符串，客户端**只做回传**，不得解析或自造。
- `limit` 默认 `20`，消息类接口限制 `1..50`。
- `has_more=false` 时 `next_cursor` 为空，停止翻页。

---

## 2. 端点总表

### 2.1 认证 `auth`

| 方法 | 路径 | 鉴权 | 请求 | 响应 |
|---|---|---|---|---|
| POST | `/api/auth/register` | 公开 | `RegisterRequest` | `RegisterResponse` |
| POST | `/api/auth/login` | 公开 | `LoginRequest` | `AuthSessionTokenResponse` |
| POST | `/api/auth/tokens` | 公开 | `CreateTokenSessionRequest` | `AuthSessionTokenResponse` |
| POST | `/api/auth/refresh` | 公开 | `RefreshAccessTokenRequest` | `AuthSessionTokenResponse` |
| POST | `/api/auth/revoke` | 公开 | `RevokeRefreshTokenRequest` | 空体 |
| PUT | `/api/auth/me/email` | 登录 | `UpdateCurrentAccountEmailRequest` | 空体 |
| POST | `/api/auth/email_codes` | 公开 | `SendEmailCodeRequest` | 空体（属 verification feature） |

语义：

- `register` 需要邮箱验证码；注册成功后自动加入默认频道。
- `login` 与 `tokens` 都签发会话凭据；`tokens` 用于非密码换取的场景。
- `refresh` 用 refresh token 换新 access token；`revoke` 撤销 refresh token。
- `me/email` 修改当前账号邮箱，需已登录。
- 失败语义见 `AuthProblem` / `VerificationProblem`。

### 2.2 频道 `channel`

| 方法 | 路径 | 鉴权 | 请求 | 响应 |
|---|---|---|---|---|
| GET | `/api/channels` | 登录 | — | `ChannelListResponse` |
| GET | `/api/channels/{channelId}` | 登录 | 路径 `channelId` | `ChannelQueryResponse` |
| GET | `/api/channels/discover` | 登录 | `q?`、`cursor?`、`limit`(默认 20) | `CursorPageResponse<DiscoverChannelResponse>` |
| POST | `/api/channels` | 登录 | `CreateChannelRequest` | `ChannelListResponseItem`（201） |
| DELETE | `/api/channels/{channelId}` | 登录 | 路径 `channelId` | 空体 |
| PATCH | `/api/channels/{channelId}` | 登录 | `UpdateChannelProfileRequest` | 空体 |

语义：列表只返回当前账号已加入的频道；`discover` 用于发现可申请加入的频道；删除需要所有者且无依赖数据（`channel_delete_blocked`）。

### 2.3 频道成员治理

| 方法 | 路径 | 鉴权 | 请求 | 响应 |
|---|---|---|---|---|
| GET | `/api/channels/{channelId}/members` | 登录 | 路径 `channelId` | `ChannelMemberListResponse` |
| PUT | `/api/channels/{channelId}/admins/{targetAccountId}` | 登录 | 路径参数 | 空体 |
| DELETE | `/api/channels/{channelId}/admins/{targetAccountId}` | 登录 | 路径参数 | 空体 |
| DELETE | `/api/channels/{channelId}/members/{targetAccountId}` | 登录 | 路径参数 | 空体 |

### 2.4 入会申请

| 方法 | 路径 | 鉴权 | 请求 | 响应 |
|---|---|---|---|---|
| POST | `/api/channels/{channelId}/applications` | 登录 | `CreateChannelApplicationRequest` | `ChannelApplicationResponse` |
| GET | `/api/channels/{channelId}/applications` | 登录 | 路径 `channelId` | `ChannelApplicationListResponse` |
| POST | `/api/channels/{channelId}/applications/{applicationId}/decisions` | 登录 | `DecideChannelApplicationRequest` | `ChannelApplicationResponse` |

语义：申请与审批都需要频道管理权限；重复审批返回 `application_already_processed`，申请人已是成员返回 `conflict`。

### 2.5 禁言（ban）

| 方法 | 路径 | 鉴权 | 请求 | 响应 |
|---|---|---|---|---|
| PUT | `/api/channels/{channelId}/bans/{targetAccountId}` | 登录 | `ChannelBanRequest` | `ChannelBanResponse`（201） |
| DELETE | `/api/channels/{channelId}/bans/{targetAccountId}` | 登录 | 路径参数 | 空体 |
| GET | `/api/channels/{channelId}/bans` | 登录 | 路径 `channelId` | `ChannelBanListResponse` |

### 2.6 消息 `message`

| 方法 | 路径 | 鉴权 | 请求 | 响应 |
|---|---|---|---|---|
| GET | `/api/channels/{channelId}/messages` | 登录 | `cursor?`、`around_mid?`、`before?`、`after?`、`limit`(1..50) | `CursorPageResponse<ChannelMessageV1Response>` |
| GET | `/api/channels/{channelId}/messages/search` | 登录 | `q?`、`keyword?`、`cursor?`、`sender_uid?`、`domain?`、`before_mid?`、`after_mid?`、`limit`(1..50) | 同上 |
| POST | `/api/channels/{channelId}/messages` | 登录 | `SendChannelMessageRequest` | `ChannelMessageV1Response`（201） |
| POST | `/api/channels/{channelId}/messages/attachments` | 登录 | `multipart/form-data`：`file`、`message_type?` | `MessageAttachmentUploadResponse` |
| POST | `/api/channels/{channelId}/messages/{messageId}/recall` | 登录 | 路径参数 | `ChannelMessageV1Response` |
| POST | `/api/messages/{messageId}/forward` | 登录 | `ForwardChannelMessageRequest` | `ChannelMessageV1Response`（201） |

语义：

- 历史查询支持锚点（`around_mid`）与方向控制（`before`/`after`），三者与 `cursor` 不可混用。
- 发送与转发是**幂等**的：同一幂等键重复提交返回首次结果，指纹不同返回 `idempotency_key_reused`。
- 被禁言账号发送返回 `user_muted`；撤回权限不足返回 `channel_message_recall_forbidden`。
- 附件上传依赖对象存储，不可用时返回 `storage_service_unavailable`。

### 2.7 已读与提及

| 方法 | 路径 | 鉴权 | 请求 | 响应 |
|---|---|---|---|---|
| PUT | `/api/channels/{channelId}/read_state` | 登录 | `UpdateChannelReadStateRequest` | `ChannelReadStateResponse` |
| GET | `/api/unreads` | 登录 | — | `UnreadListResponse` |
| GET | `/api/mentions` | 登录 | `cursor?`、`limit`、`unread_only?`、`cid?` | `MentionListResponse` |
| PUT | `/api/mentions/{mentionId}/read` | 登录 | 路径 `mentionId`（字符串） | 空体 |
| PUT | `/api/mentions/read_state` | 登录 | `UpdateMentionReadStateRequest`（可空） | 空体 |

### 2.8 文件 `file`

| 方法 | 路径 | 鉴权 | 请求 | 响应 |
|---|---|---|---|---|
| POST | `/api/files/uploads` | 登录 | `CreateFileUploadRequest` | `FileUploadResponse` |
| PUT | `/api/files/uploads/{shareKey}` | 登录 | 任意媒体类型请求体 | 空体 |
| GET | `/api/files/download/{shareKey}` | **公开** | 路径 `shareKey` | 文件流 |
| POST | `/api/users/me/background` | 登录 | `multipart/form-data`：`background` | `ProfileBackgroundUploadResponse` |

语义：上传分两步（先申请 `shareKey`，再 PUT 内容）；下载靠 shareKey 鉴权，不需要登录但 shareKey 由服务端签名，非法返回 `invalid_share_key`。

### 2.9 用户 `user`

| 方法 | 路径 | 鉴权 | 请求 | 响应 |
|---|---|---|---|---|
| GET | `/api/users/{accountId}/detail` | 公开 | 路径 `accountId` | `UserDetailProfileResponse` |
| GET | `/api/users?ids=...` | 公开 | `ids`（必填，多次或逗号分隔） | `UserPublicProfileListResponse` |
| GET | `/api/users/me` | 登录 | — | `UserDetailProfileResponse` |
| PATCH | `/api/users/me` | 登录 | `PatchCurrentUserProfileRequest` | 空体 |

### 2.10 插件与域目录

| 方法 | 路径 | 鉴权 | 请求 | 响应 |
|---|---|---|---|---|
| GET | `/api/domains/catalog` | 公开 | — | `DomainCatalogResponse` |
| GET | `/api/plugins/catalog` | 公开 | — | `PluginCatalogResponse` |
| POST | `/api/gates/required/check` | 公开 | `RequiredGateCheckRequest` | `RequiredGateCheckResult` |

语义：`gates/required/check` 供客户端启动时校验必需插件是否齐备，缺失返回 `required_plugin_missing`（412）。

### 2.11 服务发现

| 方法 | 路径 | 鉴权 | 请求 | 响应 |
|---|---|---|---|---|
| GET | `/api/server` | 公开 | — | `ServerDiscoveryDocument` |

返回内容包含服务标识、协议版本、`ws_url`、能力开关（`messageDomains`/`pluginCatalog`/`eventResume`）与必需插件列表。`cp.chat.server.realtime.enabled=false` 时 `ws_url` 为 `null`。

---

## 3. 失败语义速查

| HTTP | reason 常见值 | 典型场景 |
|---|---|---|
| 401 | `unauthorized` | 未带 token、token 无效 |
| 401 | `token_expired` | access token 过期 |
| 403 | `channel_owner_required` / `channel_role_forbidden` / `channel_kick_forbidden` / `file_access_forbidden` / `user_muted` | 权限不足 |
| 404 | `not_found` | 目标资源不存在 |
| 409 | `idempotency_key_reused` / `channel_delete_blocked` / `application_already_processed` / `conflict` | 状态冲突 |
| 412 | `required_plugin_missing` | 客户端插件不齐 |
| 413 | `payload_too_large` | 上传超限 |
| 422 | `validation_failed` / `cursor_invalid` / `invalid_share_key` / `schema_invalid` | 入参不合法 |
| 503 | `storage_service_unavailable` / `mail_service_unavailable` / `email_delivery_failed` | 外部依赖不可用 |

完整码表见 `docs/standards/异常与错误码.md` §4。

---

## 4. 客户端接入要点

1. 启动先调 `GET /api/server` 拿 `ws_url` 与能力开关，再决定是否建立实时连接（协议见 `docs/api/实时通道协议.md`）。
2. 所有写操作都走 HTTP；实时通道只负责接收推送。
3. 分页统一回传 `next_cursor`，不要自己拼游标。
4. 幂等：消息发送/转发需带幂等键；同一键只对应一次业务结果。
5. 429/503 需要退避重试；4xx 不要重试（除幂等冲突可读回首次结果）。

---

## 5. 待补项

| # | 项 | 说明 |
|---|---|---|
| 1 | 各 DTO 的字段级说明 | 当前由 OpenAPI 承载；后续按 feature 文档逐个补齐关键字段 |
| 2 | `GET /api/users?ids=` 的批量上限 | 代码中未见显式上限，待确认后写入本文 |
| 3 | 幂等键的传递位置（请求体字段名） | 见 `SendChannelMessageRequest` / `ForwardChannelMessageRequest`，待与客户端对齐后写入 |
