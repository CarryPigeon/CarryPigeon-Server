任务名称：

message 链路 after-commit 收口与真实 Spring 事务同步测试

任务目标：

将 message 应用服务中的实时广播统一迁移到 `TransactionRunner.afterCommit` 机制，并补充一组基于真实 Spring 事务同步激活的集成测试设计与实现。

任务背景：

channel 链路已完成“事务提交后广播”收口，但 `MessageApplicationService` 仍存在事务返回后直接广播的路径。当前还缺少一组不依赖 mock `TransactionTemplate` 的真实 Spring 事务同步激活测试来证明 `SpringTransactionRunner` 会接入 `TransactionSynchronizationManager` 的 after-commit 生命周期。

影响模块：

- `chat-domain`
- `infrastructure-service/database-impl`

允许修改范围：

- 允许修改 `MessageApplicationService`，仅收口事务后广播相关实现
- 允许补充 `chat-domain` 现有 message 应用服务测试
- 允许补充 `database-impl` 事务运行器测试
- 允许在 `ai-agent-workplace/` 记录任务与自检结果
- 如实现与代码现状存在必要差异，允许做最小文档补充

禁止修改范围：

- 不允许调整模块依赖方向
- 不允许新增第三方依赖
- 不允许改变 message/channel 领域规则与对外接口语义
- 不允许扩展为 outbox、全局事件总线或新的架构方案

依赖限制：

- 仅使用现有 Spring Boot、Spring Transaction、JUnit 5、Mockito 等已在项目内存在的依赖

配置限制：

- 不新增运行时配置
- 不引入未来占位配置

文档依据：

- `docs/架构文档.md`
- `docs/注释规范.md`
- `docs/测试规范.md`
- `docs/变更审核清单.md`
- `docs/任务单模板.md`

任务分解 / 执行计划：

1. 审核 `MessageApplicationService` 的所有 realtime / mention 广播点，确认事务外发送位置
2. 参照 `ChannelApplicationService` 的模式，把 message 创建、更新、撤回、删除、pin/unpin、system 发送统一改为 after-commit 注册
3. 补充 message 侧回滚不广播测试，覆盖消息广播与 mention 广播
4. 设计并实现真实 Spring 事务同步激活测试，使用真实 `TransactionTemplate` 而非 mock
5. 执行差异自检、测试尝试与任务收尾记录

关键假设与依赖：

- 已确认 `TransactionRunner` 与 `SpringTransactionRunner` 已具备 after-commit 契约与实现
- 假设 `database-impl` 当前测试依赖足以支撑基于 Spring 事务管理器的真实同步测试
- 若 Maven 继续受当前环境只读问题阻塞，则记录为验证阻塞而不扩大改动范围

实现要求：

- `MessageApplicationService` 只做事务后广播收口，不改变持久化顺序与校验语义
- 优先提炼私有 helper，避免在每个事务方法重复手写 after-commit 广播
- 测试需直接验证“回滚不广播”和“真实事务同步已激活”两个目标

测试要求：

- 至少补充 1 个 message 侧回滚不广播测试
- 至少补充 1 个真实 Spring 事务同步激活测试
- 测试类与测试方法需保留职责注释

质量门禁：

- 受影响代码无新增明显编译错误
- 关键链路具备成功与失败行为断言
- `git diff --check` 通过
- 若无法跑通 Maven，需明确阻塞原因

复审要求：

- 重点复审事务边界内外职责是否清晰
- 重点复审回滚场景下是否仍存在提前广播

文档要求：

- 仅在代码契约已有必要变化时做最小文档回写

验收标准：

- `MessageApplicationService` 中不再保留事务返回后直接进行的 message 相关广播
- message 发送/更新相关 mention 广播走统一 after-commit 路径
- 存在真实 Spring 事务同步激活测试来证明 `afterCommit` 注册接入 Spring 生命周期

完成定义：

- 代码、测试、必要文档与任务记录已完成
- 已做自检并记录验证结果与残留风险

实际结果：

- 已将 `MessageApplicationService` 的发送、转发、编辑、system 发送、撤回、删除、pin/unpin 广播收口到 `TransactionRunner.afterCommit`
- 已补充 message 侧回滚不广播测试
- 已补充真实 Spring 事务同步激活测试设计与实现

验证记录：

- `git diff --check -- <affected files>` 通过
- 已尝试运行 `mvn -q -f /mnt/d/workspace/items/carrypigeon/backend/pom.xml -pl chat-domain,infrastructure-service/database-impl -am -DskipTests=false -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=SpringTransactionRunnerTests,MessageApplicationServiceSendTests test`
- 已尝试单模块测试，但在当前环境下因内部模块依赖未预先安装，直接模块级 Maven 执行报依赖解析失败
- reactor 级测试执行未产出一份可明确归属本次改动的新完整报告，因此本次自动化验证结论以静态自检为主

残留风险：

- 尚缺一份当前环境下重新生成并可确认归属本次源码的 Maven 测试通过记录
- `afterCommit` 方案仍是单进程内提交后广播，不覆盖进程崩溃恢复一致性

知识沉淀 / 是否回写 docs：

- 视代码契约是否新增长期规则再决定

产物清理与保留说明：

- 任务过程记录保留在 `ai-agent-workplace/`

补充说明：

- 本任务仅收口单进程内“提交后再广播”一致性，不承诺进程崩溃恢复级别保证
