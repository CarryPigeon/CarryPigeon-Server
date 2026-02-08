# 领域模型说明（Domain Models）

> 本文总结 `api` 模块中定义的核心领域模型，  
> 帮助你在阅读业务代码或设计接口时快速理解实体语义。

---

## 1. 用户与认证

### 1.1 用户 `CPUser`

路径：`api/.../domain/user/CPUser.java`

字段：

- `id: long` — 用户唯一 id；
- `username: String` — 用户名；
- `avatar: long` — 头像资源 id；
- `email: String` — 邮箱；
- `sex: CPUserSexEnum` — 性别枚举（0 未知，1 男，2 女）；
- `brief: String` — 简介；
- `birthday: LocalDateTime` — 生日；
- `registerTime: LocalDateTime` — 注册时间。

用途：

- 作为 DAO 与业务层之间的用户信息载体；
- VO/Result 可根据需要暴露其中部分字段。

### 1.2 用户 Token `CPUserToken`

路径：`api/.../domain/user/token/CPUserToken.java`

字段：

- `id: long` — token 记录 id；
- `uid: long` — 用户 id；
- `token: String` — 登录 token；
- `expiredTime: LocalDateTime` — 过期时间。

用途：

- 登录状态管理与多端登录控制；
- 被 `/api/users/me`、`/api/auth/revoke` 等路由使用。

---

## 2. 频道与成员

### 2.1 频道 `CPChannel`

路径：`api/.../domain/channel/CPChannel.java`

字段：

- `id: long` — 频道 id；
- `name: String` — 频道名称；
- `owner: long` — 频道所有者的用户 id（若为 -1 表示系统固有频道）；
- `brief: String` — 简介；
- `avatar: long` — 频道头像资源 id；
- `createTime: LocalDateTime` — 创建时间。

### 2.2 频道成员 `CPChannelMember`

路径：`api/.../domain/channel/member/CPChannelMember.java`

字段：

- `id: long` — 成员记录 id；
- `uid: long` — 成员用户 id；
- `cid: long` — 频道 id；
- `name: String` — 群昵称；
- `authority: CPChannelMemberAuthorityEnum` — 权限（普通成员/管理员）；
- `joinTime: LocalDateTime` — 加入时间。

用途：

- 权限判断（管理员/成员）；
- 频道成员列表展示、通知推送目标计算。

### 2.3 频道申请 `CPChannelApplication`

路径：`api/.../domain/channel/application/CPChannelApplication.java`

字段：

- `id: Long` — 申请记录 id；
- `uid: Long` — 申请人 id；
- `cid: Long` — 频道 id；
- `state: CPChannelApplicationStateEnum` — 状态（0 待处理，1 通过，2 拒绝）；
- `msg: String` — 申请附言；
- `applyTime: LocalDateTime` — 申请时间。

用途：

- 频道加入申请流程；
- 管理员审批列表。

### 2.4 频道禁言 `CPChannelBan`

路径：`api/.../domain/channel/ban/CPChannelBan.java`

字段：

- `id: long` — 禁言记录 id；
- `cid: long` — 频道 id；
- `uid: long` — 被禁言用户 id；
- `aid: long` — 执行禁言的管理员 id；
- `duration: int` — 禁言时长（秒）；
- `createTime: LocalDateTime` — 创建时间。

方法：

- `boolean isValid()` — 判断禁言是否仍然有效（当前时间是否在禁言期内）。

---

## 3. 消息与文件

### 3.1 消息 `CPMessage`

路径：`api/.../domain/message/CPMessage.java`

字段：

- `id: long` — 消息 id；
- `uid: long` — 发送者用户 id；
- `cid: long` — 频道 id；
- `domain: String` — 消息域，格式 `Domain:SubDomain`，如 `Core:Text`；
- `data: JsonNode` — 消息内容（泛型 JSON）；
- `sendTime: LocalDateTime` — 发送时间。

用途：

- 作为消息 DAO 与业务层的统一载体；
- 与 `MessagePO` 双向转换。

### 3.2 文件信息 `CPFileInfo`

路径：`api/.../domain/file/CPFileInfo.java`

字段：

- `id: long` — 文件记录 id；
- `sha256: String` — 文件内容的 SHA-256 哈希；
- `size: long` — 文件大小（字节）；
- `objectName: String` — 对象存储中的对象名（例如 MinIO 的 key）；
- `contentType: String` — MIME 类型（可选）；
- `createTime: LocalDateTime` — 创建时间。

用途：

- 文件上传/下载 token 申请时的元信息管理；
- 客户端可通过这些字段展示文件属性。

---

## 4. 读状态 `CPChannelReadState`

路径：`api/.../domain/channel/read/CPChannelReadState.java`

字段：

- `id: long` — 记录 id；
- `uid: long` — 用户 id；
- `cid: long` — 频道 id；
- `lastReadTime: long` — 最新已读时间（毫秒时间戳，0 表示从未读）。

用途：

- 记录用户在每个频道的读状态，用于多端同步；
- 与消息未读统计组合使用，参见：
  - `doc/features/feature-message-read-state.md`
  - `doc/api/11-HTTP端点清单.md`

---

## 5. 与数据库表的关系（简要）

领域模型与数据库表的映射关系通过 PO 类（`*PO`）体现。常见映射：

- `CPUser` ↔ `UserPO` ↔ 表 `user`
- `CPUserToken` ↔ `UserTokenPO` ↔ 表 `user_token`
- `CPChannel` ↔ `ChannelPO` ↔ 表 `channel`
- `CPChannelMember` ↔ `ChannelMemberPO` ↔ 表 `channel_member`
- `CPChannelApplication` ↔ `ChannelApplicationPO` ↔ 表 `channel_application`
- `CPChannelBan` ↔ `ChannelBanPO` ↔ 表 `channel_ban`
- `CPMessage` ↔ `MessagePO` ↔ 表 `message`
- `CPFileInfo` ↔ `FileInfoPO` ↔ 表 `file_info`
- `CPChannelReadState` ↔ `ChannelReadStatePO` ↔ 表 `channel_read_state`

每个 PO 类负责：

- 将数据库字段映射为领域模型字段；
- 在 `toBo` / `fromBo` 方法中进行必要的类型转换（例如 int ↔ enum、JSON ↔ String）。

更详细的表结构说明见：`doc/domain/database-schema.md`。 

