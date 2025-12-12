# 数据库表结构说明（简要）

> 本文根据当前 MyBatis-Plus PO 定义，整理了核心业务表的结构和字段含义。  
> 实际表结构以数据库中的 DDL 为准，本文件作为阅读代码时的参考。

---

## 1. 用户与认证相关表

### 1.1 用户表 `user`

映射 PO：`UserPO`  
领域模型：`CPUser`

主要字段：

- `id BIGINT PRIMARY KEY` — 用户唯一 id；
- `username VARCHAR` — 用户名；
- `avatar BIGINT` — 头像资源 id；
- `email VARCHAR` — 邮箱；
- `sex INT` — 性别（0 未知，1 男，2 女）；
- `brief VARCHAR` — 简介；
- `birthday DATETIME` — 生日；
- `register_time DATETIME` — 注册时间。

索引建议：

- `UNIQUE (email)`：用于邮箱登录；
- `INDEX (username)`：视业务需求可选。

### 1.2 用户 Token 表 `user_token`

映射 PO：`UserTokenPO`  
领域模型：`CPUserToken`

主要字段：

- `id BIGINT PRIMARY KEY` — token 记录 id；
- `uid BIGINT NOT NULL` — 用户 id；
- `token VARCHAR NOT NULL` — token 字符串；
- `expired_time DATETIME NOT NULL` — 过期时间。

索引建议：

- `UNIQUE (token)`：根据 token 快速查询；
- `INDEX (uid)`：按用户 id 查询所有 token。

---

## 2. 频道与成员相关表

### 2.1 频道表 `channel`

映射 PO：`ChannelPO`  
领域模型：`CPChannel`

主要字段：

- `id BIGINT PRIMARY KEY` — 频道 id；
- `name VARCHAR NOT NULL` — 频道名称；
- `owner BIGINT NOT NULL` — 所有者用户 id（-1 表示系统频道）；
- `brief VARCHAR` — 简介；
- `avatar BIGINT` — 头像资源 id；
- `create_time DATETIME NOT NULL` — 创建时间。

索引建议：

- `INDEX (owner)`：按 owner 查询频道列表；
- `INDEX (create_time)`：如需按时间排序。

### 2.2 频道成员表 `channel_member`

映射 PO：`ChannelMemberPO`  
领域模型：`CPChannelMember`

主要字段：

- `id BIGINT PRIMARY KEY` — 成员记录 id；
- `uid BIGINT NOT NULL` — 用户 id；
- `cid BIGINT NOT NULL` — 频道 id；
- `name VARCHAR` — 群昵称；
- `authority INT NOT NULL` — 权限（0 成员，1 管理员）；
- `join_time DATETIME NOT NULL` — 加入时间。

索引建议：

- `INDEX (cid)`：按频道查询成员；
- `INDEX (uid)`：按用户查询所有频道成员关系；
- `UNIQUE (uid, cid)`：每个用户在频道内最多一条记录。

### 2.3 频道申请表 `channel_application`

映射 PO：`ChannelApplicationPO`  
领域模型：`CPChannelApplication`

主要字段：

- `id BIGINT PRIMARY KEY` — 申请 id；
- `uid BIGINT NOT NULL` — 申请人 id；
- `cid BIGINT NOT NULL` — 频道 id；
- `state INT NOT NULL` — 状态（0 待处理，1 通过，2 拒绝）；
- `msg VARCHAR` — 留言；
- `apply_time DATETIME NOT NULL` — 申请时间。

索引建议：

- `INDEX (uid, cid)`：按用户与频道查询申请；
- `INDEX (cid, apply_time)`：按频道分页查询申请列表。

### 2.4 频道禁言表 `channel_ban`

映射 PO：`ChannelBanPO`  
领域模型：`CPChannelBan`

主要字段：

- `id BIGINT PRIMARY KEY` — 禁言记录 id；
- `cid BIGINT NOT NULL` — 频道 id；
- `uid BIGINT NOT NULL` — 被禁言用户 id；
- `aid BIGINT NOT NULL` — 管理员 id；
- `duration INT NOT NULL` — 禁言时长（秒）；
- `create_time DATETIME NOT NULL` — 创建时间。

索引建议：

- `INDEX (cid)`：按频道查询禁言列表；
- `UNIQUE (uid, cid)`：同一频道同一用户最多一条有效禁言记录。

---

## 3. 消息与读状态相关表

### 3.1 消息表 `message`

映射 PO：`MessagePO`  
领域模型：`CPMessage`

主要字段：

- `id BIGINT PRIMARY KEY` — 消息 id；
- `uid BIGINT NOT NULL` — 发送者用户 id；
- `cid BIGINT NOT NULL` — 频道 id；
- `domain VARCHAR NOT NULL` — 消息域（`Domain:SubDomain`）；
- `data TEXT` — 消息内容 JSON；
- `send_time DATETIME NOT NULL` — 发送时间。

索引建议：

- `INDEX (cid, send_time)`：支持按时间分页拉取频道消息；
- `INDEX (cid, uid, send_time)`：支持按用户统计某时间点之后的消息数量（未读统计时使用）。

### 3.2 读状态表 `channel_read_state`

映射 PO：`ChannelReadStatePO`  
领域模型：`CPChannelReadState`

主要字段：

- `id BIGINT PRIMARY KEY` — 记录 id；
- `uid BIGINT NOT NULL` — 用户 id；
- `cid BIGINT NOT NULL` — 频道 id；
- `last_read_time BIGINT NOT NULL DEFAULT 0` — 最新已读时间（毫秒，0 表示从未读）。

索引建议：

- `UNIQUE (uid, cid)`：一个用户在一个频道中只有一条读状态记录；
- `INDEX (cid)`：如需对某频道做批量读状态统计。

---

## 4. 文件相关表

### 4.1 文件信息表 `file_info`

映射 PO：`FileInfoPO`  
领域模型：`CPFileInfo`

主要字段：

- `id BIGINT PRIMARY KEY` — 文件记录 id；
- `sha256 VARCHAR NOT NULL` — 文件内容 SHA-256 哈希；
- `size BIGINT NOT NULL` — 文件大小（字节）；
- `object_name VARCHAR NOT NULL` — 对象存储的 key；
- `content_type VARCHAR` — MIME 类型；
- `create_time DATETIME NOT NULL` — 记录创建时间。

索引建议：

- `UNIQUE (sha256)`：根据内容哈希防止重复上传；
- `INDEX (create_time)`：按时间清理或统计。

---

## 5. 其他说明

- 表结构由 MyBatis-Plus 与数据库迁移脚本共同决定，此文档仅根据当前 PO 推断字段；
- 实际部署时，应结合：
  - 具体数据库（MySQL / PostgreSQL 等）的 DDL；
  - 字段长度与字符集要求；
  - 生产环境索引规划与运维习惯；
- 新增领域模型时，应同步：
  - 在 `api` 中定义 BO；
  - 在 `dao` 中定义对应 PO 与 Mapper；
  - 在本文件中更新表结构说明。 

