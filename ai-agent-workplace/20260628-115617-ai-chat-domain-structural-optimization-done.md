任务名称：

chat-domain 结构性优化

任务目标：

在不改变业务行为、HTTP/WS 协议、错误码和模块依赖方向的前提下，按收益优先级继续优化 `chat-domain` 的结构清晰度、可读性和可扩展性。

任务背景：

当前 `chat-domain` 已形成 feature-first 结构，并完成了 application layer 移除、domain service 命名收敛、support 语义提纯等前置优化。复审显示整体结构清晰合理，但仍存在少数可继续获得收益的结构点：

- `message` 与 `channel` 的跨 feature 依赖仍偏密。
- `AbstractChannelDomainSupport` / `AbstractMessageDomainSupport` 仍承载较多共享逻辑。
- 少数 controller 体量偏大。
- realtime server 装配仍承担跨 feature 组合职责。

影响模块：

- `chat-domain`
- `ai-agent-workplace`

允许修改范围：

- `chat-domain/src/main/java/team/carrypigeon/backend/chat/domain/features/channel/**`
- `chat-domain/src/main/java/team/carrypigeon/backend/chat/domain/features/message/**`
- `chat-domain/src/main/java/team/carrypigeon/backend/chat/domain/features/server/**`
- `chat-domain/src/test/java/team/carrypigeon/backend/chat/domain/features/channel/**`
- `chat-domain/src/test/java/team/carrypigeon/backend/chat/domain/features/message/**`
- `chat-domain/src/test/java/team/carrypigeon/backend/chat/domain/features/server/**`
- 与上述结构优化直接相关的配置类和测试支撑类
- 当前任务单本身

禁止修改范围：

- 不修改 Maven 模块结构。
- 不新增第三方依赖。
- 不修改正式 `docs/`，除非发现必须沉淀的长期规则并另行确认。
- 不修改业务语义、HTTP/WS 协议、错误码、响应字段。
- 不让 `chat-domain` 依赖 `application-starter` 或任何 `*-impl`。
- 不把结构优化扩张为全局重写。

优化顺序：

1. 收口 `message -> channel` 跨 feature 依赖：优先用 message domain 内的语义端口或协作对象隔离对 channel 仓储/规则的直接理解。
2. 压缩共享基类：优先减少 `AbstractChannelDomainSupport` / `AbstractMessageDomainSupport` 中不再适合继承共享的逻辑。
3. 拆分少数超大 controller：只在收益明确、测试可覆盖时按资源或用例拆分。
4. 评估 realtime server 装配：只做低风险收口，不移动跨模块组合根职责到错误位置。

验收标准：

- 每一步都有明确结构收益，且不改变外部行为。
- 受影响测试通过。
- 至少执行一次 `mvn -pl chat-domain -am test -DskipTests=false`，或记录无法执行的具体阻塞。
- 边界扫描不出现 `chat-domain` 依赖 `application-starter` 或 `*-impl`。
- 任务单记录实际结果、验证命令、残留风险，并关闭为 `done`。

执行前确认点：

- 本任务属于结构性优化，需用户确认后再开始修改正式代码。
- 默认按上述 1 -> 4 的顺序逐步推进；若中途发现某一步收益不足或风险过高，应停止扩张并记录原因。

实际结果：

- 收口 `message -> channel` 跨 feature 依赖：
  - 新增 `MessageChannelBoundary`，message 领域服务只依赖消息侧语义边界。
  - 新增 `ChannelBackedMessageChannelBoundary`，把对 channel 仓储、pin、审计和治理策略的理解集中到 `message/support/channel`。
  - `MessageRealtimePublisher` 的 pin 实时发布改用 message 侧 pin 快照，避免实时发布接口直接暴露 `ChannelPin`。
- 收口 `channel -> message` 跨 feature 依赖：
  - 新增 `ChannelMessageBoundary`，channel 领域服务只关心消息存在性和频道归属快照。
  - 新增 `MessageBackedChannelMessageBoundary`，把对 `MessageRepository` / `ChannelMessage` 的理解集中到 `channel/support/message`。
  - `AbstractChannelDomainSupport` 不再直接持有 `MessageRepository`。
  - `ChannelGovernancePolicy.requireCanRecallMessage` 改为接收 `senderAccountId`，不再接收 message 模型。
