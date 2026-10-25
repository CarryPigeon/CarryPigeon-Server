# 持久化与 SQL

**状态：草稿（待评审）**
**读者：** 编写 database-api、database-impl、domain Repository 的开发者
**边界：** 本文规定数据库访问的分层职责、API 形态、命名、事务与 SQL 约束。表结构本身以 `docs/sql/` 为准；错误码见 `docs/standards/异常与错误码.md`。

---

## 1. 总原则

数据库访问分为三层：

```text
domain Repository（业务语义）
        ↓
database-api（表级元能力）
        ↓
database-impl（MyBatis / JDBC / SQL）
```

硬规则：

1. **database-api 不承载业务规则**。它不知道“已发送”“已读”“只能前进”“成员才可见”等语义。
2. **业务数据库操作由 domain Repository 编排**。Repository 决定状态过滤、字段选择、状态流转和查询组合。
3. **能用元能力实现的 API 不单独定义**。只有元能力组合会产生明显性能损耗，或属于搜索/跨表读模型时，才允许专用 API。
4. **database-impl 不反向依赖 chat-domain**，不使用 `ProblemException` 或 feature 错误码。
5. **事务边界在 domain**，由 `TransactionRunner` 控制；database-api/impl 不自行开启事务。
6. **SQL 只出现在 database-impl**；domain Repository 不出现 MyBatis、JDBC、表名或列名。

---

## 2. 通用元能力清单

本节是**强制基线**，规范优先于当前调用情况。每张表的 database-api 都必须提供下列 8 个基础方法，即使当前业务还没有用到：

| 方法 | 说明 |
|---|---|
| `insert(Record)` | 写入一行 |
| `insertAll(List<Record>)` | 批量写入 |
| `delete(key)` | 按主键删除 |
| `deleteAll(List<key>)` | 按主键批量删除 |
| `update(Record)` | 更新完整行，不筛选业务列 |
| `list()` | 查询全表 |
| `findById(key)` | 按主键查询 |
| `findByIds(List<key>)` | 按主键批量查询 |

主键形态：

- 单列主键：`key` 使用 `long`，例如 `delete(long messageId)`、`findByIds(List<Long> messageIds)`。
- 复合主键：使用稳定的 `*Key` record，例如 `ChannelReadStateKey(channelId, accountId)`，对应 `delete(ChannelReadStateKey)`、`findByIds(List<ChannelReadStateKey>)`。

约束：

- `update` 更新完整行。若 Repository 只想修改部分列，由 Repository 构造完整记录，不能把“只更新某几列”的业务策略放进 database-impl。
- 通用元能力必须齐全，不能因为“当前没有调用方”而缺失；规范优先于实际使用。
- 例外：仅追加的技术台账（如 `plugin_schema_history`）不适用 8 项基线；只提供 `insert`、按业务键 `find` 和存储初始化，禁止 `update`、`delete`，以保持审计不可变。
- 额外 API 才按需添加：`updateAll`、`upsert`、`find(Query)`、`findForUpdate`、`deleteByRelation`、`search`、跨表读模型等。
- `find(Query)` 的 Query 只能包含数据谓词，例如主键、外键、状态值、关键字、排序方向、limit；不能包含权限判断、时间线语义或业务动作。

---

## 3. Query 对象规范

Query 是 database-api 的数据查询契约，不是业务命令。

允许：

- 主键 / 外键过滤；
- 状态、类型等列值过滤；
- ID 范围、游标、排序方向、limit；
- 通用关键字（仅限专用搜索 API）。

禁止：

- `unreadOnly`、`markAsRead`、`advanceIfNewer` 这类业务语义命名；
- 在 Query 上实现方法或默认业务规则；
- 把权限、成员资格、角色判断放进 Query；
- 让 Query 依赖 chat-domain 类型。

业务到数据的转换由 Repository 完成：

```text
unreadOnly = true  →  read = false
status = sent      →  Repository 传入 "sent"
before/after 语义  →  Repository 决定 beforeMessageId / afterMessageId 和 ascending
```

---

## 4. Repository 的职责

Repository 是 domain 层的业务语义边界，必须负责：

