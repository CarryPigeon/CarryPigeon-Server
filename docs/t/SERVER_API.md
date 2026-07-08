# 服务端待实现 API 接口清单

> 本文档整理客户端（CarryPigeon Desktop）已定义但服务端尚未实现的 HTTP API 端点、模型扩展和 WebSocket 事件。供后端服务端团队参考实现。

## 协议约定

所有接口遵循 `docs/t/` 目录下的 v1 协议规范：

| 规范 | 文档 |
|------|------|
| 通用协议 | `docs/t/10-http-ws-protocol-v1.md` |
| 端点清单 | `docs/t/11-http-endpoints-v1.md` |
| WS 事件 | `docs/t/12-ws-events-v1.md` |
| 错误模型 | `docs/t/13-error-model-and-reasons-v1.md` |
| 分页游标 | `docs/t/14-pagination-and-cursor-v1.md` |

### 全局约束

- Base URL: `/api`
- 鉴权: `Authorization: Bearer <access_token>`（除非标注"公开"）
- JSON 字段: `snake_case`
- 时间字段: Unix epoch 毫秒（`int64`）
- 雪花 ID: JSON 中编码为十进制字符串
- 分页响应统一结构: `{ items[], next_cursor: string|null, has_more: boolean }`
- 错误响应统一结构: `{ error: { status, reason, message, details? } }`
- 错误 `reason` 枚举: 见 `docs/t/13-error-model-and-reasons-v1.md`

---

## 一、HTTP 端点（已实现前端层，待服务端实现）

> 以下端点客户端侧已完整实现（Wire 模型 + HTTP 函数 + Port 接口 + Domain 模型 + Mapper），**仅需服务端提供对应 HTTP 端点**。

### 1.1 消息搜索

```
GET /api/channels/{cid}/messages/search
```

#### Path 参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `cid` | string | 是 | 目标频道 |

#### Query 参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `q` | string | 是 | 搜索词，trim 后长度 1-100 |
| `cursor` | string | 否 | 不透明分页游标 |
| `limit` | number | 否 | 默认 20，最大 50 |
| `sender_uid` | string | 否 | 按发送者过滤（P0 可选） |
| `domain` | string | 否 | 按消息 domain 过滤（P0 可选） |
| `before_mid` | string | 否 | 只查该消息之前的结果（P1 可选） |
| `after_mid` | string | 否 | 只查该消息之后的结果（P1 可选） |

#### 响应

```json
{
  "items": [
    {
      "mid": "723155640365318144",
      "cid": "12345",
      "uid": "67890",
      "sender": { "uid": "67890", "nickname": "Bob", "avatar": "avatars/u/67890.png" },
      "send_time": 1700000000000,
      "domain": "Core:Text",
      "domain_version": "1.0.0",
      "data": { "text": "hello" },
      "preview": "hello"
    }
  ],
  "next_cursor": "msg_000000000000000100",
  "has_more": true
}
```

#### 错误 reason 参考

| 条件 | HTTP | reason |
|------|------|--------|
| q 为空或超长 | 422 | `validation_failed` |
| cursor 无效 | 422 | `cursor_invalid` |
| 非频道成员 | 403 | `not_channel_member` |

- 客户端参考: `ChatMessageSearchQueryWire` → `httpSearchChannelMessages` → `searchChannelMessages`
- 分页规范: 遵循 `docs/t/14-pagination-and-cursor-v1.md`

---

### 1.2 消息上下文定位

```
GET /api/channels/{cid}/messages?around_mid={mid}
```

#### Path 参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `cid` | string | 是 | 频道 ID |

#### Query 参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `around_mid` | string | 是 | 目标消息 ID，返回目标消息及前后上下文 |
| `before` | number | 否 | 目标消息之前加载条数，默认 25，最大 50 |
| `after` | number | 否 | 目标消息之后加载条数，默认 25，最大 50 |

#### 响应

与 `GET /api/channels/{cid}/messages` 同结构（复用消息分页响应）。

- 客户端参考: `httpListChannelMessagesAround` → `listChannelMessagesAround`

---

### 1.3 消息编辑

