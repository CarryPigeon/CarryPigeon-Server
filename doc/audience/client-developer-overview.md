# 客户端 / 机器人端文档（入口概览）

> 目标读者：实现 CarryPigeon 客户端（App/Web/桌面）或机器人端（Bot）的同学。  
> 覆盖：握手协议、加密与帧格式、请求/响应、推送，以及**全量路由清单**。

## 1. 你应该优先读哪份文档

对接主文档（接口细节最完整，含请求/响应结构与示例）：`doc/audience/client-developer-guide.md`

旧入口（已迁移，保留兼容跳转）：`doc/clients/api-client.md`

机器人端实践指南：`doc/clients/bot-client-guide.md`

推送样例（生产格式示例）：`doc/push.md`

## 2. 传输与加密（握手摘要）

### 2.1 TCP 帧

- 每个包：`2 bytes length` + `n bytes payload`

### 2.2 握手（ECC → AES 会话密钥）

1. 客户端本地生成 AES 会话密钥
2. 将 AES 密钥的 Base64 文本用服务端 ECC 公钥加密
3. 发送明文 JSON（`CPAESKeyPack`）：

```json
{ "id": 1, "session_id": 0, "key": "..." }
```

4. 服务端解密成功后，会回一条 **已用该 AES 密钥加密** 的推送确认：

```json
{
  "id": -1,
  "code": 0,
  "data": { "route": "handshake", "data": { "session_id": 123456789 } }
}
```

握手细节与 AAD 结构见：`doc/audience/client-developer-guide.md`

## 3. 业务请求 / 响应

### 3.1 请求（CPPacket）

- `id`：请求 id（用于匹配响应）
- `route`：业务路由（必须与服务端一致）
- `data`：请求体对象（JSON，字段为 `snake_case`）

### 3.2 响应（CPResponse）

- `code`：
  - `200`：成功
  - `100`：参数/业务错误
  - `300`：权限错误
  - `404`：路由不存在
  - `500`：服务端错误

## 4. 推送（通知）

- 推送统一外层：`CPResponse(id=-1, code=0, data=CPNotification)`
- `CPNotification.route` 为通知类型；`CPNotification.data` 为 payload（可为 `null` 表示“刷新提示”）

推送的生产样例与字段说明：`doc/push.md`

### 4.1 推送路由清单（当前版本）

| 通知 route | 说明 |
| --- | --- |
| `handshake` | 握手成功确认（包含 `session_id`，用于写入 AAD） |
| `/core/message` | 频道消息变化通知（create/delete 等） |
| `/core/channel/message/read/state` | 读状态同步通知（同一用户多会话同步） |
| `/core/channel/list` | 频道列表变化刷新提示 |
| `/core/channel/member/list` | 成员变化刷新提示/结构化变更通知 |
| `/core/channel/application/list` | 频道申请列表变化刷新提示 |
| `/core/channel/ban/list` | 禁言列表变化刷新提示 |
| `/core/user/profile/get` | 用户资料变化刷新提示 |

## 5. 全量业务路由清单（当前版本）

> 说明：路由的请求/响应示例与字段解释以 `doc/audience/client-developer-guide.md` 为准。

### 5.1 用户

| route |
| --- |
| `/core/user/register` |
| `/core/user/login/email` |
| `/core/user/login/token` |
| `/core/user/login/token/logout` |
| `/core/user/profile/get` |
| `/core/user/profile/update` |
| `/core/user/profile/update/email` |

### 5.2 服务

| route |
| --- |
| `/core/service/email/send` |

### 5.3 频道

| route |
| --- |
| `/core/channel/create` |
| `/core/channel/delete` |
| `/core/channel/list` |
| `/core/channel/profile/get` |
| `/core/channel/profile/update` |
| `/core/channel/member/list` |
| `/core/channel/member/get` |
| `/core/channel/member/delete` |
| `/core/channel/admin/create` |
| `/core/channel/admin/delete` |
| `/core/channel/application/create` |
| `/core/channel/application/process` |
| `/core/channel/application/list` |
| `/core/channel/ban/create` |
| `/core/channel/ban/delete` |
| `/core/channel/ban/list` |

### 5.4 消息与读状态

| route |
| --- |
| `/core/channel/message/create` |
| `/core/channel/message/delete` |
| `/core/channel/message/list` |
| `/core/channel/message/get` |
| `/core/channel/message/unread/get` |
| `/core/channel/message/read/state/update` |
| `/core/channel/message/read/state/get` |

### 5.5 文件（HTTP token 申请）

| route |
| --- |
| `/core/file/upload/token/apply` |
| `/core/file/download/token/apply` |
