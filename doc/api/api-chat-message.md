# Chat 消息相关 API 说明

> 本文对消息相关的所有路由做集中说明，补充 `doc/api.md` 中 3.3 节的内容，方便前后端协同。

---

## 1. 统一说明

- 所有路由走统一的 JSON 协议：
  - 外层包：`CPPacket { id, route, data }`
  - 内层响应：`CPResponse { id, code, data }`
- 所有路由要求：
  - 需要登录的接口，会在说明中标出；
  - `code = 200` 为成功，`100/300/404/500` 分别为参数错误、权限错误、路由不存在、服务异常。

---

## 2. 发送频道消息 `/core/channel/message/create`

### 2.1 请求

- VO：`CPMessageCreateVO`
- 路由：`/core/channel/message/create`
- 需要登录，且用户必须是频道成员，且未被禁言。

请求体（`CPPacket.data`）：

```json
{
  "cid": 12345,
  "domain": "Core:Text",
  "data": { "text": "hello" }
}
```

- `cid`: long，频道 id
- `domain`: string，消息域，形如 `Domain:SubDomain`（例如 `Core:Text`）
- `data`: object，具体消息内容，结构由 `domain` 决定

### 2.2 响应

- Result：`CPMessageCreateResult`

```json
{
  "mid": 1
}
```

- `mid`: long，新消息 id

---

## 3. 删除频道消息 `/core/channel/message/delete`

### 3.1 请求

- VO：`CPMessageDeleteVO`
- 路由：`/core/channel/message/delete`
- 需要登录，且只有满足权限（消息发送者或频道管理员/所有者）才能删除。

请求体：

```json
{
  "mid": 1
}
```

- `mid`: long，要删除的消息 id

### 3.2 响应

- Result：默认结果（`CPControllerDefaultResult`），成功时 `code = 200`，`data = null`。

---

## 4. 拉取频道消息列表 `/core/channel/message/list`

### 4.1 请求

- VO：`CPMessageListVO`
- 路由：`/core/channel/message/list`
- 需要登录，且用户必须是频道成员。

请求体：

```json
{
  "cid": 12345,
  "start_time": 0,
  "count": 50
}
```

- `cid`: long，频道 id
- `start_time`: long，起始时间（毫秒时间戳）  
  语义：返回 `send_time <= start_time` 的最新若干条消息；
- `count`: int，本次拉取的最大条数，范围 `[1, 50]`。

### 4.2 响应

- Result：`CPMessageListResult`（具体结构见 `doc/api.md` 对应章节），本质上是消息列表及分页信息。

---

## 5. 获取未读消息数量 `/core/channel/message/unread/get`

### 5.1 请求

- VO：`CPMessageGetUnreadVO`
- 路由：`/core/channel/message/unread/get`
- 需要登录，且用户必须是频道成员。

请求体：

```json
{
  "cid": 12345,
  "start_time": 1700000000000
}
```

- `cid`: long，频道 id
- `start_time`: long，从该时间点（毫秒）之后统计未读消息数量

> 注意：  
> 未读数统计完全基于 **前端传入的时间戳**，不会自动使用服务端的读状态表。  
> 典型用法是前端以“最后一次已读时间”作为 `start_time`。

### 5.2 响应

- Result：`CPMessageGetUnreadResult`

```json
{
  "count": 10
}
```

- `count`: long，未读消息数量

---

## 6. 更新读状态 `/core/channel/message/read/state/update`

用于同步「当前用户在某频道的最新已读时间」，并广播给该用户所有在线会话。

### 6.1 请求

- VO：`CPMessageReadStateUpdateVO`
- 路由：`/core/channel/message/read/state/update`
- 需要登录，且用户必须是频道成员。

请求体：

```json
{
  "cid": 12345,
  "last_read_time": 1700000000000
}
```

- `cid`: long，频道 id
- `last_read_time`: long，最新已读时间（毫秒时间戳）

约束：

- 服务端内部维护 `CPChannelReadState(uid, cid, lastReadTime)`（对外 JSON 字段为 `last_read_time`）；
- 每次更新时，只有当 `last_read_time` **大于** 之前记录的值时才会覆盖（只前进不后退）。

### 6.2 响应

- Result：默认成功结果（`CPControllerDefaultResult`），成功时 `code = 200`。

### 6.3 广播通知

当读状态更新成功后，服务端会向该用户所有在线会话推送一条通知：

- 通知 route：`/core/channel/message/read/state`
- 通知 payload：`CPChannelReadStateNotificationData`

```json
{
  "cid": 12345,
  "uid": 67890,
  "last_read_time": 1700000000000
}
```

- `cid`: long，频道 id
- `uid`: long，用户 id
- `last_read_time`: long，最新已读时间（毫秒）

客户端策略建议：

- 所有端收到该通知后，应更新本地缓存的 `(uid, cid)` 对应 `last_read_time`；
- 后续调用 `/unread/get` 时，可以以该时间为基准计算未读数。

---

## 7. 查询读状态 `/core/channel/message/read/state/get`

用于在新设备或重新登录时获取当前用户在某个频道的服务端记录的最新读状态。

### 7.1 请求

- VO：`CPMessageReadStateGetVO`
- 路由：`/core/channel/message/read/state/get`
- 需要登录，且用户必须是频道成员。

请求体：

```json
{
  "cid": 12345
}
```

- `cid`: long，频道 id

### 7.2 响应

- Result：`CPMessageReadStateGetResult`

```json
{
  "cid": 12345,
  "uid": 67890,
  "last_read_time": 1700000000000
}
```

语义：

- 若没有记录，则服务端会返回 `last_read_time = 0`；
- 否则返回最新一次 `/read/state/update` 或内部更新记录的时间。

### 7.3 与未读数的组合使用建议

在多端同步场景下，推荐策略：

1. 新设备上线：
   - 调用 `/core/channel/message/read/state/get` 拿到 `last_read_time`；
   - 根据业务需要，加载历史消息列表。
2. 展示未读数：
   - 以 `last_read_time` 作为 `start_time` 调用 `/core/channel/message/unread/get`；
3. 用户阅读新消息后：
   - 以本地最新阅读时间调用 `/core/channel/message/read/state/update`；
   - 所有设备收到 `/core/channel/message/read/state` 通知后更新本地状态。

这样可以在保持 **服务端作为权威读状态存储** 的前提下，让未读数计算仍然简单地基于“前端传入时间戳”完成。 