```
PATCH /api/messages/{mid}
```

#### Path 参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `mid` | string | 是 | 目标消息 ID |

#### 请求体

| 字段 | 类型 | 必填 | 用途 |
|------|------|------|------|
| `domain` | string | 是 | P0 只要求 `Core:Text` |
| `domain_version` | string | 是 | domain 版本号 |
| `data` | object | 是 | domain payload，如 `{ "text": "..." }` |
| `mentions` | array | 否 | 候选提及列表 `[{ type, uid }]`，服务端重新校验和规范化 |
| `expected_edit_version` | number | 否 | 乐观并发版本，不匹配返回 `conflict` |

#### 响应

返回完整的消息对象（建议含 `edited_at` 和 `edit_version` 字段）。

#### 错误 reason

| 条件 | HTTP | reason |
|------|------|--------|
| 消息不可编辑（如非自己发送、频道已删除等） | 403 | `message_not_editable` |
| 编辑窗口已过期（超出允许编辑时间） | 403 | `message_edit_window_expired` |
| 乐观并发版本不匹配 | 409 | `conflict` |

#### 语义约定

- 编辑后推送 `message.updated` WS 事件（见 WS 事件章节）
- 编辑时间窗口由服务端策略决定（建议至少 5 分钟）
- `expected_edit_version` 缺省时服务端应使用最新版本写入

- 客户端参考: `ChatMessageEditWire` → `httpEditMessage` → `editMessage`

---

### 1.4 频道置顶

```
POST /api/channels/{cid}/pins/{mid}
```

#### Path 参数

| 字段 | 类型 | 必填 | 用途 |
|------|------|------|------|
| `cid` | string | 是 | 频道 ID |
| `mid` | string | 是 | 消息 ID |

#### 请求体（可选）

| 字段 | 类型 | 必填 | 用途 |
|------|------|------|------|
| `note` | string | 否 | 置顶备注，最大 200 字符 |

#### 错误 reason

| 条件 | HTTP | reason |
|------|------|--------|
| 频道置顶数已达上限 | 409 | `pin_limit_reached` |

- 客户端参考: `httpPinMessage` → `pinMessage`

---

### 1.5 取消置顶

```
DELETE /api/channels/{cid}/pins/{mid}
```

#### Path 参数

| 字段 | 类型 | 必填 | 用途 |
|------|------|------|------|
| `cid` | string | 是 | 频道 ID |
| `mid` | string | 是 | 消息 ID |

#### 响应

`204 No Content`

- 客户端参考: `httpUnpinMessage` → `unpinMessage`

---

### 1.6 置顶列表

```
GET /api/channels/{cid}/pins
```

#### Path 参数

| 字段 | 类型 | 必填 | 用途 |
|------|------|------|------|
| `cid` | string | 是 | 频道 ID |

#### Query 参数

| 字段 | 类型 | 必填 | 用途 |
|------|------|------|------|
| `cursor` | string | 否 | 不透明分页游标 |
| `limit` | number | 否 | 默认 20，最大 50 |

#### 响应

```json
{
  "items": [
    {
      "cid": "12345",
      "mid": "723155640365318144",
      "pinned_by_uid": "67890",
      "pinned_at": 1700000000000,
      "note": "重要通知"
    }
  ],
  "next_cursor": null,
  "has_more": false
}
```

- 客户端参考: `ChatPinListWire` → `httpListPins` → `listPins`

---

### 1.7 消息转发

```
POST /api/messages/{mid}/forward
```

#### Path 参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `mid` | string | 是 | 源消息 ID |

#### 请求体

| 字段 | 类型 | 必填 | 用途 |
|------|------|------|------|
| `target_cid` | string | 是 | 目标频道 ID |
| `comment` | string | 否 | 附言，P0 最大 500 字符 |
| `idempotency_key` | string | 否 | 幂等键，HTTP header 优先 |

#### 响应

返回目标频道中创建的转发消息对象（同消息响应结构）。

#### 语义约定

- 转发成功后应在目标频道创建一条新消息，`data` 包含转发来源摘要
- 建议在消息模型中增加 `forwarded_from` 字段标记转发来源

