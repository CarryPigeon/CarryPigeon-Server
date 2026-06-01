任务名称：

realtime 安全语义与 system channel 初始化修复

任务目标：

修复当前项目在 realtime 事件恢复、reauth 会话切换、mention 广播事务边界与 system channel 初始化上的高风险设计缺陷，并补充对应测试，确保行为与当前架构边界一致。

任务背景：

基于刚完成的项目功能切面审查，当前高优先级问题集中在：

- realtime 事件恢复未按接收者过滤，存在跨用户事件泄露风险
- realtime `reauth` 不清理旧账户订阅，存在多账户串流风险
- mention 创建事件在事务内提前广播，可能出现回滚后幽灵事件
- system channel 由注册流程懒创建且缺少唯一约束，存在并发重复创建风险

影响模块：

- `chat-domain`
- `application-starter`
- `infrastructure-service/database-api`
- `infrastructure-service/database-impl`
- `ai-agent-workplace/`

允许修改范围：

- 允许修改上述模块中的正式代码、正式测试、Flyway 迁移脚本
- 允许在 `ai-agent-workplace/` 记录任务过程

禁止修改范围：

- 不修改模块依赖方向
- 不引入新第三方依赖
- 不扩展为新的事件总线或新的架构模式
- 不大范围重写聊天业务结构

依赖限制：

- 仅使用仓库现有 Spring Boot / MyBatis / Netty / 测试能力

配置限制：

- 不新增未来占位配置
- 仅在现有配置语义下修正 discovery / realtime 行为时调整必要代码

文档依据：

- `docs/架构文档.md`
- `docs/包结构规范.md`
- `docs/依赖引入规范.md`
- `docs/异常与错误码规范.md`
- `docs/测试规范.md`
- `docs/AI协作开发规范.md`
- `docs/变更审核清单.md`

任务分解 / 执行计划：

1. 阅读相关现有测试与实现，确认可最小修复的切入点。
2. 修复 realtime 事件日志与恢复语义，确保只向合法接收者恢复事件，并修复 `reauth` 旧身份残留。
3. 调整 mention 广播时机，使其与消息主广播一致地落到事务提交后。
4. 将 system channel 初始化从“业务懒创建”收敛到更稳定的持久化基线，并补防重复约束。
5. 补充单元/契约/回归测试。
6. 运行受影响模块测试并记录结果。
7. 自检后关闭任务单。

关键假设与依赖：

- 假设当前 `system` 频道在业务语义上是单实例 canonical channel，而不是允许多条同类记录。
- 假设 realtime 恢复仍采用当前内存窗口模式，只做最小安全修正，不引入持久化事件存储。

实现要求：

- 保持 domain -> port -> impl 依赖方向不变
- 修复应优先最小化，避免在一个任务里同时推翻现有 realtime 协议
- 所有新增行为必须有测试锁定

测试要求：

- 至少补充：
  - realtime 恢复接收者过滤测试
  - realtime `reauth` 清理旧账户绑定测试
  - mention 广播事务边界测试
  - system channel 初始化/迁移相关测试

质量门禁：

- 相关测试通过
- 无新增依赖方向违规
- 关键行为有测试覆盖
- 任务单记录实际验证命令与结果

复审要求：

- 对 realtime 安全语义、事务副作用边界、启动/迁移行为进行深度自检

文档要求：

- 本轮先以代码与测试修复为主
- 若实现中确认了新的长期协议规则，再评估是否补 `docs/`

验收标准：

- 已修复上述 4 类高优先级问题
- 相关测试新增并通过
- 无额外架构漂移

完成定义：

- 代码、测试、迁移脚本修改完成
- 相关测试已执行
- 任务单已记录实际结果、验证记录、残留风险，并改为 `done`

实际结果：

- 已完成 4 项高优先级修复：
- realtime 事件日志追加接收者信息，断线续传按当前认证账户过滤，只恢复当前账户可见事件。
- realtime `reauth` 在切换到账户新身份前会先注销旧账户通道绑定，避免单连接残留多账户订阅。
- `mention.created` 改为在事务成功提交后的应用层主流程中发布，避免 mention 持久化失败时提前广播幽灵事件。
- 注册流程不再懒创建 `system` channel，改为要求持久化基线已存在；新增 Flyway `V16__insert_system_channel.sql` 补齐 canonical system channel 种子。
- 已补充对应回归测试，覆盖 resume 过滤、reauth 清理、mention 事务边界与缺失 system channel 的失败语义。

验证记录：

- 静态检查：
- `git diff --check -- chat-domain/src/main/java/team/carrypigeon/backend/chat/domain/features/auth/application/service/AuthApplicationService.java chat-domain/src/main/java/team/carrypigeon/backend/chat/domain/features/message/application/service/MessageApplicationService.java chat-domain/src/main/java/team/carrypigeon/backend/chat/domain/features/server/controller/ws/RealtimeChannelHandler.java chat-domain/src/main/java/team/carrypigeon/backend/chat/domain/features/server/support/realtime/RealtimeSessionRegistry.java chat-domain/src/main/java/team/carrypigeon/backend/chat/domain/features/server/support/realtime/NettyMessageRealtimePublisher.java chat-domain/src/main/java/team/carrypigeon/backend/chat/domain/features/server/support/realtime/NettyChannelRealtimePublisher.java chat-domain/src/test/java/team/carrypigeon/backend/chat/domain/features/auth/application/service/AuthApplicationServiceTests.java chat-domain/src/test/java/team/carrypigeon/backend/chat/domain/features/message/application/service/MessageApplicationServiceSendTests.java chat-domain/src/test/java/team/carrypigeon/backend/chat/domain/features/message/application/service/MessageApplicationServiceTestSupport.java chat-domain/src/test/java/team/carrypigeon/backend/chat/domain/features/server/controller/ws/RealtimeChannelHandlerLifecycleTests.java chat-domain/src/test/java/team/carrypigeon/backend/chat/domain/features/server/controller/ws/RealtimeChannelHandlerMessageDispatchTests.java chat-domain/src/test/java/team/carrypigeon/backend/chat/domain/features/server/controller/ws/RealtimeChannelHandlerTestSupport.java application-starter/src/main/resources/db/migration/V16__insert_system_channel.sql`
- 结果：通过，无空白错误。
- 定向测试：
- `mvn -q -pl chat-domain -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=RealtimeChannelHandlerLifecycleTests,RealtimeChannelHandlerMessageDispatchTests,MessageApplicationServiceSendTests,AuthApplicationServiceTests test`
- 结果：通过，退出码 0。

残留风险：

- `system` channel 当前通过迁移种子保证存在，但数据库层仍未建立“仅允许一条 system 频道”的强约束；后续若出现人工脏数据，`findSystemChannel()` 仍依赖查询结果唯一性。
- realtime 续传仍是进程内存窗口方案，本轮只修复安全语义，不扩展为跨重启可恢复的持久化事件流。
- 更低优先级的 capability / discovery 语义不一致与文档漂移问题未纳入本任务范围。

知识沉淀 / 是否回写 docs：

- 本轮未引入新的长期架构规则，暂不回写 `docs/`。
- 关键结论已通过测试与迁移脚本固化在正式代码中，并通过本任务单留痕。

产物清理与保留说明：

- 保留本任务单作为本次实现留痕

补充说明：

- 本任务属于实现类任务，需完成代码修改、测试验证与关闭记录。
