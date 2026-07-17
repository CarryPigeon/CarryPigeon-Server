# SQL 表结构与测试数据整理任务单

## 任务目标

以当前项目 Java 类、MyBatis 实体和数据库服务契约为表元数据来源，修正 `docs/sql/` 中数据库表定义与测试数据不一致或不正确的问题。

## 任务类型

实现类任务，修改正式 SQL 文档/初始化脚本，不修改架构边界。

## 影响模块

- `docs/sql/`
- `infrastructure-service/database-api`
- `infrastructure-service/database-impl`
- `chat-domain`

## 允许修改范围

- `docs/sql/*.sql`
- `docs/sql/README.md`（仅当脚本说明需要同步时）
- 任务单状态与验证记录

## 禁止边界

- 不修改 Java 领域模型、数据库 API 契约或 MyBatis 实体，除非发现必须升级为单独任务的问题。
- 不新增依赖。
- 不改变模块职责或依赖方向。
- 不引入新的数据库迁移框架或运行时机制。

## 治理文档

- `docs/architecture/架构文档.md`
- `docs/architecture/包结构规范.md`
- `docs/architecture/依赖引入规范.md`
- `docs/standards/测试规范.md`
- `docs/standards/变更审核清单.md`
- `docs/sql/README.md`

## 执行计划

1. 读取现有 SQL 脚本、数据库实体、Mapper 与 database-api record。
2. 建立当前类字段与表字段的对照。
3. 修正建表脚本和 all-in-one 汇总脚本。
4. 修正测试数据，使其满足当前模型字段、唯一性和业务基线。
5. 运行 SQL 静态检查与相关 Maven 测试。

## 验收标准

- SQL 表字段与当前 Java 元数据一致。
- 测试数据可匹配当前领域服务和数据库实现的基线假设。
- 不破坏既有模块边界。
- 记录实际修改、验证命令和残留风险。

## 实际修改

- 将 channel/message/notification 相关时间字段统一为 `DATETIME(6)`，与 Java `Instant` 元数据和 auth/user 表时间精度保持一致。
- 将 `chat_channel_invite.application_id` 调整为 `NOT NULL`，匹配 `ChannelInvite` / `ChannelInviteRecord` 中的 `long applicationId`。
- 移除 `chat_channel.system_channel_guard` 生成列，避免 SQL 额外引入 Java 类元数据之外的字段。
- 为 `chat_channel_pin` 和 `chat_mention` 补齐当前模型关系对应的外键与辅助索引。
- 同步修正 `00-all-in-one.sql` 与分领域 SQL，避免两个初始化入口漂移。
- 收紧 `10-test-data.sql` 清理逻辑，使测试账号、测试频道和测试消息可重复导入，并避免残留外键阻塞。

## 验证记录

```bash
rg -n "TIMESTAMP|system_channel_guard|application_id BIGINT NULL" docs/sql
```

结果：无匹配。

```bash
python3 - <<'PY'
from pathlib import Path
all_sql=Path('docs/sql/00-all-in-one.sql').read_text()
for p in ['03-channel.sql','04-message.sql','05-notification.sql']:
    txt=Path('docs/sql/'+p).read_text()
    missing=[line.strip() for line in txt.splitlines() if line.strip() and not line.strip().startswith('--') and line.strip() not in all_sql]
    print(p, len(missing))
PY
```

结果：`03-channel.sql 0`、`04-message.sql 0`、`05-notification.sql 0`。

```bash
mvn -pl infrastructure-service/database-api,infrastructure-service/database-impl -am test -DskipTests=false
```

结果：通过。`database-api` 51 个测试通过，`database-impl` 97 个测试通过、1 个真实 MySQL 环境测试按条件跳过。

## 残留风险

- 当前环境没有 `mysql` CLI，未直接在真实 MySQL 上执行 SQL 脚本。
- `chat_channel_pin.message_id` 外键在 `04-message.sql` 中通过 `ALTER TABLE` 添加，符合当前分文件执行顺序，但如果单独只执行 `03-channel.sql`，置顶表会先不含消息外键。