#### 错误 reason

| 条件 | HTTP | reason |
|------|------|--------|
| 不允许转发该消息 | 403 | `message_forward_forbidden` |

- 客户端参考: `ChatMessageForwardWire` → `httpForwardMessage` → `forwardMessage`

---

### 1.8 提及收件箱列表

```
GET /api/mentions
```

#### Query 参数

| 字段 | 类型 | 必填 | 用途 |
|------|------|------|------|
| `cursor` | string | 否 | 不透明分页游标 |
| `limit` | number | 否 | 默认 20，最大 50 |
| `unread_only` | boolean | 否 | 只返回未读提及 |
| `cid` | string | 否 | 限定频道 |

#### 响应

```json
{
  "items": [
    {
      "mention_id": "723155640365318144",
      "cid": "12345",
      "mid": "723155640365318144",
      "from_uid": "67890",
      "target": { "type": "user", "uid": "123" },
      "created_at": 1700000000000,
      "read": false
    }
  ],
  "next_cursor": null,
  "has_more": false
}
```

- 客户端参考: `ChatMentionPageWire` → `httpListMentions` → `listMentions`

---

### 1.9 标记单条提及已读

```
PUT /api/mentions/{mention_id}/read
```

#### Path 参数

| 字段 | 类型 | 必填 | 用途 |
|------|------|------|------|
| `mention_id` | string | 是 | 目标提及 ID |

#### 响应

`204 No Content`

- 客户端参考: `httpMarkMentionRead` → `markMentionRead`

---

### 1.10 批量标记提及已读

```
PUT /api/mentions/read_state
```

#### 请求体

| 字段 | 类型 | 必填 | 用途 |
|------|------|------|------|
| `before_mention_id` | string | 否 | 标记该 mention 及之前的提及为已读 |
| `cid` | string | 否 | 只标记该频道内的提及 |

若两者都缺省，标记当前用户所有提及为已读（应二次确认）。

#### 响应

`204 No Content`

- 客户端参考: `httpBatchMarkMentionsRead` → `batchMarkMentionsRead`

---

## 二、HTTP 端点（客户端和服务端均未实现）

> 以下端点客户端侧也无实现，需新建完整的 Wire 类型、Domain 模型、Port 方法、HTTP 函数和 Mapper。字段定义已优化为最大复用现有模式，**新建字段以 ★ 标记**。

### 2.1 通知偏好

#### 2.1.1 通知偏好查询

```
GET /api/notification_preferences
```

##### 响应

| 嵌套字段 | 类型 | 用途 | 字段来源 |
|----------|------|------|----------|
| `server.mode` | string | 服务端级别，枚举: `all`, `mentions_only`, `muted` | ★ 新建——`NotificationServerMode` |
| `server.muted_until` | number | 服务端静音截止时间，0=永久 | ★ 新建——静音时间戳 |
| `channels[].cid` | string | 频道 ID | ✅ 复用 `ChatChannelWire.cid` |
| `channels[].mode` | string | 频道级别，枚举: `all`, `mentions_only`, `muted`, `inherit` | ★ 新建——`NotificationMode` |
| `channels[].muted_until` | number | 频道静音截止时间，0=永久 | ★ 新建——静音时间戳 |

响应结构:

```json
{
  "server": {
    "mode": "all",
    "muted_until": 0
  },
  "channels": [
    { "cid": "12345", "mode": "all", "muted_until": 0 }
  ]
}
```

---

#### 2.1.2 频道通知偏好设置

```
PUT /api/channels/{cid}/notification_preference
```

##### Path 参数

| 字段 | 类型 | 必填 | 用途 | 字段来源 |
|------|------|------|------|----------|
| `cid` | string | 是 | 频道 ID | ✅ 复用 `ChatChannelWire.cid` |

##### 请求体

| 字段 | 类型 | 必填 | 用途 | 字段来源 |
|------|------|------|------|----------|
| `mode` | string | 是 | 枚举: `all`, `mentions_only`, `muted`, `inherit` | ★ 新建——`NotificationMode` |
| `muted_until` | number | 否 | 0=永久静音 | ★ 新建——静音时间戳 |

