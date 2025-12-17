# 功能说明：频道消息读状态同步

> 目标：在多端场景下，同一用户在同一频道的 **消息已读状态** 能被各端实时同步，  
> 并且接口简单、行为可预期，便于客户端实现。

---

## 1. 业务目标与约束

### 1.1 业务目标

1. 支持同一用户在多个终端（Web / Mobile / Desktop）之间同步“已读到哪”。
2. 允许前端根据自己的需要定义“已读”的时间点，但服务端作为 **权威存储**。
3. 提供简单稳定的接口用于：
   - 更新读状态；
   - 查询当前读状态；
   - 基于时间戳计算未读消息数量。

### 1.2 设计约束

- 不强制依赖服务端的读状态来计算未读数，避免耦合太重；
- 读状态只以 **时间戳（毫秒）** 表示，不依赖具体消息 id；
- 服务端保证读状态 **只前进不后退**，避免脏数据覆盖；
- 所有接口都要求用户必须是对应频道的成员。

---

## 2. 数据结构与存储

### 2.1 领域模型：`CPChannelReadState`

文件：`api/src/main/java/team/carrypigeon/backend/api/bo/domain/channel/read/CPChannelReadState.java`

```java
public class CPChannelReadState {
    private long id;            // 主键
    private long uid;           // 用户 id
    private long cid;           // 频道 id
    private long lastReadTime;  // 最新已读时间（毫秒），0 表示从未读
}
```

含义：

- 每条记录代表一个 `(uid, cid)` 对的读状态；
- `lastReadTime` 是该用户在该频道中“已读到”的时间点。

### 2.2 数据持久化表（建议）

表名：`channel_read_state`

关键字段（简要）：

- `id BIGINT PRIMARY KEY`
- `uid BIGINT NOT NULL`
- `cid BIGINT NOT NULL`
- `last_read_time BIGINT NOT NULL DEFAULT 0`

索引建议：

- 唯一索引：`UNIQUE (uid, cid)`  
  支持最常见的按 `(uid, cid)` 查询；
- 可选索引：`INDEX (cid)`  
  如果未来需要按频道维度做统计，可以考虑。

### 2.3 DAO 抽象

接口：`ChannelReadStateDao`  
文件：`api/src/main/java/team/carrypigeon/backend/api/dao/database/channel/read/ChannelReadStateDao.java`

核心方法：

- `CPChannelReadState getByUidAndCid(long uid, long cid)`
- `boolean save(CPChannelReadState state)`
- `boolean delete(CPChannelReadState state)`

默认实现支持 Spring Cache + Redis 缓存。

---

## 3. 对外接口设计

读状态涉及 3 条主要路由（均为 Netty 路由）：

1. `/core/channel/message/unread/get`  
   - 基于 **客户端提供的时间戳** 计算未读数；
   - 不直接依赖读状态表。
2. `/core/channel/message/read/state/update`  
   - 更新服务端存储的 `(uid, cid)` 读状态；
   - 成功后广播给该用户所有在线会话。
3. `/core/channel/message/read/state/get`  
   - 查询服务端存储的 `(uid, cid)` 读状态；
   - 新设备上线时，可以用它获取“起点”。

具体请求/响应结构见：`doc/api/api-chat-message.md`。

### 3.1 获取未读消息数量 `/core/channel/message/unread/get`

语义：

- 输入：`cid`, `startTime`（毫秒）
- 输出：`count`（startTime 之后该用户在该频道中的有效消息数量）

约束：

- 完全基于客户端传入的 `startTime` 计算；
- 不自动使用服务端的读状态表。

典型用途：

- 前端手动维护一个“认为自己读到的时间”，以此为基准获取未读数。

### 3.2 更新读状态 `/core/channel/message/read/state/update`

语义：

- 将 `(uid, cid)` 的 `lastReadTime` 更新为客户端上报值，只允许前进；
- 成功之后，服务端向所有该用户的在线会话广播一条读状态变更通知。

约束：

- 需要登录，且必须是频道成员；
- 若客户端上报的时间 `t_new <= t_old`，则忽略此次更新。

### 3.3 查询读状态 `/core/channel/message/read/state/get`

语义：

- 查询服务端记录的 `(uid, cid)` 的 `lastReadTime`；
- 若不存在记录，则返回 `lastReadTime = 0`。

典型用途：

- 新设备上线时，用于获取同步起点；
- 断线重连后，用于校正本地状态。

---

## 4. 服务端内部链路设计（LiteFlow）

### 4.1 更新读状态链 `/core/channel/message/read/state/update`

LiteFlow 链配置（示意）：

