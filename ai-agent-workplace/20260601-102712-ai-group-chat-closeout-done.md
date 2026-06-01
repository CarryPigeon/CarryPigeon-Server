任务名称：
群聊模块四项收口

任务目标：
按顺序完成群聊模块的 realtime 入站承接、频道类型模型统一、邀请/申请审批 after-commit 广播补齐、sender 快照与 resume 可用性优化，并补充验证。

任务背景：
当前群聊能力已具备基础频道治理、消息存储与 realtime 推送，但仍存在四类缺口：
1. WS 入站 dispatcher 已存在但缺少实际 handler，文档声明的兼容命令未真正可用。
2. discover 过滤类型与数据库/领域频道类型模型不一致。
3. 邀请、入群申请与审批链路尚未统一到事务提交后广播。
4. sender 信息仍由 realtime 发布器逐条查询，resume 仅保留极小内存窗口，恢复可用性偏弱。

影响模块：
- `chat-domain`
- `application-starter`（仅当需要核对 migration / 文档事实，不预期改代码）
- `docs`（仅更新与当前代码事实直接相关的接口文档）

允许修改范围：
- 允许修改 `chat-domain` 内群聊相关 application/service/support/config/test 代码
- 允许补充或调整现有群聊相关测试
- 允许更新与本次改动直接相关的 `docs/API.md`
- 允许新增本任务单

禁止修改范围：
- 不允许调整 Maven 模块结构与依赖方向
- 不允许新增第三方依赖
- 不允许引入新的跨进程事务/事件架构
- 不允许改动无关 feature 的业务语义

依赖限制：
- 仅使用现有 Spring Boot、JUnit、Netty、仓库内基础设施抽象

配置限制：
- 不新增未来占位配置
- 如需调整 resume 窗口，优先使用代码内最小稳定默认值

文档依据：
- `docs/架构文档.md`
- `docs/API.md`
- `docs/包结构规范.md`
- `docs/注释规范.md`
- `docs/测试规范.md`
- `docs/变更审核清单.md`

任务分解 / 执行计划：
1. 审核 realtime 协议、handler、消息服务与现有测试，确认入站兼容命令与 after-commit 边界。
2. 实现 WS 入站 handler，并补充 handler/message dispatch 测试。
3. 统一 discover 频道类型模型到当前持久化实际值，并修正测试与文档。
4. 为 invite/application/decision/accept 链路补齐 after-commit 广播与测试。
5. 优化消息 realtime sender 快照与 resume 窗口实现，补充对应测试。
6. 运行定向测试、自检 diff 与文档事实一致性，完成任务单归档。

关键假设与依赖：
- 已确认 `public/private/system` 是当前仓库内稳定频道类型事实。
- 已确认 WS 文档保留了旧 `send_channel_message` 最小兼容承接。
- 本次不引入 Redis/outbox，因此 resume 优化以单进程内最小增强为边界。

实现要求：
- 新增代码保持 `features` 分包，不建立模糊 util 桶。
- 事务后广播统一通过 `TransactionRunner.AfterCommitExecutor` 登记。
- 测试应覆盖成功与失败路径，并保持注释规范。

测试要求：
- 补充或更新 realtime handler dispatch tests
- 补充或更新 channel application service tests
- 补充或更新 message realtime publisher / session registry tests
- 运行受影响模块定向测试

质量门禁：
- 相关代码可编译
- 相关测试通过
- 不引入新的架构越界依赖
- 文档与代码事实保持一致

复审要求：
- 复查群聊链路中事务后广播触发点是否遗漏
- 复查 discover/realtime 文档陈述是否与实现一致

文档要求：
- 只更新与本次代码事实直接相关的文档条目

验收标准：
- WS 入站至少可处理旧 `send_channel_message` 命令
- discover 类型过滤与仓库当前频道类型模型一致
- invite/application/approval/accept 产生的频道刷新广播只在事务提交后触发
- sender 快照生成避免 publish 时额外仓储查询
- resume 相比当前实现具备更稳定的窗口行为，并有测试覆盖

完成定义：
- 代码、测试、必要文档与任务单全部收口

实际结果：
- 已新增 `ChannelMessageRealtimeInboundHandler`，补齐旧 `send_channel_message` WS 入站承接，并补充 dispatch 测试。
- 已把 discover 类型过滤统一到当前持久化事实 `public/private/system`，同步修正相关测试与 API 文档。
- 已为 invite / accept invite / create application / decide application 链路补齐 `TransactionRunner.afterCommit` 广播登记。
- 已把消息 realtime sender 展示信息改为由应用服务预先生成 `MessageSenderSnapshot`，发布器不再逐条查用户资料仓储。
- 已把 resume 从全局 200 条共享窗口改为按账户隔离的内存事件窗口，并新增针对性测试。
- 已回写 `docs/API.md` 与 `docs/架构文档.md` 中相关事实说明。

验证记录：
- 编译验证：
  - `cd /mnt/d/workspace/items/carrypigeon/backend && mvn -pl chat-domain -am -DskipTests test-compile -Dsurefire.failIfNoSpecifiedTests=false`
- 定向测试：
  - `cd /mnt/d/workspace/items/carrypigeon/backend && mvn -pl chat-domain -am -Dtest=RealtimeChannelHandlerMessageDispatchTests,RealtimeSessionRegistryTests,NettyMessageRealtimePublisherTests,ChannelDiscoverApplicationServiceTests,ChannelApplicationServiceTests,RealtimeServerConfigurationPublisherFactoryTests -Dsurefire.failIfNoSpecifiedTests=false test`
- 定向测试结果：
  - `Tests run: 44, Failures: 0, Errors: 0, Skipped: 0`

残留风险：
- 当前 resume 仍是单节点内存窗口实现，尚不具备跨实例恢复与持久化补偿能力。
- 邀请 / 申请相关 realtime 仍以 `channels.changed` / `channel.changed` refresh hint 为主，没有细分专用事件类型。

知识沉淀 / 是否回写 docs：
- 已回写 `docs/API.md` 中 discover 类型、HTTP 消息能力、resume 与 WS 兼容命令事实。
- 已回写 `docs/架构文档.md` 中 channel 治理链路 after-commit 广播边界。

产物清理与保留说明：
- 保留本任务单；不新增其他临时产物

补充说明：
- 本任务按用户要求依次执行 `1 2 3 4`，但测试与文档收口可在实现后集中完成。