##### 响应

`204 No Content`

---

#### 2.1.3 服务端通知偏好设置

```
PUT /api/notification_preferences/server
```

##### 请求体

| 字段 | 类型 | 必填 | 用途 | 字段来源 |
|------|------|------|------|----------|
| `mode` | string | 是 | 枚举: `all`, `mentions_only`, `muted`（不允许 `inherit`） | ★ 新建——`NotificationServerMode` |
| `muted_until` | number | 否 | 0=永久静音 | ★ 新建——静音时间戳 |

##### 响应

`204 No Content`

---

### 2.2 远端频道发现

```
GET /api/channels/discover
```

#### Query 参数

| 参数 | 类型 | 必填 | 说明 | 字段来源 |
|------|------|------|------|----------|
| `q` | string | 否 | 搜索词；缺省时返回推荐或公开频道 | ✅ 复用 `ChatMessageSearchQueryWire.q` 模式 |
| `cursor` | string | 否 | 不透明分页游标 | ✅ 复用通用分页模式 |
| `limit` | number | 否 | 默认 20，最大 50 | ✅ 复用通用分页模式 |
| `type` | string | 否 | 筛选: `text`, `announcement`, `management` | ★ 新建——频道类型枚举 |

#### 响应

| 字段 | 类型 | 用途 | 字段来源 |
|------|------|------|----------|
| `cid` | string | 频道 ID | ✅ 复用 `ChatChannelWire.cid` |
| `name` | string | 频道名 | ✅ 复用 `ChatChannelWire.name` |
| `brief` | string | 简介 | ✅ 复用 `ChatChannelWire.brief` |
| `avatar` | string | 头像 | ✅ 复用 `ChatChannelWire.avatar` |
| `member_count` | number | 频道成员数 | ★ 新建 |
| `requires_application` | boolean | 是否需要申请加入 | ★ 新建 |
| `next_cursor` | string | 下一页游标 | ✅ 复用 `ChatMessagePageWire.next_cursor` |
| `has_more` | boolean | 是否还有更多 | ✅ 复用 `ChatMessagePageWire.has_more` |

```json
{
  "items": [
    {
      "cid": "12345",
      "name": "General",
      "brief": "讨论区",
      "avatar": "avatars/ch/12345.png",
      "member_count": 42,
      "requires_application": false
    }
  ],
  "next_cursor": null,
  "has_more": false
}
```

---

### 2.3 审计日志

```
GET /api/audit_logs
```

#### Query 参数

| 参数 | 类型 | 必填 | 说明 | 字段来源 |
|------|------|------|------|----------|
| `cursor` | string | 否 | 不透明分页游标 | ✅ 复用通用分页模式 |
| `limit` | number | 否 | 默认 50，最大 100 | ✅ 复用通用分页模式 |
| `cid` | string | 否 | 限定频道 | ✅ 复用 `ChatChannelWire.cid` |
| `actor_uid` | string | 否 | 操作者 UID | ★ 新建——遵循 `from_uid`/`uid` 模式 |
| `action` | string | 否 | 动作类型枚举 | ★ 新建——12 个枚举值 |
| `from_time` | number | 否 | 起始时间，epoch ms | ★ 新建——时间范围筛选 |
| `to_time` | number | 否 | 结束时间，epoch ms | ★ 新建——时间范围筛选 |

#### 响应

| 字段 | 类型 | 用途 | 字段来源 |
|------|------|------|----------|
| `audit_id` | string | 雪花 ID | ★ 新建——遵循雪花 ID 模式（同 `mid`） |
| `cid` | string | 频道 ID | ✅ 复用 `ChatChannelWire.cid` |
| `actor_uid` | string | 操作者 UID | ★ 新建 |
| `action` | string | 动作类型 | ★ 新建——12 枚举值 |
| `details` | unknown | 扩展细节 | ★ 新建——预留 |
| `created_at` | number | 创建时间 | ★ 新建——复用 `ChatMentionWire.created_at` 命名 |
| `items` | array | 本页数据 | ✅ 复用分页模式 |
| `next_cursor` | string | 下一页游标 | ✅ 复用分页模式 |
| `has_more` | boolean | 是否还有更多 | ✅ 复用分页模式 |

