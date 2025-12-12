# 性能优化与实践建议

> 本文汇总当前项目中几个主要的性能关注点，并给出建议做法。  
> 重点覆盖：消息与读状态、DAO 与索引、缓存、LiteFlow 链路、通知与连接层。

---

## 1. 消息与读状态相关性能

### 1.1 消息表查询与索引

核心 DAO：`ChannelMessageDao`  
实现：`dao/.../impl/message/MessageDaoImpl.java`

关键查询：

- `getBefore(long cid, LocalDateTime time, int count)`  
  - 条件：`cid = ? AND send_time <= ?`，按 `send_time DESC` 排序并 limit。
- `getAfterCount(long cid, long uid, LocalDateTime time)`  
  - 条件：`cid = ? AND uid = ? AND send_time > ?`。

索引建议（表 `message`）：

- `(cid, send_time)`：加速 `getBefore`；
- `(cid, uid, send_time)`：加速 `getAfterCount`。

注意：

- `count` 范围被 VO 限制为 `[1, 50]`，避免一次拉取过多消息；
- 如需支持“历史消息很深”的跳转，建议先通过时间 / id 做粗筛，再分页拉取。

### 1.2 读状态表访问

读状态 DAO：`ChannelReadStateDao`  
实现：`ChannelReadStateDaoImpl` + Redis 缓存。

索引建议（表 `channel_read_state`）：

- `UNIQUE (uid, cid)`：支持 `getByUidAndCid`；
- 可选 `(cid)`：如果未来会按频道做聚合统计。

注意：

- 读状态接口访问频率相对较低（主要在读状态更新与同步时），配合缓存即可满足需求；
- 未读统计接口仍然只基于时间戳统计，不会对读状态表产生高频压力。

### 1.3 JSON 数据处理

消息数据存储为 JSON 字符串：`MessagePO.data`  
转换逻辑：

- `MessagePO.toBo()`：`String -> JsonNode`；
- `MessagePO.fromBo()`：`JsonNode -> String`。

性能建议：

- 对于只需要统计数量的接口（未读数），已经只做 `COUNT`，不会走 JSON 反序列化，这是正确的；
- 若未来增加“消息概览”接口且访问量较大，可考虑：
  - 在表中冗余一列简短摘要（如 `s_content`），避免每次解析整个 `data`；
  - 对于冷数据分页查询，结合缓存或只在需要展示细节时解析 JSON。

---

## 2. DAO 与缓存策略

### 2.1 Spring Cache + Redis

配置：

- Redis 缓存管理：`dao/.../cache/RedisConfig.java`
- 全局启用缓存：`application-starter/.../config/CacheConfig.java`

DAO 常见模式：

- `@Cacheable(cacheNames = "...", key = "...")` 用于只读查询；
- `@CacheEvict(..., allEntries = true)` 用于写操作时清理缓存。

建议：

- 对“高频读取、低频写入”的实体（用户、频道、成员、读状态等）使用缓存是合适的；
- 对于写操作，`allEntries = true` 是安全但粗暴的做法，后续可以按需改为：
  - 精确删除对应 key，减少缓存穿透；
  - 必要时按实体粒度拆分 cacheNames。

### 2.2 InMemory DAO 用于测试

`chat-domain` 测试使用 `InMemoryDatabase` + InMemory DAO 实现，避免真实 DB 压力。  
这对性能没有直接影响，但确保测试阶段不会因为外部依赖导致性能误判。

---

## 3. LiteFlow 链路设计

LiteFlow 是目前所有业务链路的编排核心，性能主要受以下因素影响：

1. 链路长度（节点数量）；
2. 单节点执行时间；
3. 节点中是否存在阻塞外部调用。

建议：

- 把节点拆分为“逻辑清晰”的粒度即可，不必过度微粒化（避免链路过长）；
- 对于纯数据搬运节点（如简单 Rename/Setter），可以视情况与邻近节点合并；
- 在性能敏感链路（高 QPS），确保每个节点：
  - 不做重型 IO；
  - 不做长时间锁等待；
  - 出错时快速失败或降级。

调试建议：

- 结合 `CPNodeComponent` 的日志，关注：
  - 常见错误的集中节点；
  - 某些链路执行时间异常的节点（可在日志中增加链路耗时统计）。

---

## 4. 通知与会话管理

通知服务：`CPNotificationService`  
会话中心：`CPSessionCenterService`

### 4.1 通知广播成本

当前通知模型：

- 按 uid 维度获取所有 `CPSession`；
- 对每个 session 写一条 JSON 响应。

潜在瓶颈：

- 某个 uid 对应的会话数量过多；
- 通知频率过高（例如频繁读状态更新）。

优化建议：

- 控制单用户最大会话数：
  - 在 `CPSessionCenterService.addSession` 中，对某个 uid 的 session 数量设定上限（例如 5），必要时移除最老的；
- 对高频通知做限频或合并：
  - 对某些“非关键通知”（如频繁读状态变更）可以：
    - 在前端做节流（减少 update 调用频率）；
    - 或在服务端做简单合并（例如一定时间窗口内只发送最新状态）。

### 4.2 写入性能

通知写入本质上是对 Netty channel 的写操作：

- 文本大小通常较小（包一层 `CPResponse` + `CPNotification`）；
- 性能更多取决于网络与客户端处理能力。

建议：

- 在高并发场景下，监控：
  - 写队列长度；
  - 发送失败/超时次数；
- 若出现瓶颈，可以考虑：
  - 限制每秒通知条数；
  - 对某些非关键通知降级为“本地处理，不推送”。

---

## 5. 连接层与加密

连接模块：`connection`

### 5.1 Netty 帧协议

- 使用 2 字节长度前缀 + payload；
- 单帧大小限制为 64KB（由 `NettyDecoder` 控制）。

建议：

- 对于大文件 / 大消息不要走主协议，而是用专门的文件上传接口；
- 监控解码错误和超大帧日志，避免恶意/异常客户端拖垮服务。

### 5.2 AES-GCM + AAD 校验

- 每个业务包包含：
  - nonce（12B）
  - AAD（20B：包序列号 + sessionId + 时间戳）
  - cipherText
- 服务端会校验：
  - 序列号单调递增；
  - sessionId 匹配；
  - 时间戳在允许窗口内。

性能影响：

- AES-GCM 自身性能较好，但在高 QPS 场景下仍是固定成本；
- 严格的 AAD 校验有助于安全，但也会带来少量 CPU 开销。

建议：

- 优先保证安全性，不在 AAD 校验上妥协；
- 若需要进一步优化，可以考虑：
  - 使用硬件加速（如 AES-NI）；
  - 合理配置线程池与事件循环组大小。

---

## 6. 总结与演进方向

当前项目在性能上已经做了若干基础工作：

- 限制消息列表一次拉取数量；
- 使用 DAO + Spring Cache + Redis 组合缓存热点数据；
- LiteFlow 节点拆分清晰，便于优化单个节点；
- 连接层采用 Netty + 长连接 + AES-GCM，减少握手开销。

后续可以重点关注的方向：

1. **数据库层面**：进一步优化 `message` / `channel_member` / `channel_read_state` 等表的索引和统计；
2. **缓存精细化**：从 `allEntries = true` 逐步演进到精确驱逐；
3. **节点性能监控**：对高频 LiteFlow 链路增加耗时指标，定位慢节点；
4. **通知限流与合并**：为高频通知（如读状态更新）设计轻量级的限频策略。

随着业务发展，可以在此文档基础上持续补充更细致的性能调优实践。 

