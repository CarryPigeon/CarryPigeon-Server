# DAO 与缓存模块说明

> 本文介绍本项目的数据库访问（DAO）与缓存策略，帮助你在扩展或替换 DAO 时有统一的参考。

---

## 1. 总体结构与分层

数据访问相关代码主要分布在两个模块：

1. **API 模块（稳定接口层）**
   - 包路径：`team.carrypigeon.backend.api.dao.*`
   - 职责：
     - 定义 DAO 接口（例如 `ChannelMessageDao`, `ChannelReadStateDao`）；
     - 暴露给上层业务代码和插件使用。

2. **DAO 模块（默认实现层）**
   - 包路径：`team.carrypigeon.backend.dao.database.*`
   - 职责：
     - 使用 MyBatis-Plus 将 DAO 接口映射到具体数据库实现；
     - 提供数据库 PO（`*PO`）、Mapper（`*Mapper`）、实现类（`*DaoImpl`）；
     - 结合 Spring Cache + Redis 做缓存。

这样的分层带来两个好处：

- 上层业务只依赖 `api` 模块中的接口，对底层存储透明；
- 侵入性插件可以通过实现 `api` 中的 DAO 接口来替换默认实现。

---

## 2. MyBatis-Plus 使用方式

### 2.1 配置

数据库配置类：  
`dao/src/main/java/team/carrypigeon/backend/dao/database/CPDaoDatabaseConfiguration.java`

```java
@Configuration
@MapperScan({"team.carrypigeon.backend.dao.database.mapper"})
public class CPDaoDatabaseConfiguration {
    @PostConstruct
    public void init() {
        log.info("CPDaoDatabaseConfiguration initialized, mapperScan='team.carrypigeon.backend.dao.database.mapper'");
    }
}
```

说明：

- 所有 Mapper（`*Mapper`）都放在 `team.carrypigeon.backend.dao.database.mapper` 下；
- 使用 MyBatis-Plus 的 `BaseMapper<PO>` 提供基础 CRUD 能力。

### 2.2 典型模式

以消息 DAO 为例：

- PO：

```java
@TableName("message")
public class MessagePO {
    @TableId
    private Long id;
    private Long uid;
    private Long cid;
    private String domain;
    private String domainVersion;
    private String data;
    private LocalDateTime sendTime;

    public CPMessage toBo() { ... }
    public static MessagePO fromBo(CPMessage message) { ... }
}
```

- Mapper：

```java
public interface MessageMapper extends BaseMapper<MessagePO> {
}
```

- DAO 实现：

```java
@Slf4j
@Service
public class MessageDaoImpl implements ChannelMessageDao {

    private final MessageMapper messageMapper;

    @Override
    public CPMessage getById(long id) {
        return Optional.ofNullable(messageMapper.selectById(id))
                .map(MessagePO::toBo)
                .orElse(null);
    }

    @Override
    public CPMessage[] listBefore(long cid, long cursorMid, int count) {
        long cursor = cursorMid <= 0 ? Long.MAX_VALUE : cursorMid;
        LambdaQueryWrapper<MessagePO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MessagePO::getCid, cid)
                .lt(MessagePO::getId, cursor)
                .orderByDesc(MessagePO::getId)
                .last("LIMIT " + count);
        ...
    }
}
```

要点：

- 所有对外暴露的类型是 `CP*` 领域模型（在 `api` 模块中定义）；
- DAO 实现负责在 BO 与 PO 之间做转换；
- 查询使用 `LambdaQueryWrapper`，避免硬编码列名。

---

## 3. DAO 接口与默认实现（概览）

### 3.1 用户与认证

- `UserDao`（API）
  - `dao/.../impl/user/UserDaoImpl.java`

- `UserTokenDao`（API）
  - `dao/.../impl/user/token/UserTokenDaoImpl.java`

### 3.2 频道与成员

- `ChannelDao`
  - `dao/.../impl/channel/ChannelDaoImpl.java`

- `ChannelMemberDao`
  - `dao/.../impl/channel/member/ChannelMemberDaoImpl.java`

- `ChannelBanDAO`
  - `dao/.../impl/channel/ban/ChannelBanDaoImpl.java`

- `ChannelApplicationDAO`
  - `dao/.../impl/channel/application/ChannelApplicationDaoImpl.java`

### 3.3 消息与读状态

- `ChannelMessageDao`
  - `dao/.../impl/message/MessageDaoImpl.java`

- `ChannelReadStateDao`
  - `dao/.../impl/channel/read/ChannelReadStateDaoImpl.java`

上述实现类都符合统一模式：  
使用 Mapper 访问数据库 + Optional + 日志 + Spring Cache 注解。

