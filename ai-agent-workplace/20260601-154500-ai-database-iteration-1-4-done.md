任务名称：
数据库设计迭代优化 1-4 轮落地

任务目标：
在不突破既有模块边界的前提下，落实数据库优化方案第 1-4 轮：修复 schema/实现不一致，收敛频道 discover 读模型，补齐索引与查询对齐，并增强关键数据完整性约束。

任务背景：
上一轮探索任务已确认当前数据库设计存在若干高优先级问题，包括：
- `chat_channel_pin` schema 与 mapper/实体字段不一致
- message edit 字段未完整落库
- `ChannelRecord` 同时承担主表事实与 discover 投影语义
- 搜索 / mentions / pins / unreads 的索引与查询条件未完全对齐
- system channel 缺少数据库级唯一约束

用户已明确要求继续执行优化方案的第 1、2、3、4 轮。

影响模块：
- `application-starter`
- `chat-domain`
- `infrastructure-service/database-api`
- `infrastructure-service/database-impl`
- `ai-agent-workplace`

允许修改范围：
- 允许新增 Flyway 迁移脚本
- 允许调整 `database-api` 中与频道 discover 读模型相关的最小契约
- 允许调整 `database-impl` 中 mapper/entity/service 实现
- 允许调整 `chat-domain` 中对应 persistence adapter 与 discover 读链路
- 允许补充或调整受影响测试
- 允许更新当前任务单

禁止修改范围：
- 不修改模块依赖方向
- 不引入新的架构模式或新第三方依赖
- 不把 `chat-domain` 改为直接依赖 `*-impl`
- 不并行扩大到 outbox、分库分表、搜索中间件替换等超范围方案

依赖限制：
- 仅使用当前已引入的 Spring Boot、MyBatis-Plus、JUnit、Mockito 等依赖

配置限制：
- 不新增面向业务的新配置项
- 如需数据库行为修复，优先通过迁移脚本与 SQL 收敛，而不是引入新开关

文档依据：
- `AGENTS.md`
- `docs/架构文档.md`
- `docs/包结构规范.md`
- `docs/AI协作开发规范.md`
- `docs/变更审核清单.md`
- `docs/任务单模板.md`

任务分解 / 执行计划：
1. 建立实现任务单并确认受影响边界。
2. 第 1 轮：新增迁移脚本并修复 pin/message 等 schema 与实现不一致问题。
3. 第 2 轮：将频道 discover 聚合投影从 `ChannelRecord` 主事实契约中拆出。
4. 第 3 轮：补齐与查询语义匹配的索引，并收敛相关 SQL。
5. 第 4 轮：补 system channel 唯一约束与必要的数据完整性治理。
6. 更新受影响单测并执行针对性验证。
7. 自检后关闭任务单。

关键假设与依赖：
- 允许通过新增 Flyway 迁移脚本修复当前 schema 缺口。
- `database-api` 的 discover 读模型拆分属于“最小契约收敛”，不构成新增架构模式。
- 当前 notification preference 的 `muted_until BIGINT` 在本轮先不强行改型到 `TIMESTAMP`，除非为了完成 1-4 轮出现直接阻塞。

实现要求：
- 所有正式代码变更必须落在对应模块内。
- 迁移脚本必须保持可追加，不重写历史脚本事实。
- `database-api` 调整应尽量最小，只收敛已确认的语义混用点。
- 保持现有异常语义与主要业务行为稳定。

测试要求：
- 至少覆盖 pin/message 修复路径
- 覆盖频道 discover 读模型拆分后的成功路径
- 覆盖 system channel 唯一性 / discover / message edit 相关关键回归

质量门禁：
- 受影响模块编译与相关测试通过
- 新增迁移脚本与代码实现在仓库内逻辑一致
- 任务单记录实际结果、验证记录、残留风险

