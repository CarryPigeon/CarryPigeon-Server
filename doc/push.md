# 推送（通知）协议生产样例文档

本文件汇总服务端推送给客户端的几类通知的“生产样例”，包括外层 `CPResponse` 包装格式、`CPNotification` 结构以及典型的 `data` 字段内容。

> 约定：
> - 所有推送通过 Netty TCP 通道发送，使用 AES-GCM 加密；
> - 推送的外层统一是 `CPResponse`，其中：
>   - `id = -1`
>   - `code = 0`
>   - `data` 为一个 `CPNotification` 对象。

---

## 1. 通知统一外层结构

### 1.1 CPResponse 外层

推送到客户端的完整 JSON 包（在 AES 解密之后的明文）统一形态如下：

```json
{
  "id": -1,
  "code": 0,
  "data": {
    "route": "<notification-route>",
    "data": { ... }
  }
}
```

- `id`: 固定 `-1`，表示这是服务端主动推送，不需要客户端回应。
- `code`: 固定 `0`，表示“通知”而非普通业务响应。
- `data`: 为一个 `CPNotification` 对象：
  - `route`: 通知路由，用于区分通知类型（例如 `/core/message`）。
  - `data`: 该路由下的具体通知数据，结构因路由而异。

---

## 2. 握手成功通知（handshake）

### 2.1 触发时机

客户端使用服务器 ECC 公钥加密 AES 会话密钥并发送 `CPAESKeyPack` 后，服务器成功解密并保存该 AES 密钥，会立即发送一条握手成功通知。

### 2.2 明文 JSON 示例

```json
{
  "id": -1,
  "code": 0,
  "data": {
    "route": "handshake",
    "data": {
      "sessionId": 123456789
    }
  }
}
```

字段说明：

- `route = "handshake"`：固定值，表示这是“握手成功”通知。
- `data.sessionId`: long，可选，当前 Netty 会话的会话 id，用于客户端调试或日志关联。

客户端解密后只要看到 `route = "handshake"`，即可认为加密会话已建立成功，可以开始发送业务请求。

---

## 3. 频道消息通知（/core/message）

当频道内有消息变化（创建 / 删除）时，服务端会向相关用户的所有在线会话推送 `/core/message` 通知。

### 3.1 新消息通知

#### 3.1.1 场景

- 某用户在频道中发送新消息；
- 服务端在保存消息后，向该频道内所有在线成员推送。

#### 3.1.2 明文 JSON 示例

```json
{
  "id": -1,
  "code": 0,
  "data": {
    "route": "/core/message",
    "data": {
      "type": "create",
      "s_content": "你好，这是一条新消息",
      "cid": 730000000000000001,
      "uid": 720000000000000004,
      "send_time": 1734000000000
    }
  }
}
```

字段说明（`CPMessageNotificationData`）：

- `type`: `"create"`，表示新消息创建。
- `s_content`: 文本摘要，通常为消息内容截断，用于会话列表预览。
- `cid`: long，频道 id。
- `uid`: long，消息发送者用户 id。
- `send_time`: long，消息发送时间（毫秒时间戳）。

客户端收到后可选择：

- 仅用 `s_content` 在会话列表做预览；或
- 使用 `/core/channel/message/list` 拉取对应频道的最新消息做增量刷新。

### 3.2 删除消息通知

#### 3.2.1 场景

- 管理员或发送者删除某条消息；
- 服务端在删除后向相关用户推送。

#### 3.2.2 明文 JSON 示例

```json
{
  "id": -1,
  "code": 0,
  "data": {
    "route": "/core/message",
    "data": {
      "type": "delete",
      "s_content": "message deleted",
      "cid": 730000000000000001,
      "uid": 720000000000000004,
      "send_time": 1734000000000
    }
  }
}
```

字段说明：

- `type`: `"delete"`，表示消息被删除；
- `s_content`: 固定文案 `"message deleted"` 或类似的删除提示；
- 其他字段语义与“新消息通知”相同。

客户端收到后可以：

- 在消息列表中将对应消息标记为“已删除”；或
- 直接刷新该频道的消息列表。

---

## 4. 频道成员变动通知（/core/channel/member/list）