- 配置职责修正：
  - 新增 `ChannelRealtimeConfiguration`，channel realtime 默认 noop bean 回到 channel feature。
  - `MessagePersistenceConfiguration` 只保留 message 持久化和 message realtime 默认 bean。
- controller 职责拆分：
  - 新增 `ChannelPinsController`，承接 `/api/channels/{channelId}/pins...` 置顶、取消置顶和列表接口。
  - `ChannelMessageController` 保留历史/搜索、发送、编辑、删除和附件上传入口。
- 未继续拆分 `ChannelController` 和 realtime server 装配：
  - `ChannelController` 虽然体量较大，但当前接口和测试仍高度聚合，继续拆分需要更广协议回归验证。
  - realtime server 装配当前位于 server feature，承担 Netty 运行时组合根职责，继续移动收益不足。

验证记录：

- `mvn -pl chat-domain -am -Dtest='team.carrypigeon.backend.chat.domain.features.message.domain.service.MessageDomainServiceSendTests,team.carrypigeon.backend.chat.domain.features.message.domain.service.MessageDomainServicePinsTests,team.carrypigeon.backend.chat.domain.features.message.domain.service.MessageDomainServiceQueryTests,team.carrypigeon.backend.chat.domain.features.message.domain.service.MessageDomainServiceForwardTests,team.carrypigeon.backend.chat.domain.features.server.support.realtime.NettyMessageRealtimePublisherTests,team.carrypigeon.backend.chat.domain.features.server.controller.ws.RealtimeChannelHandlerMessageDispatchTests' -Dsurefire.failIfNoSpecifiedTests=false test -DskipTests=false`
  - 通过：46 tests, 0 failures, 0 errors, 0 skipped。
- `mvn -pl chat-domain -am -Dtest='team.carrypigeon.backend.chat.domain.features.channel.domain.service.ChannelDomainServiceAccessTests,team.carrypigeon.backend.chat.domain.features.channel.domain.service.ChannelDomainServiceLifecycleTests,team.carrypigeon.backend.chat.domain.features.channel.domain.service.ChannelDomainServiceGovernanceTests,team.carrypigeon.backend.chat.domain.features.channel.domain.service.ChannelDomainServiceApplicationFlowTests,team.carrypigeon.backend.chat.domain.features.message.domain.service.MessageDomainServiceForwardTests,team.carrypigeon.backend.chat.domain.features.message.domain.service.MessageDomainServicePinsTests' -Dsurefire.failIfNoSpecifiedTests=false test -DskipTests=false`
  - 通过：40 tests, 0 failures, 0 errors, 0 skipped。
- `mvn -pl chat-domain -am -Dtest='team.carrypigeon.backend.chat.domain.features.message.controller.http.ChannelPinsControllerTests,team.carrypigeon.backend.chat.domain.features.message.controller.http.ChannelMessageQueryControllerTests,team.carrypigeon.backend.chat.domain.features.message.controller.http.MessageForwardControllerTests' -Dsurefire.failIfNoSpecifiedTests=false test -DskipTests=false`
  - 通过：15 tests, 0 failures, 0 errors, 0 skipped。
- `mvn -pl chat-domain -am test -DskipTests=false`
  - 通过：chat-domain 302 tests, 0 failures, 0 errors, 0 skipped；依赖模块同时通过。
- 边界扫描：
  - `message` main 中对 `features.channel` 的依赖仅存在于 `features/message/support/channel/ChannelBackedMessageChannelBoundary.java`。
  - `channel` main 中对 `features.message` 的依赖仅存在于 `features/channel/support/message/MessageBackedChannelMessageBoundary.java`。
  - `chat-domain/src/main/java` 与 `chat-domain/src/test/java` 未发现对 `application-starter` 或 `infrastructure-service/*-impl` 的依赖。

残留风险：

- `ChannelController` 仍偏大，但本轮未拆分，避免在没有足够收益和回归范围的情况下扩大协议层改动。
- `RealtimeServerConfiguration` 仍承担 channel/message Netty publisher 装配，当前判断属于 server feature 运行时组合根职责，未继续移动。
- 工作树存在大量本任务前已有的未提交变更，本轮未回滚也未清理无关变更。

知识沉淀 / 是否回写 docs：

- 未新增长期项目规则，不回写 `docs/`。

产物清理与保留说明：

- 保留本任务单作为结构优化追踪记录，完成后由 `current` 重命名为 `done`。