---

## 4. 缓存配置与策略

### 4.1 Redis 缓存配置

配置类：`dao/src/main/java/team/carrypigeon/backend/dao/cache/RedisConfig.java`

主要 Bean：

- `RedisTemplate<String, Object>`：用于通用对象存储；
- `StringRedisTemplate`：用于简单字符串存储；
- `RedisCacheManager`：作为 Spring Cache 的 CacheManager。

`RedisCacheManager` 默认配置：

- 过期时间：1 小时（可根据需要调整）；
- key 使用 `String` 序列化；
- value 使用 `RedisSerializer.java()`；
- 禁止缓存 `null`（`disableCachingNullValues`）。

### 4.2 启用缓存

全局启用缓存：

- `dao` 模块中的 `RedisConfig` 使用了 `@EnableCaching`；
- `application-starter` 模块中的 `CacheConfig` 也标记了 `@EnableCaching`；

这使得所有 DAO 实现上的 `@Cacheable` / `@CacheEvict` 注解生效。

### 4.3 常见缓存模式

以 `ChannelMemberDaoImpl` 为例：

- 查询缓存：

```java
@Cacheable(cacheNames = "channelMemberById", key = "#id")
public CPChannelMember getById(long id) { ... }

@Cacheable(cacheNames = "channelMembersByCid", key = "#cid")
public CPChannelMember[] getAllMember(long cid) { ... }

@Cacheable(cacheNames = "channelMemberByUidCid", key = "#uid + ':' + #cid")
public CPChannelMember getMember(long uid, long cid) { ... }
```

- 写操作清缓存：

```java
@CacheEvict(cacheNames = {
    "channelMemberById",
    "channelMembersByCid",
    "channelMemberByUidCid",
    "channelMembersByUid"
}, allEntries = true)
public boolean save(CPChannelMember channelMember) { ... }
```

读状态 DAO 同样模式：

- `channelReadStateById` / `channelReadStateByUidCid` 两个 cacheNames；
- `save` / `delete` 时 `allEntries = true` 做全量失效。

---

## 5. 测试环境的 InMemory DAO

在 `chat-domain` 模块的测试中，为减少外部依赖并提升测试速度，使用了一套 **内存实现**：

- 配置类：`chat-domain/src/test/java/.../support/dao/InMemoryDaoConfig.java`
- 存储：`chat-domain/src/test/java/.../support/InMemoryDatabase.java`

特点：

- 实现同样的 DAO 接口（例如 `ChannelMessageDao`, `ChannelReadStateDao`）；
- 使用简单的 `Map` / `List` 结构存储数据；
- 不涉及真实数据库或 Redis。

用途：

- 让 LiteFlow 链路测试可以在纯内存环境下运行；
- 不参与生产环境，避免对性能和行为产生影响。

---

## 6. 替换与扩展 DAO 的建议

对于侵入性插件或内部重构，需要替换或扩展 DAO 实现时，建议遵循：

1. **接口兼容**
   - 实现必须与 `api` 模块中的 DAO 接口签名一致；
   - 行为尽量与默认实现保持一致（尤其是异常和返回值语义）。

2. **使用 `@Primary` 或条件装配**
   - 在新实现类上添加 `@Primary`，或：

```java
@Primary
@ConditionalOnProperty(prefix = "cp.read-state.dao", name = "mode", havingValue = "distributed")
public class DistChannelReadStateDaoImpl implements ChannelReadStateDao { ... }
```

   - 这样可以通过配置切换使用哪个实现。

3. **充分利用缓存**
   - 对高频只读接口继续使用 `@Cacheable`；
   - 写操作后注意清理或更新对应缓存；
   - 如需细粒度控制，可设计更具体的 cacheNames 与 key。

4. **日志与监控**
   - 在新 DAO 实现中保留必要的 debug / warn 日志；
   - 对高频方法增加监控（耗时、错误率），便于定位性能瓶颈。

---

## 7. 与其他模块的协作

- `chat-domain`：
  - 所有业务 LiteFlow 节点通过 DAO 接口访问数据；
  - 替换或扩展 DAO 实现不会影响节点代码，只要接口兼容。

- `connection`：
  - 连接层不直接访问 DAO，但它承载的是所有业务调用的入口；
  - DAO 的性能与稳定性会直接影响整体请求延迟和稳定性。

- 插件模块：
  - 拓展性插件可安全读取 DAO 提供的数据（注意写入权限）；
  - 侵入性插件可实现新的 DAO 模块，并通过 Spring 装配接管数据访问。

通过遵守以上约定，可以在保持系统稳定性的同时，对 DAO 层进行持续优化与演进。 