当频道成员发生变化时（加入 / 离开 / 升降级为管理员），服务端会向受影响的用户推送 `/core/channel/member/list` 通知。

### 4.1 成员加入（join）

#### 4.1.1 场景

- 普通用户入群申请被管理员批准；
- 或管理员直接添加新成员；
- 服务端向频道内相关成员推送“有新成员加入”的通知。

#### 4.1.2 明文 JSON 示例

```json
{
  "id": -1,
  "code": 0,
  "data": {
    "route": "/core/channel/member/list",
    "data": {
      "type": "join",
      "cid": 730000000000000001,
      "uid": 720000000000000010
    }
  }
}
```

字段说明（`CPChannelMemberNotificationData`）：

- `type`: `"join"`，表示有新成员加入频道。
- `cid`: long，频道 id。
- `uid`: long，新加入成员的用户 id。

客户端可以：

- 在当前成员列表中直接插入该用户；或
- 调用 `/core/channel/member/list` 拉取最新成员列表做全量刷新。

### 4.2 成员离开（leave）

#### 4.2.1 场景

- 某成员被管理员移除频道；
- 或成员主动退群；
- 服务端推送“成员离开”通知。

#### 4.2.2 明文 JSON 示例

```json
{
  "id": -1,
  "code": 0,
  "data": {
    "route": "/core/channel/member/list",
    "data": {
      "type": "leave",
      "cid": 730000000000000001,
      "uid": 720000000000000010
    }
  }
}
```

字段说明：

- `type`: `"leave"`，表示某成员离开频道；
- 其他字段同上。

### 4.3 成员角色变更（管理员增删）

#### 4.3.1 成员被设为管理员（admin_add）

```json
{
  "id": -1,
  "code": 0,
  "data": {
    "route": "/core/channel/member/list",
    "data": {
      "type": "admin_add",
      "cid": 730000000000000001,
      "uid": 720000000000000010
    }
  }
}
```

#### 4.3.2 成员被取消管理员（admin_remove）

```json
{
  "id": -1,
  "code": 0,
  "data": {
    "route": "/core/channel/member/list",
    "data": {
      "type": "admin_remove",
      "cid": 730000000000000001,
      "uid": 720000000000000010
    }
  }
}
```

字段说明：

- `type`:
  - `"admin_add"`：成员被授予管理员权限；
  - `"admin_remove"`：成员被降级为普通成员。
- `cid` / `uid`：同前。

客户端收到后可更新频道成员列表中该成员的权限标记，或直接重新拉取成员列表。

---

## 5. 读状态 / 未读数相关通知（示意）

当前仓库中读状态和未读数主要通过主动拉取实现（`/core/channel/message/read/state/get`、`/core/channel/message/unread/get`），通知部分的抽象是通用的 `CPNotification`。如果后续为读状态增加推送，推荐沿用以下结构：

### 5.1 推送路由建议

- 建议使用：`/core/channel/message/read/state/update` 作为通知路由。

### 5.2 可能的通知 payload 示例

```json
{
  "id": -1,
  "code": 0,
  "data": {
    "route": "/core/channel/message/read/state/update",
    "data": {
      "cid": 730000000000000001,
      "uid": 720000000000000004,
      "last_read_time": 1734000000000
    }
  }
}
```

字段说明：

- `cid`: long，频道 id。
- `uid`: long，更新读状态的用户 id。
- `last_read_time`: long，该用户在该频道内的最新读到时间（毫秒时间戳）。

> 说明：本节为“推荐结构示例”，具体实现需根据业务需要在 LiteFlow 链路与 `CPNotificationService` 中补充。

---

## 6. 客户端消费建议

1. 统一入口：  
   - 在解密后的 `CPResponse` 层判断：
     - `id == -1 && code == 0` → 视为通知；
     - 解析 `data.route` 再分发到各通知处理器。

2. 容错策略：  
   - 未识别的 `route` 可以先记录日志并忽略，避免因为新类型通知导致旧版本客户端崩溃。

3. 与拉取接口的配合：  
   - 推送只保证“提示有变更”，不要求绝对完备；
   - 对于重要视图（消息列表、成员列表），在处理推送时适当调用对应拉取接口做最终一致性校准。