- 业务状态过滤与状态流转；
- 字段选择、字段合并；
- 时间线方向、分页游标、搜索条件组合；
- 已读位置“只前进不后退”等规则；
- 领域模型与 database-api Record 的双向转换；
- 未读等读模型的业务解释。

Repository 不得：

- 直接写 SQL；
- 依赖 MyBatis/JDBC；
- 把业务规则再次下推给 database-api；
- 只是机械透传 database-api 方法而没有业务职责。

---

## 5. 允许的专用 API 例外

### 5.1 搜索

`search` 可以作为专用 API。原因：关键字匹配、跨 JSON 字段搜索和排序通常无法用简单 `findById` 组合，且组合会产生明显性能损耗。

要求：

- 搜索条件由 Repository 传入，包括状态、关键字、范围；
- database-impl 不硬编码业务状态常量；
- 搜索字段和排序策略必须在 API 注释中写明；
- 返回的是表记录或明确的读模型，不是领域模型。

### 5.2 跨表读模型

跨表聚合（如未读统计）允许作为专用读模型 API，前提是元能力组合会产生明显的 N+1 或全表扫描。

要求：

- 接口名和注释明确标注“读模型”；
- 只读，不承担写操作；
- 不包含权限判断；权限由 Repository/domain 先完成；
- 读模型 record 不放在通用表记录中混用，命名为 `*UnreadRecord`、`*View` 等；
- 必须有明确的性能理由，不能因为“写 SQL 方便”而滥用。

### 5.3 原子并发操作

如果业务需要“读-比较-写”原子性，优先使用通用锁定读 `findForUpdate`，由 Repository 在事务内完成比较和写入。

只有在锁定读无法满足、且专用原子 SQL 明显更安全或更高效时，才保留专用原子 API；必须在接口注释中说明并发语义。

---

## 6. Record 与 Entity

- `*Record` 是 database-api 的表行投影，只承载列字段，不写业务方法。
- `*Entity` 是 database-impl 的 MyBatis 映射对象，不暴露到 database-api。
- `*UnreadRecord` 等读模型不是表行，必须单独命名并说明用途。
- Record 字段与表列保持一一对应；JSON 文本字段仍以 `String` 传递，序列化由 adaptor 完成。
- 不在 Record 上放校验、状态机、计算属性或 feature 错误码。

---

## 7. SQL 规范

- 优先使用 MyBatis-Plus 条件构造器表达简单查询。
- 显式 SQL 只用于：
  - UPSERT；
  - 批量写入/更新；
  - 跨表聚合读模型；
  - 条件构造器无法表达的数据库特性。
- 显式 SQL 不得包含业务状态硬编码，例如 `status = 'sent'`；状态值应由参数传入。
- 显式 SQL 必须使用参数绑定，不拼接用户输入。
- `LIMIT` 只接受受控的整数值，不直接拼接外部字符串。
- 表名、列名以 `docs/sql/` 为准；改表必须同步脚本、Entity、Record、Mapper。

---

## 8. 事务与并发

- 事务由 domain 的 `TransactionRunner` 开启。
- database-api/impl 不标注 `@Transactional`。
- 锁定读必须在事务内调用；Repository 负责在锁定读后完成比较和写入。
- 批量写优先使用 `updateAll` 或单条批量 SQL，避免明显 N+1。
- 不在 database-api 中实现“状态只能前进”等业务条件；该判断属于 Repository。

---

## 9. 命名规范

| 场景 | 命名 |
|---|---|
| 按主键查询 | `findById` |
| 批量主键查询 | `findByIds` |
| 通用条件查询 | `find(Query)` |
| 单行写入 | `insert` |
| 批量写入 | `insertAll` |
| 完整更新 | `update` |
| 全表查询 | `list` |
| 按主键删除 | `delete` |
| 按主键批量删除 | `deleteAll` |
| 批量写入/更新（额外） | `updateAll` |
| 按关系删除（额外） | `deleteBy<Relation>` |
| 锁定读（额外） | `findByIdForUpdate` |
| 专用搜索（额外） | `search...` |
| 跨表读模型（额外） | `find...Unread...` |

禁止的 database-api 命名：