#### P0 action 枚举值（12 个，全部新建）

| 枚举值 | 说明 |
|--------|------|
| `channel.create` | 创建频道 |
| `channel.delete` | 删除频道 |
| `channel.update` | 更新频道资料 |
| `channel.member.kick` | 踢出成员 |
| `channel.admin.grant` | 设为管理员 |
| `channel.admin.revoke` | 撤销管理员 |
| `channel.ban.create` | 禁言 |
| `channel.ban.delete` | 解除禁言 |
| `message.delete` | 删除消息 |
| `message.edit` | 编辑消息 |
| `message.pin` | 置顶消息 |
| `message.unpin` | 取消置顶 |

---

## 三、扩展现有端点

### 3.1 发送消息扩展字段

`POST /api/channels/{cid}/messages` 现有请求体新增字段：

| 字段 | 类型 | 必填 | 用途 | 字段来源 |
|------|------|------|------|----------|
| `mentions` | array | 否 | 候选提及列表 `[{ type, uid }]` | ✅ 复用 `ChatMessageEditWire.mentions` 类型 |
| `client_message_id` | string | 否 | 客户端本地消息 ID，用于 optimistic 关联，不参与全局 ID 语义 | ★ 新建 |

服务端行为约定：
- `mentions` 由服务端校验、去重、过滤后，写回消息记录中的规范化结果
- 校验失败时可返回 reason: `mention_target_invalid`, `mention_not_allowed`（P0 可先映射为 `validation_failed`/`forbidden`）
- `client_message_id` 不参与全局 ID 语义，仅用于客户端乐观更新关联

---

### 3.2 发送消息请求体扩展（Wire）

`ChatSendMessageWire` 需补充：

```json
{
  "domain": "Core:Text",
  "domain_version": "1.0.0",
  "data": { "text": "hello" },
  "reply_to_mid": "0",
  "mentions": [{ "type": "user", "uid": "67890" }],
  "client_message_id": "local-msg-001"
}
```

---

## 四、扩展数据模型

### 4.1 扩展消息模型

消息 record 新增字段（HTTP 响应和 WS 事件中返回）：

| 字段 | 类型 | 用途 | 字段来源 |
|------|------|------|----------|
| `edited_at` | number | 消息最后编辑时间，未编辑时缺省 | ★ 新建——类型同 `send_time`（epoch ms） |
| `edit_version` | number | 编辑版本号，从 1 开始；用于乐观并发 | ★ 新建——与 `expected_edit_version` 对应 |
| `mentions` | array | 服务端规范化后的提及目标列表 `[{ type, uid }]` | ✅ 复用 `ChatMessageEditWire.mentions` 类型 |
| `forwarded_from` | object | 转发消息的来源摘要 | ★ 新建容器——内部字段均复用现有类型 |

`forwarded_from` 内部结构：

| 字段 | 类型 | 用途 | 字段来源 |
|------|------|------|----------|
| `mid` | string | 源消息 ID | ✅ 复用 `ChatMessageWire.mid` |
| `cid` | string | 源频道 ID | ✅ 复用 `ChatChannelWire.cid` |
| `uid` | string | 源发送者 UID | ✅ 复用 `ChatUserWire.uid` |
| `preview` | string | 源消息预览 | ✅ 复用 `ChatMessageWire.preview` |
| `send_time` | number | 源发送时间 | ✅ 复用 `ChatMessageWire.send_time` |

---

### 4.2 扩展频道模型

频道 record 新增字段：

| 字段 | 类型 | 必填 | 用途 | 字段来源 |
|------|------|------|------|----------|
| `category_id` | string | 否 | 频道分类 ID；缺省归入 `"default"` | ★ 新建 |
| `category_name` | string | 否 | 分类展示名；缺省展示 `"Channels"` | ★ 新建 |
| `order` | number | 否 | 服务端排序键，越小越靠前 | ★ 新建 |
| `type` | string | 否 | P0 支持 `text`, `announcement`, `management` | ★ 新建——同 2.2 type 枚举 |
| `joined` | boolean | 否 | 当前用户是否已加入 | ✅ 已存在于客户端 `ChannelSummary` |
| `join_requested` | boolean | 否 | 当前用户是否已提交申请 | ✅ 已存在于客户端 `ChannelSummary` |

