任务名称：
频道申请语义收口

任务目标：
按当前项目代码语义收紧频道申请与频道邀请的边界，避免管理端把邀请误当作申请处理。

任务背景：
群聊模块当前将邀请与申请复用 `ChannelInvite` 持久化模型，但应用服务尚未完整隔离两类语义，存在申请列表混入邀请记录、审批接口可误处理邀请记录、旧记录复用时未标准化申请标记的问题。

影响模块：
- `chat-domain`

允许修改范围：
- 允许修改频道申请应用服务实现
- 允许补充/调整对应契约测试
- 如有必要允许最小化更新相关接口文档

禁止修改范围：
- 不修改模块依赖方向
- 不新增第三方依赖
- 不引入新架构组件或新的持久化模型
- 不扩散到无关功能链路

依赖限制：
- 仅使用现有 Spring Boot、JUnit 与项目内基础设施能力

配置限制：
- 不新增配置项

文档依据：
- `docs/架构文档.md`
- `docs/异常与错误码规范.md`
- `docs/测试规范.md`
- `docs/变更审核清单.md`

任务分解 / 执行计划：
1. 审核 `ChannelApplicationService` 中申请/邀请共模的当前行为。
2. 收紧申请创建、申请列表、申请审批三处语义。
3. 补充成功/失败路径测试，覆盖邀请不可作为申请处理的约束。
4. 运行定向回归测试确认不影响既有事务后广播收口。

关键假设与依赖：
- 当前代码以内存语义约定 `inviterAccountId == 0L` 表示“申请”，非零表示“邀请”。
- 本次仅按既有模型收口，不引入新表或类型字段。

实现要求：
- 申请视图只暴露真实申请记录。
- 审批接口只能处理真实申请记录。
- 复用旧记录时要显式标准化为申请语义。

测试要求：
- 补充邀请记录不会出现在申请列表中的测试。
- 补充邀请记录不能走申请审批接口的测试。
- 补充旧邀请记录复用为申请时会重置申请语义的测试。

质量门禁：
- 相关模块编译通过
- 定向测试通过
- 无新增依赖与配置

复审要求：
- 检查异常语义是否符合当前问题模型
- 检查实时广播触发点未被破坏

文档要求：
- 仅在对外契约描述需要更明确时做最小更新

验收标准：
- 申请列表只返回申请
- 申请审批拒绝处理邀请记录
- 旧记录复用后语义一致
- 相关测试通过

完成定义：
- 代码、测试、自检完成并将任务单转为 `done`

实际结果：
- `ChannelApplicationService` 已将“申请”统一识别为 `inviterAccountId == 0L`
- 申请列表已过滤掉主动邀请记录
- 申请审批已拒绝处理邀请记录
- 接受邀请接口已拒绝处理申请记录
- 申请复用旧记录时会标准化 `inviterAccountId`，且返回值与实际持久化记录一致
- 已最小更新 `docs/t/11-http-endpoints-v1.md` 的接口约定说明

验证记录：
- 通过：`mvn -pl chat-domain -am -Dtest=ChannelApplicationServiceTests,RealtimeChannelHandlerMessageDispatchTests,RealtimeSessionRegistryTests,NettyMessageRealtimePublisherTests,ChannelDiscoverApplicationServiceTests,RealtimeServerConfigurationPublisherFactoryTests -Dsurefire.failIfNoSpecifiedTests=false test`
- 结果：`Tests run: 48, Failures: 0, Errors: 0, Skipped: 0`
- 说明：`ChannelApplicationControllerTests` 在当前环境因 Mockito inline mock maker 自附着失败，未作为本次有效回归依据

残留风险：
- 共享持久化模型仍依赖约定表达两类语义，后续如继续扩展治理能力仍建议拆模

知识沉淀 / 是否回写 docs：
- 已回写 `docs/t/11-http-endpoints-v1.md`，明确 applications 接口只处理申请语义

产物清理与保留说明：
- 保留任务单作为追踪记录

补充说明：
- 本次任务基于当前项目代码进行最小收口，不扩大为群治理重构。
