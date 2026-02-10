# Error Reasons (Generated)

> Auto-generated from `CPProblemReason` enum. Do not edit manually.

Total: 28

## Status Distribution
- `401`: 2
- `403`: 7
- `404`: 1
- `406`: 1
- `409`: 5
- `412`: 1
- `422`: 7
- `429`: 1
- `500`: 3

## Canonical Reason Table
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
