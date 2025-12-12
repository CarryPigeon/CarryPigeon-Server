# 机器人端开发指南（Bot Client）

> 本文面向「机器人端」开发者：  
> 希望以程序方式连接 CarryPigeon 后端，作为“用户”参与频道、收发消息、同步读状态。

---

## 1. 总体能力与定位

机器人端在协议上与普通客户端**完全一致**，区别仅在于：

- 使用固定的机器人账号（普通用户或预留的“机器人用户”）；
- 通过代码实现自动登录、收发消息、处理通知等。

机器人端可以：

- 登录 / 登出；
- 创建/管理频道（若有权限）；
- 在频道中发送消息、拉取历史消息、统计未读数；
- 更新读状态，并利用通知实现多进程/多实例同步；
- 监听服务端推送的消息通知与读状态通知，做自动回复或业务处理。

---

## 2. 连接与握手（Netty + 加密）

机器人端需要按以下步骤与服务器建立加密连接：

1. **建立 TCP 连接**
   - 地址：按部署配置；
   - 端口：`connection.port`（默认示例为 `7609`，具体见运维配置）。

2. **握手阶段（ECC + AES）**
   - 客户端生成 ECC 密钥对；
   - 向服务器发送 JSON 格式的 `CPECCKeyPack`：

```json
{
  "id": 1,
  "key": "<Base64-encoded-ECC-public-key>"
}
```

   - 服务器返回 JSON 格式的 `CPAESKeyPack`：

```json
{
  "id": 1,
  "sessionId": 123456789,
  "key": "<Base64-encoded-encrypted-AES-key>"
}
```

   - 客户端使用自己的 ECC 私钥解密 `key` 字段，得到 AES 会话密钥；
   - 记住 `sessionId`，后续业务包需要写入 AAD。

3. **业务阶段（AES-GCM + AAD）**
   - 之后所有请求的 payload 为：

```text
+---------+---------+--------------+
| nonce   | AAD     | cipherText   |
+---------+---------+--------------+
  12 B      20 B       (n-32) B
```

   - `nonce`：12 字节随机数；全 0 用于心跳；
   - `AAD`：由服务器约定的结构：
     - 4 字节：包序号（int）；
     - 8 字节：`sessionId`；
     - 8 字节：包时间戳（毫秒）。
   - `cipherText`：AES-GCM 加密的业务 JSON 文本（下文的 `CPPacket`）。

服务器会校验：

- 包序号递增；
- sessionId 一致；
- 时间戳在合理时间窗口；
- 解密成功且 JSON 格式正确。

> 建议：  
> 机器人端实现一个“会话对象”，负责维护：
> - 当前 AES key；  
> - 当前 sessionId；  
> - 当前包序号（每发一包自增）；  
> - 心跳与重连逻辑。

---

## 3. 请求与响应协议（CPPacket / CPResponse）

### 3.1 请求：CPPacket

业务 JSON 包结构：

```json
{
  "id": 12345,
  "route": "/core/user/login/token",
  "data": {
    "token": "xxx.yyy.zzz"
  }
}
```

- `id`: long，请求 id，用于将响应与请求对应；
- `route`: string，业务路由，例如 `/core/channel/message/create`；
- `data`: object，业务参数，结构由对应 VO 定义。

### 3.2 响应：CPResponse

服务器响应结构：

```json
{
  "id": 12345,
  "code": 200,
  "data": { ... }
}
```

- `id`: 与请求的 `id` 相同；
- `code`:
  - `200`：成功；
  - `100`：参数/业务错误；
  - `300`：权限错误；
  - `404`：路由不存在；
  - `500`：服务器内部错误；
- `data`: 业务响应实体（由对应 Result 定义）。

机器人端一般会：

- 为每个发送的请求维护一个 `id → 回调` 的映射；
- 当收到响应时，根据 `id` 分发到对应回调。

---

## 4. 机器人常用业务接口

详细字段见：

- 用户/频道：`doc/api/api-user-channel.md`
- 消息/读状态：`doc/api/api-chat-message.md`

这里给出机器人常用的一组：

### 4.1 登录（推荐：token 登录）

1. 通过邮箱登录或注册获取 token（一次性操作，人工或脚本）：
   - `/core/user/register`
   - `/core/user/login/email`

2. 机器人正常运行时使用 token 登录：

```json
{
  "id": 1,
  "route": "/core/user/login/token",
  "data": {
    "token": "your-bot-token"
  }
}
```

成功后：

- 服务器在 `CPSession` 中记录当前用户 id；
- 后续路由通过 `UserLoginChecker` 使用此信息。

### 4.2 获取频道与成员信息

- 获取频道资料：`/core/channel/profile/get`
- 获取频道成员列表：`/core/channel/member/list`

机器人通常会：

- 在启动时或定期拉取自己关心的频道列表与成员列表；
- 用于构建内部路由或权限判断（例如只在特定频道自动回复）。

### 4.3 发送消息到频道

路由：`/core/channel/message/create`