- `markAsRead`、`markAllAsRead`
- `advanceIfNewer`
- `findByChannelIdBefore` / `findByChannelIdAfter`（应由 Query 表达）
- 任何带角色、权限、成员、时间线业务含义的方法名

这些命名应出现在 Repository，而不是 database-api。

---

## 10. 参考实现与覆盖范围

| 表服务 | 主键 | 8 项基线 | 额外能力 |
|---|---|---|---|
| `AuthAccountDatabaseService` | `id` | 已齐全 | `findByUsername`、`findByEmail` |
| `AuthRefreshSessionDatabaseService` | `id` | 已齐全 | `revoke`、`revokeIfActive` |
| `UserProfileDatabaseService` | `account_id` | 已齐全 | — |
| `ChannelDatabaseService` | `channel_id` | 已齐全 | `findSystemChannels`、`discoverChannels` |
| `ChannelMemberDatabaseService` | `member_id` | 已齐全 | `findByRelation`、`deleteByChannelId`、按频道/账户查询 |
| `ChannelApplicationDatabaseService` | `application_id` | 已齐全 | 按频道/申请人查询 |
| `ChannelBanDatabaseService` | `ban_id` | 已齐全 | 按频道/账户查询 |
| `MessageDatabaseService` | `message_id` | 已齐全 | `find(MessageQuery)`、搜索 |
| `MentionDatabaseService` | `mention_id` | 已齐全 | `updateAll`、`deleteByMessageId`、`find(MentionQuery)` |
| `ChannelReadStateDatabaseService` | `(channel_id, account_id)` | 已齐全，使用 `ChannelReadStateKey` | `upsert`、`findByIdForUpdate`、未读读模型 |
| `PluginMigrationDatabaseService` | `(plugin_id, migration_version)` | append-only 例外 | `ensureHistoryStorage`、按业务键 `find` |

### Message

### Message

- 通用元能力：`insert`、`insertAll`、`delete`、`deleteAll`、`update`、`list`、`findById`、`findByIds`。
- 额外查询：`find(MessageQuery)`；专用搜索 `searchByChannelId(...)`，`status` 由 Repository 传入。
- Repository：`DatabaseBackedMessageRepository` 决定 status、before/after 和排序。

### Mention

- 通用元能力：`insert`、`insertAll`、`delete`、`deleteAll`、`update`、`list`、`findById`、`findByIds`。
- 额外能力：`updateAll`、`deleteByMessageId`、`find(MentionQuery)`。
- Repository：`markAsRead` 使用 `findById + update`；`markAllAsRead` 使用 `find + updateAll`。

### ChannelReadState

- 通用元能力：`insert`、`insertAll`、`delete(ChannelReadStateKey)`、`deleteAll(List<ChannelReadStateKey>)`、`update`、`list`、`findById(ChannelReadStateKey)`、`findByIds(List<ChannelReadStateKey>)`。
- 额外能力：`upsert`、`findByIdForUpdate`。
- Repository：`advanceIfNewer` 使用锁定读后比较并 `upsert`。
- 专用读模型：`findUnreadsByAccountId`，属于跨表聚合性能例外。

---

## 11. 评审清单

新增或修改 database 代码时逐项检查：

- [ ] `insert`、`insertAll`、`delete`、`deleteAll`、`update`、`list`、`findById`、`findByIds` 是否齐全？
- [ ] 复合主键是否使用稳定的 `*Key` record 表达 `delete` / `findById` / `findByIds`？
- [ ] database-api 是否只表达表级数据访问？
- [ ] 是否能用现有元能力组合？组合是否有明显性能损耗？
- [ ] 业务状态、权限、时间线语义是否在 Repository？
- [ ] `update` 是否完整行更新？
- [ ] 是否出现硬编码业务常量或业务 SQL 条件？
- [ ] 专用 API 是否标注了 search / 读模型 / 并发例外？
- [ ] 是否在 database-impl 中依赖 chat-domain 类型？
- [ ] 是否在 Repository 中出现 SQL、表名或 MyBatis 类型？
- [ ] 批量操作是否避免 N+1？
- [ ] 是否需要同步 `docs/sql/`？
