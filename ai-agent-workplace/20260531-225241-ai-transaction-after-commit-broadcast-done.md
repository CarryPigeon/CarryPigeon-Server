任务名称：

统一事务后广播机制并收敛频道实时广播

任务目标：

在不引入 outbox 的前提下，为当前项目建立最小统一的事务后广播机制，并将 `channel` feature 中仍处于事务体内的 realtime 广播收敛到事务提交后执行。

任务背景：

- 当前 `message` 主链路已基本在事务成功后执行 realtime 广播
- 当前 `channel` feature 仍有多处在 `runInTransaction(...)` 内直接调用 realtime 发布器
- 用户已明确要求基于当前代码实现一版“最小统一事务后广播机制”

影响模块：

- `chat-domain`
- `infrastructure-service/database-api`
- `infrastructure-service/database-impl`
- `ai-agent-workplace/`

允许修改范围：

- 允许修改事务抽象契约
- 允许修改 Spring 事务适配实现
- 允许修改 `channel` feature 应用服务与相关测试
- 允许在 `ai-agent-workplace/` 记录任务过程

禁止修改范围：

- 不引入 outbox、消息队列或新外部依赖
- 不扩展为跨实例持久化事件补偿系统
- 不修改模块依赖方向

依赖限制：

- 仅使用现有 Spring 事务能力、现有 realtime 发布器与现有测试设施

配置限制：

- 不新增配置项

文档依据：

- `docs/架构文档.md`
- `docs/异常与错误码规范.md`
- `docs/测试规范.md`
- `docs/AI协作开发规范.md`
- `docs/变更审核清单.md`

任务分解 / 执行计划：

1. 复核当前事务入口与事务内广播点，确定统一收敛方式。
2. 为 `TransactionRunner` 增加最小事务后回调契约，并在 Spring 实现中对齐提交后执行语义。
3. 将 `ChannelApplicationService` 的 realtime 广播迁移到事务后执行。
4. 补充或更新契约测试，覆盖成功提交与回滚不广播的核心行为。
5. 运行受影响测试并记录结果。
6. 自检后关闭任务单。

关键假设与依赖：

- 本轮目标是“单进程内提交后一致性”的最小收敛，不承诺跨实例全局恢复能力。
- Spring `TransactionTemplate.execute(...)` 返回时，可视为当前事务已完成提交或回滚决议。

实现要求：

- 尽量复用现有 `TransactionRunner`，不额外引入复杂总线抽象
- 广播收敛后，回滚路径不得发送已注册的 after-commit 动作
- 保持调用方代码清晰，避免每个 service 手写临时列表收集副作用

测试要求：

- 至少补充：
  - `TransactionRunner` 事务后回调在成功时执行的测试
  - `TransactionRunner` 事务后回调在失败时不执行的测试
  - `ChannelApplicationService` 回滚后不广播 realtime 的测试

质量门禁：

- 相关测试通过
- 无新增依赖方向违规
- 关键行为有测试覆盖
- 任务单记录实际验证命令与结果

复审要求：

- 对事务边界、广播时机和回滚路径做深度自检

文档要求：

- 若实现改变了正式文档中的当前行为描述，需同步回写 `docs/`

验收标准：

- 项目存在可复用的最小事务后广播机制
- `channel` feature 的 realtime 广播不再位于事务体内直接执行
- 回滚路径不会触发已注册广播

完成定义：

- 代码、测试、必要文档更新完成
- 相关测试已执行
- 任务单已记录实际结果、验证记录、残留风险，并改为 `done`

实际结果：

- 已为 `TransactionRunner` 增加最小事务后副作用登记契约：
  - `runInTransaction(TransactionalAction<T>)`
  - `runInTransaction(TransactionalRunnable)`
  - `AfterCommitExecutor`
- 已在 `SpringTransactionRunner` 中对齐提交后执行语义：
  - 当 Spring 事务同步可用时，使用事务同步在真正 `afterCommit` 阶段执行
  - 当测试或非同步场景下没有事务同步时，退化为在事务回调成功返回后执行
- 已将 `ChannelApplicationService` 中已有 realtime 广播从事务体内直接执行收敛为事务成功后执行，覆盖：
  - `channels.changed`
  - `channel.changed`
  - `read_state.updated`
- 已补充测试：
  - `SpringTransactionRunnerTests` 覆盖 after-commit 成功执行与失败不执行
  - `ChannelApplicationServiceTests` 覆盖回滚后不提前广播
- 已同步更新 `docs/架构文档.md` 中关于事务广播边界的当前实现描述。

验证记录：

- 静态检查：
- `git diff --check -- chat-domain/src/main/java/team/carrypigeon/backend/chat/domain/features/channel/application/service/ChannelApplicationService.java infrastructure-service/database-api/src/main/java/team/carrypigeon/backend/infrastructure/service/database/api/transaction/TransactionRunner.java infrastructure-service/database-impl/src/main/java/team/carrypigeon/backend/infrastructure/service/database/impl/transaction/SpringTransactionRunner.java chat-domain/src/test/java/team/carrypigeon/backend/chat/domain/features/channel/application/service/ChannelApplicationServiceTests.java infrastructure-service/database-impl/src/test/java/team/carrypigeon/backend/infrastructure/service/database/impl/transaction/SpringTransactionRunnerTests.java docs/架构文档.md ai-agent-workplace/20260531-225241-ai-transaction-after-commit-broadcast-current.md`
- 结果：通过，无格式错误。
- 定向构建 / 测试尝试：
- `mvn -q -pl chat-domain,infrastructure-service/database-impl -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=ChannelApplicationServiceTests,SpringTransactionRunnerTests test`
- `mvn -pl infrastructure-service/database-impl -am -Dtest=SpringTransactionRunnerTests -Dsurefire.failIfNoSpecifiedTests=false test`
- `mvn -pl chat-domain -am -Dtest=ChannelApplicationServiceTests -Dsurefire.failIfNoSpecifiedTests=false test`
- 结果：均在 `infrastructure-basic` 资源复制阶段被环境阻塞，错误为 `infrastructure-basic/target/classes/log4j2-spring.xml: 只读文件系统`，未能完成正式测试执行。
- 补充验证：
- 编译期曾暴露一处新增 helper 缺失 `ChannelReadState` import，已修复后再次进入环境层只读阻塞，说明当前剩余阻塞点不在已观察到的业务代码语法层。

残留风险：

- 当前机制解决的是单进程内“事务提交后再广播”的一致性问题，不解决进程在提交成功后、广播执行前崩溃的丢事件问题。
- 当前仍未引入 outbox，因此不能宣称具备跨实例、可恢复的全局一致性。
- 由于环境 `target/classes` 只读，本轮未能拿到正式 Maven 通过记录；虽然静态检查与局部编译修复已完成，但仍建议在可写构建环境中复跑目标测试。

知识沉淀 / 是否回写 docs：

- 已回写 `docs/架构文档.md`，把事务广播边界更新为当前实现事实。
- 本轮新增的是可复用的最小事务后副作用机制，属于正式长期实现规则，已体现在代码与文档中。
