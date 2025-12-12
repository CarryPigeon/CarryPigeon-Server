# 用户与频道相关 API 说明

> 本文对用户与频道相关的主要路由做集中说明，补充 `doc/api.md` 中 3.1 和 3.2 小节的内容。

---

## 1. 统一说明

- 所有路由使用统一的 JSON 协议：
  - 请求外层：`CPPacket { id, route, data }`
  - 响应外层：`CPResponse { id, code, data }`
- 状态码：
  - `200`：成功
  - `100`：参数错误 / 业务错误
  - `300`：权限错误
  - `404`：路由不存在
  - `500`：服务器内部错误

---

## 2. 用户相关路由

### 2.1 用户注册 `/core/user/register`

- VO：`CPUserRegisterVO`
- 路由：`/core/user/register`
- 不需要登录。

请求体：

```json
{
  "email": "string",
  "code": 123456
}
```

- `email`: string，用户邮箱；
- `code`: int，邮箱验证码（需要先通过 `/core/service/email/send` 发送）。

响应：

- Result：`CPUserRegisterResult`

```json
{
  "token": "string"
}
```

- `token`: string，注册完成后发放的登录 token。

### 2.2 邮箱登录 `/core/user/login/email`

- VO：`CPUserEmailLoginVO`
- 路由：`/core/user/login/email`
- 不需要登录。

请求体：

```json
{
  "email": "string",
  "code": 123456
}
```

响应：

- Result：`CPUserEmailLoginResult`

```json
{
  "token": "string"
}
```

### 2.3 Token 登录 `/core/user/login/token`

- VO：`CPUserTokenLoginVO`
- 路由：`/core/user/login/token`

请求体：

```json
{
  "token": "string"
}
```

响应：

- Result：`CPUserTokenLoginResult`

```json
{
  "token": "string",
  "uid": 12345
}
```

说明：

- 返回的 `token` 可以是刷新后的 token；
- `uid` 为当前登录用户 id。

### 2.4 Token 登出 `/core/user/login/token/logout`

- VO：`CPUserTokenLogoutVO`
- 路由：`/core/user/login/token/logout`

请求体：

```json
{
  "token": "string"
}
```

响应：

- 默认成功结果，无 data，成功时 `code = 200`。

### 2.5 获取用户资料 `/core/user/profile/get`

- VO：`CPUserGetProfileVO`
- 路由：`/core/user/profile/get`
- 可用于查询自己或其他用户的公开信息。

请求体：

```json
{
  "uid": 12345
}
```

响应：

- Result：`CPUserGetProfileResult`

```json
{
  "username": "string",
  "avatar": 0,
  "email": "string",
  "sex": 0,
  "brief": "string",
  "birthday": 0
}
```

说明：

- `avatar`: long，头像 id；
- `sex`: int，性别枚举；
- `birthday`: long，生日时间戳（毫秒）。

### 2.6 更新用户资料 `/core/user/profile/update`

- VO：`CPUserUpdateProfileVO`
- 路由：`/core/user/profile/update`
- 需要登录。

请求体：

```json
{
  "username": "string?",
  "avatar": 0?,
  "sex": 0?,
  "brief": "string?",
  "birthday": 0?
}
```

说明：

- 字段为可选，客户端仅传需要更新的字段；
- 服务端会将这些字段更新到当前登录用户的资料中。

响应：

- 默认成功结果。

### 2.7 更新邮箱 `/core/user/profile/update/email`

- VO：`CPUserUpdateEmailProfileVO`
- 路由：`/core/user/profile/update/email`
- 需要登录，并通过验证码校验。

请求体：

```json
{
  "email": "new@example.com",
  "code": 123456
}
```

响应：

- 默认成功结果。

---

## 3. 频道相关路由

### 3.1 创建频道 `/core/channel/create`

- VO：`CPChannelCreateVO`
- 路由：`/core/channel/create`
- 需要登录。

请求体：

```json
{
  "name": "My Channel",
  "brief": "some description",
  "avatar": 0
}
```

响应：

- Result：`CPChannelCreateResult`

```json
{
  "cid": 12345
}
```

### 3.2 删除频道 `/core/channel/delete`

- VO：`CPChannelDeleteVO`
- 路由：`/core/channel/delete`
- 需要登录，且必须是频道所有者。

请求体：

```json
{
  "cid": 12345
}
```

响应：

- 默认成功结果。

### 3.3 获取频道资料 `/core/channel/profile/get`

- VO：`CPChannelGetProfileVO`
- 路由：`/core/channel/profile/get`

请求体：

```json
{
  "cid": 12345
}
```

响应：

- Result：`CPChannelGetProfileResult`

```json
{
  "name": "My Channel",
  "owner": 10001,
  "brief": "some description",
  "avatar": 0,
  "createTime": 1700000000000
}
```

### 3.4 更新频道资料 `/core/channel/profile/update`

- VO：`CPChannelUpdateProfileVO`
- 路由：`/core/channel/profile/update`
- 需要登录，且必须是频道所有者。

请求体：

```json
{
  "cid": 12345,
  "name": "New Name?",
  "owner": 10002?,
  "brief": "new brief?",
  "avatar": 1?
}
```

响应：

- 默认成功结果。

---

## 4. 频道成员与申请、禁言

### 4.1 频道成员

- 列表成员 `/core/channel/member/list`
  - VO：`CPChannelListMemberVO`
  - 请求：

```json
{
  "cid": 12345
}
```

  - 响应：`CPChannelListMemberResult`

```json
{
  "count": 2,
  "members": [
    { "uid": 1, "name": "A", "authority": 0, "joinTime": 1700000000000 },
    { "uid": 2, "name": "B", "authority": 0, "joinTime": 1700000000000 }
  ]
}
```

- 删除成员 `/core/channel/member/delete`
  - VO：`CPChannelDeleteMemberVO`
  - 请求：

```json
{
  "cid": 12345,
  "uid": 67890
}
```

  - 响应：默认成功结果。

### 4.2 频道申请

路由列表：

- 创建申请：`/core/channel/application/create`
- 处理申请：`/core/channel/application/process`
- 列出申请：`/core/channel/application/list`

列表接口响应形状（`CPChannelListApplicationResult`）：

```json
{
  "count": 1,
  "applications": [
    {
      "id": 1,
      "uid": 123,
      "state": 0,
      "msg": "hello",
      "applyTime": 1700000000000
    }
  ]
}
```

### 4.3 频道禁言

路由列表：

- 创建禁言：`/core/channel/ban/create`
- 删除禁言：`/core/channel/ban/delete`
- 列出禁言：`/core/channel/ban/list`

列表接口响应形状（`CPChannelListBanResult`）：

```json
{
  "count": 1,
  "bans": [
    {
      "uid": 123,
      "duration": 3600
      // 其他字段见 CPChannelListBanResultItem
    }
  ]
}
```

---

## 5. 与消息、读状态的关系

用户与频道 API 与消息/读状态 API 的关系：

- 用户登录接口提供 `uid` 与 `token`，供后续所有消息与频道操作使用；
- 频道与成员相关接口保证：
  - 只有频道成员才能发送消息、更新读状态、拉取未读数；
  - 管理员/所有者才能执行管理动作（删除频道、删成员、禁言等）。

客户端通常的调用顺序：

1. 使用 `/core/user/login/email` 或 `/core/user/login/token` 完成登录；
2. 使用频道相关接口获取/管理频道；
3. 使用消息相关接口（见 `api-chat-message.md`）进行消息收发、读状态同步和未读统计。 