请求示例：

```json
{
  "id": 2,
  "route": "/core/channel/message/create",
  "data": {
    "cid": 12345,
    "domain": "Core:Text",
    "data": { "text": "hello from bot" }
  }
}
```

响应：

```json
{
  "id": 2,
  "code": 200,
  "data": { "mid": 10 }
}
```

> 注意：机器人必须是该频道成员，且未被禁言。

### 4.4 拉取消息与未读数

- 拉取消息列表：`/core/channel/message/list`
  - 根据 `startTime` 和 `count` 拉取历史消息；
- 获取未读数：`/core/channel/message/unread/get`
  - 传入一个时间戳，统计之后的未读消息数量。

机器人可以：

- 定期通过 `list` 拉取最近消息进行分析；
- 使用 `unread/get` 结合自己的“最后处理时间”来计算是否有新消息需要处理。

### 4.5 同步读状态

读状态相关接口：

- 更新读状态：`/core/channel/message/read/state/update`
- 查询读状态：`/core/channel/message/read/state/get`

推荐用法：

1. 机器人启动后：
   - 对每个关心的 `cid` 调用 `/read/state/get`，拿到 `lastReadTime`；
   - 用作初次拉取消息和未读统计的基准。

2. 处理完新消息后：

```json
{
  "id": 3,
  "route": "/core/channel/message/read/state/update",
  "data": {
    "cid": 12345,
    "lastReadTime": 1700000000000
  }
}
```

3. 后端会将 `(uid, cid)` 的 `lastReadTime` 更新为较大的值，并广播通知到该用户所有会话。

---

## 5. 处理服务端推送（通知）

### 5.1 通知封装格式

通知通过 `CPResponse` 发送，特点：

- `id = -1`：表示这是一个“推送”，不是某个请求的响应；
- `code = 0`：表示通知类型；
- `data` 中封装了 `CPNotification`：

```json
{
  "id": -1,
  "code": 0,
  "data": {
    "route": "/core/message",
    "data": { ... }
  }
}
```

机器人需要区分：

- `code != 0` → 这是请求响应；
- `code == 0` → 这是通知（需根据内层 `route` 处理）。

### 5.2 消息通知 `/core/message`

payload 类型：`CPMessageNotificationData`：

```json
{
  "route": "/core/message",
  "data": {
    "sContent": "short text summary",
    "cid": 12345,
    "uid": 67890,
    "sendTime": 1700000000000
  }
}
```

- 新消息通知与删除消息通知都会使用 `/core/message` route，只是 `sContent` 不同（例如 `"message deleted"`）。

机器人典型处理：

- 监听 `/core/message` 通知；
- 根据 `cid`、`uid` 和 `sendTime` 定位消息；
- 视业务需要决定是否自动回复或执行其他动作。

### 5.3 读状态通知 `/core/channel/message/read/state`

payload 类型：`CPChannelReadStateNotificationData`：

```json
{
  "route": "/core/channel/message/read/state",
  "data": {
    "cid": 12345,
    "uid": 67890,
    "lastReadTime": 1700000000000
  }
}
```

机器人用途示例：

- 若机器人以多个实例运行（多个进程或机器），可以订阅自己用户的读状态通知；
- 当某实例更新读状态后，其他实例通过通知同步本地缓存，避免重复处理消息。

---

## 6. 会话与重连建议

机器人通常是长时间运行的进程，需考虑：

1. **心跳**
   - 定期发送心跳帧（nonce 全 0 的帧，payload 可以为空或约定固定值）；
   - 若一段时间未收到服务器任何数据，可以主动探测或重连。

2. **异常断连**
   - 捕获 socket 关闭、读写异常；
   - 清理内部状态（如包序号、sessionId 等）；
   - 重新进行握手与登录。

3. **幂等性**
   - 对于可能重试的操作（如发送消息），建议在业务层做幂等控制：
     - 例如在消息体中加入业务方的 `clientMessageId`，服务端可以据此去重（如未来扩展）。

---

## 7. 开发 Checklist（机器人端）

在开发机器人端时，可以参考以下清单：

- [ ] 实现握手流程（ECC + AES-GCM），正确处理 sessionId 与包序号；
- [ ] 实现 CP 协议编解码（`CPPacket` / `CPResponse`）；
- [ ] 支持同时处理“请求响应”和“通知”两类消息；
- [ ] 实现 token 登录，并在连接建立后自动登录；
- [ ] 为常用频道维护本地缓存（频道资料、成员列表）；
- [ ] 正确使用 `/message/list`、`/unread/get`、`/read/state/update`、`/read/state/get`；
- [ ] 订阅并处理 `/core/message` 与 `/core/channel/message/read/state` 通知；
- [ ] 实现心跳与自动重连逻辑；
- [ ] 在日志中记录关键字段（uid/cid/route/id），便于排查问题。

通过遵守上述约定，你可以将机器人端视为普通客户端的一种，实现稳定、可观测且易于维护的自动化业务流程。 