---

## 五、WebSocket 事件扩展

### 5.1 `message.updated`

触发条件: 消息被编辑后推送

```json
{
  "type": "event",
  "data": {
    "event_id": "723155640365318144",
    "event_type": "message.updated",
    "server_time": 1700000000000,
    "payload": {
      "cid": "12345",
      "message": {
        "mid": "1",
        "cid": "12345",
        "uid": "67890",
        "sender": { "uid": "67890", "nickname": "Bob", "avatar": "avatars/u/67890.png" },
        "send_time": 1700000000000,
        "edited_at": 1700000100000,
        "edit_version": 2,
        "domain": "Core:Text",
        "domain_version": "1.0.0",
        "data": { "text": "edited content" },
        "preview": "edited content"
      }
    }
  }
}
```

**字段说明**: 载荷结构与 `message.created` 完全一致，可直接复用。

---

### 5.2 `message.pinned` / `message.unpinned`

触发条件: 消息被置顶/取消置顶

```json
{
  "type": "event",
  "data": {
    "event_id": "723155640365318144",
    "event_type": "message.pinned",
    "server_time": 1700000000000,
    "payload": {
      "cid": "12345",
      "mid": "723155640365318144",
      "pin_id": "723155640365318145",
      "pinned_by_uid": "67890",
      "pinned_at": 1700000000000
    }
  }
}
```

`message.unpinned` 对应字段: `unpinned_by_uid`、`unpinned_at`，其余同。

**字段来源**: `cid`/`mid`/`pinned_by_uid`/`pinned_at` 复用 `ChatPinWire`，仅 `pin_id` 为新建。

---

### 5.3 `mention.created`

触发条件: 用户被 @提及

```json
{
  "type": "event",
  "data": {
    "event_id": "723155640365318144",
    "event_type": "mention.created",
    "server_time": 1700000000000,
    "payload": {
      "mention_id": "723155640365318144",
      "cid": "12345",
      "mid": "723155640365318144",
      "from_uid": "67890",
      "target": { "type": "user", "uid": "123" },
      "created_at": 1700000000000
    }
  }
}
```

**字段说明**: 载荷所有字段在客户端已有对应类型（`ChatMentionWire`）。

---

---

## 六、优先级建议

| 优先级 | 端点/模型 | 理由 |
|--------|-----------|------|
| P0 | 消息搜索、消息编辑、@提及 x3、置顶 x3、转发 | 前端已完整实现，仅缺后端 |
| P0 | 扩展消息模型（edited_at, edit_version, mentions, forwarded_from） | 被编辑/转发功能依赖 |
| P1 | 消息上下文定位 | 跳转定位功能 |
| P1 | 通知偏好 x3 | 独立功能，可并行开发 |
| P2 | 审计日志、远端频道发现 | 管理后台/频道发现功能 |
| P2 | WS 事件扩展 | 实时性优化，无 WS 回退到 HTTP 补拉 |

---

## 附录：客户端参考文件

| 文件 | 内容 |
|------|------|
| `src/features/chat/domain/ports/chatApiPort.ts` | 所有 Port 接口定义 |
| `src/features/chat/data/chat-api/httpChatApi.ts` | HTTP 适配器实现（请求构造 + 响应解析） |
| `src/features/chat/data/protocol/chatWireModels.ts` | Wire 层 snake_case 模型 |
| `src/features/chat/data/protocol/chatWireEvents.ts` | WS 事件 Wire 模型 |
| `src/features/chat/domain/types/chatApiModels.ts` | Domain 层 camelCase 模型 |
| `src/features/chat/data/protocol/chatWireMappers.ts` | Wire ↔ Domain 映射器 |
| `src/shared/mock/protocol/protocolMockTransport.ts` | 协议层 Mock（模拟服务端行为） |
