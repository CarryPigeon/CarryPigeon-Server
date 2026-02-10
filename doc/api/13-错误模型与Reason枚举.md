# 13｜错误模型与 Reason 枚举

版本：1.0（draft）  
日期：2026-02-01  

## 1. 目标

- 让客户端以 `reason` 做稳定分支（而不是解析 `message` 文案）。
- 同一类错误在 HTTP 与 WS 下保持一致结构（字段子集允许）。
- 充分利用 HTTP status code，但不把业务语义“绑死”在 status code 上。

## 2. HTTP 错误响应结构（统一）

当 HTTP 返回非 2xx 时，响应体必须为：

```json
{
  "error": {
    "status": 422,
    "reason": "validation_failed",
    "message": "validation failed",
    "request_id": "req_01H...",
    "details": {}
  }
}
```

字段说明：
- `status`：HTTP 状态码镜像（便于日志与网关统一处理）
- `reason`：机器可读原因（客户端分支关键字段）
- `message`：面向用户/开发者的简短描述（可用于 toast，但不作为分支条件）
- `request_id`：服务端生成的请求 id（用于链路追踪；可选但强烈建议）
- `details`：可选扩展细节（字段错误、缺失插件列表等）

## 3. WS 错误结构（字段子集）

WS `command.err` 的 `error` 字段应与 HTTP `error` 结构保持一致（可省略 `request_id`）：

```json
{
  "type": "command.err",
  "id": "1",
  "error": {
    "reason": "unauthorized",
    "message": "access token expired"
  }
}
```

<!-- AUTO-GENERATED:ERROR_REASONS:BEGIN -->
## 3.1 自动生成 Reason 总表（代码权威）

> 本节由 `scripts/generate_protocol_artifacts.py` 从 `CPProblemReason` 自动生成。
> 变更 reason 枚举后请重新生成产物，不要手改下表。

### 状态码分布
- `401`: 2
- `403`: 7
- `404`: 1
- `406`: 1
- `409`: 5
- `412`: 1
- `422`: 7
- `429`: 1
- `500`: 3

### Canonical Reason Table
| Enum | Reason Code | HTTP Status |
|---|---|---|
| `UNAUTHORIZED` | `unauthorized` | `401` |
| `TOKEN_EXPIRED` | `token_expired` | `401` |
| `API_VERSION_UNSUPPORTED` | `api_version_unsupported` | `406` |
| `FORBIDDEN` | `forbidden` | `403` |
| `NOT_CHANNEL_MEMBER` | `not_channel_member` | `403` |
| `NOT_CHANNEL_ADMIN` | `not_channel_admin` | `403` |
| `NOT_CHANNEL_OWNER` | `not_channel_owner` | `403` |
| `USER_MUTED` | `user_muted` | `403` |
| `CANNOT_BAN_ADMIN` | `cannot_ban_admin` | `403` |
| `CANNOT_CHANGE_OWNER_AUTHORITY` | `cannot_change_owner_authority` | `403` |
| `NOT_FOUND` | `not_found` | `404` |
| `CONFLICT` | `conflict` | `409` |
| `ALREADY_IN_CHANNEL` | `already_in_channel` | `409` |
| `APPLICATION_ALREADY_PROCESSED` | `application_already_processed` | `409` |
| `IDEMPOTENCY_PROCESSING` | `idempotency_processing` | `409` |
| `REQUIRED_PLUGIN_MISSING` | `required_plugin_missing` | `412` |
| `VALIDATION_FAILED` | `validation_failed` | `422` |
| `SCHEMA_INVALID` | `schema_invalid` | `422` |
| `CURSOR_INVALID` | `cursor_invalid` | `422` |
| `EVENT_TOO_OLD` | `event_too_old` | `422` |
| `CHANNEL_FIXED` | `channel_fixed` | `422` |
| `EMAIL_INVALID` | `email_invalid` | `422` |
| `EMAIL_CODE_INVALID` | `email_code_invalid` | `422` |
| `RATE_LIMITED` | `rate_limited` | `429` |
| `INTERNAL_ERROR` | `internal_error` | `500` |
| `EMAIL_SERVICE_DISABLED` | `email_service_disabled` | `500` |
| `EMAIL_SEND_FAILED` | `email_send_failed` | `500` |
| `EMAIL_EXISTS` | `email_exists` | `409` |

<!-- AUTO-GENERATED:ERROR_REASONS:END -->

## 4. Reason 枚举（P0 必须稳定）

### 4.1 认证与权限

- `unauthorized`：缺少/无效 access_token（HTTP 通常为 401）
- `forbidden`：已登录但无权限（HTTP 通常为 403）
- `token_expired`：token 过期（可归类为 `unauthorized`，但建议细分）
- `api_version_unsupported`：请求的 API 版本不被支持（HTTP 可用 406 或 426；WS 可在 `auth.err` 返回）

### 4.2 required gate（P0）

- `required_plugin_missing`
  - `details.missing_plugins: string[]`

建议 HTTP status：`412 Precondition Failed`

示例：

```json
{
  "error": {
    "status": 412,
    "reason": "required_plugin_missing",
    "message": "required plugins are missing",
    "details": { "missing_plugins": ["mc-bind"] }
  }
}
```

### 4.3 参数与校验

- `validation_failed`：字段缺失/类型不符/越界
  - `details.field_errors[] = { field, reason, message }`
- `schema_invalid`：domain contract/schema 校验失败（尤其是非 `Core:*` domain）

建议 HTTP status：`422 Unprocessable Entity`

### 4.4 资源与状态

- `not_found`：资源不存在或已被硬删除（HTTP 通常为 404）
- `conflict`：状态冲突（例如重复申请/重复加入）（HTTP 通常为 409）
- `cursor_invalid`：分页游标无效/过期（HTTP 通常为 422 或 404）
- `rate_limited`：频率限制（HTTP 通常为 429，`details.retry_after_ms`）
- `event_too_old`：事件回放窗口不足（常见于 WS resume.failed，也可用 HTTP 422 表达）

### 4.5 频道与成员（P0）

- `not_channel_member`：非频道成员访问成员/消息等资源（HTTP 通常为 403）
- `not_channel_admin`：需要管理员权限（HTTP 通常为 403）
- `not_channel_owner`：需要频道 owner 权限（HTTP 通常为 403）
- `user_muted`：当前用户在该频道被禁言（HTTP 通常为 403）
- `application_already_processed`：入群申请已被处理（HTTP 通常为 409）

### 4.6 服务端错误

- `internal_error`：未分类服务端错误（HTTP 通常为 500）

## 5. HTTP status code 建议映射（推荐）

- 400：`validation_failed`（基础参数错误）
- 401：`unauthorized` / `token_expired`
- 403：`forbidden`
- 404：`not_found`
- 409：`conflict`
- 412：`required_plugin_missing`
- 422：`validation_failed` / `schema_invalid`
- 429：`rate_limited`
- 500：`internal_error`

> 注意：客户端必须以 `error.reason` 为主分支条件；status 仅用于通用兜底与日志分组。