复审要求：
- 本任务涉及数据持久化、schema 迁移与 `database-api` 契约调整，完成后需重点复审：
  - 架构边界是否仍正确
  - schema 与 mapper 是否重新一致
  - discover 读模型是否不再污染主事实契约
  - 索引与 SQL 是否匹配

文档要求：
- 若未引入新的长期规则，不回写 `docs/`

验收标准：
- pin/message 的显著 schema/实现不一致已修复
- 频道 discover 不再依赖 `ChannelRecord` 混合承载聚合投影
- 关键查询已补齐匹配索引或已按新模型重写
- system channel 数据库唯一性得到硬约束
- 相关测试与验证已记录

完成定义：
- 代码、迁移、测试和任务单都已完成
- 质量门禁已执行并记录
- 任务单已从 `current` 收口为 `done`

实际结果：
- 已完成第 1-4 轮目标落地。
- 已通过追加 Flyway 迁移修复 `chat_channel_pin` schema/实体不一致，并补齐 message edit 字段更新落库。
- 已将频道 discover 读模型从 `ChannelRecord` / `Channel` 主事实模型中拆分为独立的 `ChannelDiscoverRecord` / `DiscoveredChannel`。
- 已补齐 pin / mention / message 过滤相关索引，并新增 system channel 唯一约束与关键外键。
- 已同步更新 `chat-domain`、`database-api`、`database-impl`、`application-starter` 相关测试与测试夹具。
- 为完成当前环境下验证，补充了 `database-impl` 与 `application-starter` 的 Mockito `mock-maker-subclass` 测试配置，修复了 `StarterRegressionConfiguration` 的消息实时发布器测试桩签名，并为 starter 内存测试运行时预置 system channel。

验证记录：
- 通过：`mvn -o -pl chat-domain -am -Dtest=ChannelDiscoverApplicationServiceTests,ChannelDiscoverControllerTests,DatabaseBackedChannelRepositoryTests,MessageApplicationServicePinsTests -Dsurefire.failIfNoSpecifiedTests=false test -DskipTests=false -DtrimStackTrace=false`
- 结果：`Tests run: 11, Failures: 0, Errors: 0, Skipped: 0`
- 通过：`mvn -o -pl infrastructure-service/database-impl -am test -DskipTests=false -DtrimStackTrace=false`
- 结果：`Tests run: 86, Failures: 0, Errors: 0, Skipped: 0`
- 通过：`mvn -o -pl application-starter -am -Dtest=ApplicationStarterSmokeTests,AuthPersistenceConfigurationTests,InitializationCheckConfigurationTests,OpenApiConfigurationTests,UserProfilePersistenceConfigurationTests,MessageAttachmentRegressionTests -Dsurefire.failIfNoSpecifiedTests=false test -DskipTests=false -DtrimStackTrace=false`
- 结果：`Tests run: 14, Failures: 0, Errors: 0, Skipped: 0`
- 说明：在当前沙箱中，若直接执行更大范围的 `chat-domain` 全量测试，`RealtimeServerConfigurationContextTests` 会因禁止打开服务端 socket 报环境型失败；该问题与本轮数据库改动无关，受影响链路已通过定向验证。

残留风险：
- `V18__strengthen_channel_integrity.sql` 中基于生成列的 system channel 唯一约束仍需在真实目标 MySQL 版本上完成一次实际迁移验证；当前仓库测试以单元/装配测试为主，未在本地真实数据库实例执行 Flyway。
- 本轮未处理 notification `muted_until BIGINT` 与时间语义统一问题，仍留待后续数据库设计迭代。
- 本轮未扩大到全文搜索、中间件替换、conversation 拆表、outbox 等超范围议题。

知识沉淀 / 是否回写 docs：
- 暂无。

产物清理与保留说明：
- 保留本任务单作为本次实现任务追溯记录。

补充说明：
- 若实施中发现需要引入全文搜索中间件、conversation 独立表或 outbox，需要中止本任务并单独确认。