```xml
<chain name="/core/channel/message/read/state/update">
    THEN(
    UserLoginChecker,
    RenameArg.data("RenameScript","ChannelMemberInfo_Uid:SessionId;ChannelMemberInfo_Cid:ChannelReadStateInfo_Cid"),
    CPChannelMemberSelector.bind("key","CidWithUid"),
    CPChannelReadStateUpserter,
    CPUserSelfCollector,
    CPChannelReadStateNotifyBuilder,
    CPNotifier.bind("route","/core/channel/message/read/state")
    )
</chain>
```

关键节点说明：

- `UserLoginChecker`
  - 校验登录态，写入 `SessionId`；
- `RenameArg`
  - 将 `SessionId` 和 `ChannelReadStateInfo_Cid` 映射为成员选择所需的参数；
- `CPChannelMemberSelector`
  - 校验用户确实是该频道成员；
- `CPChannelReadStateUpserter`
  - 根据 `(uid, cid)` 读写读状态表，保证 `lastReadTime` 单调递增；
- `CPUserSelfCollector`
  - 基于 `SessionId` 收集当前用户 uid，写入 `Notifier_Uids`；
- `CPChannelReadStateNotifyBuilder`
  - 从 `ChannelReadStateInfo` 构建 `CPChannelReadStateNotificationData`，写入 `Notifier_Data`；
- `CPNotifier`
  - 调用 `CPNotificationService` 向该用户所有在线 `CPSession` 推送通知。

### 4.2 查询读状态链 `/core/channel/message/read/state/get`

LiteFlow 链配置（示意）：

```xml
<chain name="/core/channel/message/read/state/get">
    THEN(
    UserLoginChecker,
    RenameArg.data("RenameScript","ChannelMemberInfo_Uid:SessionId;ChannelMemberInfo_Cid:ChannelReadStateInfo_Cid"),
    CPChannelMemberSelector.bind("key","CidWithUid"),
    CPChannelReadStateSelector
    )
</chain>
```

`CPChannelReadStateSelector` 行为：

- 输入：`SessionId`, `ChannelReadStateInfo_Cid`
- 查询：`ChannelReadStateDao.getByUidAndCid(uid, cid)`
- 若不存在记录：创建 `lastReadTime = 0` 的临时对象返回；
- 输出：`ChannelReadStateInfo`，供 Result 组装响应。

---

## 5. 客户端推荐时序

### 5.1 新设备首次进入频道

1. 建立连接，完成鉴权；
2. 调用 `/core/channel/message/read/state/get`，得到：

```json
{
  "cid": 12345,
  "uid": 67890,
  "lastReadTime": 1700000000000
}
```

3. 以 `lastReadTime` 为基准，调用 `/core/channel/message/unread/get` 获取未读数；
4. 根据实际产品需求，拉取历史消息（可用 `/core/channel/message/list`）。

### 5.2 阅读新消息

1. 用户在当前设备上阅读了一部分消息，前端本地更新“最后阅读时间”；
2. 以最新阅读时间调用 `/core/channel/message/read/state/update`：

```json
{
  "cid": 12345,
  "lastReadTime": 1700001000000
}
```

3. 服务端更新 `(uid, cid)` 的 `lastReadTime` 后，向该用户所有在线会话广播通知：

```json
{
  "cid": 12345,
  "uid": 67890,
  "lastReadTime": 1700001000000
}
```

4. 其他设备收到通知后：
   - 更新本地缓存的读状态；
   - 更新 UI（未读数、已读标记等）。

### 5.3 重新进入频道

1. 客户端本地可以缓存 `lastReadTime`，也可以再次调用 `/read/state/get` 校验；
2. 需要展示未读数时，以 `lastReadTime` 调用 `/unread/get`。

---

## 6. 与未读统计的关系

设计上刻意分离：

- 读状态表：用于 **多端同步**，保证服务端能给出一个权威的 `lastReadTime`；
- 未读统计接口：保持 **简单**，只依赖调用时传入的时间戳。

这带来的好处：

- 读状态可以有更多内部用途（例如推荐、统计）而不影响现有接口；
- 客户端可以在特殊场景下使用自定义的基准时间（而不是强制依赖服务端值）；
- 即使读状态暂时不可用，未读统计接口仍然能继续工作。

---

## 7. 后续扩展方向

未来如需扩展读状态功能，可以考虑：

- 增加按频道列表批量获取读状态的接口（例如返回所有频道的 `lastReadTime` 映射）；
- 在通知中增加更多信息（例如最近一条已读消息的摘要）；
- 基于读状态做更加细粒度的“已读回执”（例如每条消息的已读用户列表）。

这些扩展可以在保持当前接口向后兼容的前提下，逐步实现。 
