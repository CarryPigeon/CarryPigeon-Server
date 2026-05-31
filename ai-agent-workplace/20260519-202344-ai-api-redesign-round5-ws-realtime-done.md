任务名称：

第 5 轮：WebSocket 实时协议重写

任务目标：

按客户端基准 API 重写 `/api/ws` 实时协议，包括 auth、reauth、resume、event envelope、心跳与消息事件推送。

任务背景：

当前实时链路仍是最小自定义协议，与客户端基准 WS 协议不兼容，必须单列一轮处理。

影响模块：

- `chat-domain`
- 可能涉及 `application-starter`

允许修改范围：

- `server` feature 下 WS 入口、session、dispatcher、publisher、相关测试

禁止修改范围：

- 不在本轮大范围返修已完成的 HTTP 协议，除非为适配 WS 必需

依赖限制：

- 遵守既有模块边界

配置限制：

- 仅允许最小必要配置调整

文档依据：

- `docs/t/10-http-ws-protocol-v1.md`
- `docs/t/12-ws-events-v1.md`
- `docs/t/13-error-model-and-reasons-v1.md`

任务分解 / 执行计划：

1. 重写 WS 连接与认证模型。
2. 重写命令响应结构。
3. 重写事件 envelope 与事件类型。
4. 落地 resume 与失败兜底语义。
5. 补齐实时协议测试。

关键假设与依赖：

- 依赖消息 HTTP 模型已基本收敛。

实现要求：

- 事件字段、命令名和错误结构必须对齐客户端基准。

测试要求：

- 至少覆盖 auth、reauth、resume、ping/pong、message.created、message.deleted。

质量门禁：

- 实时协议关键路径可验证。

复审要求：

- 重点复审事件顺序、恢复语义与 envelope 一致性。

文档要求：

- 最终收口时更新正式 API 文档。

验收标准：

- 客户端 WS 连接、认证、恢复与实时消息消费可按基准协议工作。

完成定义：

- 新 WS 协议可运行并通过关键测试。

实际结果：

- 已将 WebSocket 握手从“升级前 Bearer 鉴权”改为“升级前仅准备请求上下文”，认证迁移到首帧 `auth` / `reauth`。
- 已将 WS 客户端帧模型重写为 `type / id / ts / data`，并保留对历史 `send_channel_message` 入站模型的最小兼容解析，便于平滑切换现有 realtime 发送链路。
- 已将服务端帧模型重写为 `type / id / data / error`，形成统一的命令响应与事件输出外壳。
- 已在 `RealtimeChannelHandler` 中落地最小 `auth`、`reauth`、`ping`、`resume.failed` 闭环：首次 `auth` 会绑定主体并回写 `auth.ok`，`reauth` 会回写 `reauth.ok`，`ping` 会回写 `pong`，无法回放时会回写 `resume.failed`。
- 已在 `RealtimeSessionRegistry` 中补充最小内存事件日志与 `eventsAfter(...)` 回放查询能力，为 `event_id` 与 resume 奠定基础。
- 已将 `NettyMessageRealtimePublisher` 改为输出 v1 `event` envelope，并为新建消息下发 `message.created`，对撤回更新下发 `message.deleted` 兜底语义。
- 已同步更新 realtime 配置装配链与测试支架，使新的 handler / publisher / initializer 依赖能够在 `chat-domain` 内正常装配与验证。

验证记录：

- 测试命令：`mvn -q -pl chat-domain -am -Dtest=RealtimeAccessTokenHandshakeHandlerTests,RealtimeChannelHandlerLifecycleTests,RealtimeChannelHandlerMessageDispatchTests,NettyMessageRealtimePublisherTests,RealtimeServerConfigurationContextTests,RealtimeServerConfigurationPublisherFactoryTests,RealtimeServerPropertiesTests,SendChannelMessageRealtimeHandlerTests -Dsurefire.failIfNoSpecifiedTests=false test`
- 结果：通过
- surefire 结果：`chat-domain/target/surefire-reports/team.carrypigeon.backend.chat.domain.features.server.controller.ws.RealtimeAccessTokenHandshakeHandlerTests.txt`
- surefire 结果：`chat-domain/target/surefire-reports/team.carrypigeon.backend.chat.domain.features.server.controller.ws.RealtimeChannelHandlerLifecycleTests.txt`
- surefire 结果：`chat-domain/target/surefire-reports/team.carrypigeon.backend.chat.domain.features.server.controller.ws.RealtimeChannelHandlerMessageDispatchTests.txt`
- surefire 结果：`chat-domain/target/surefire-reports/team.carrypigeon.backend.chat.domain.features.server.support.realtime.NettyMessageRealtimePublisherTests.txt`
- surefire 结果：`chat-domain/target/surefire-reports/team.carrypigeon.backend.chat.domain.features.server.config.RealtimeServerConfigurationContextTests.txt`
- surefire 结果：`chat-domain/target/surefire-reports/team.carrypigeon.backend.chat.domain.features.server.support.realtime.SendChannelMessageRealtimeHandlerTests.txt`

残留风险：

- 该轮最容易暴露历史消息模型与实时事件模型的耦合问题。
- 当前 resume 仍是最小内存事件日志实现，只保证当前进程内、有限窗口下的 `event_id` 回放语义；跨实例、长窗口或持久化回放尚未覆盖。
- `message.created` 事件中的 `sender.nickname/avatar` 当前仍使用最小占位值，未进一步接入用户资料快照查询；若客户端严格依赖该字段展示，还需在后续轮次继续收口。
- 当前旧的 `send_channel_message` 实时入站命令仍保留兼容承接，方便现有业务链路继续运行；若最终对外协议只保留 v1 command 集，需要在后续轮次明确是否删除该兼容路径。

知识沉淀 / 是否回写 docs：

- 当前已经形成一条稳定实现经验：当实时协议从历史自定义帧切到统一 envelope 时，优先重写“认证壳层、事件壳层、回放壳层”，再逐步替换内部业务命令，可显著降低对既有业务链的破坏范围。是否回写 `docs/`，建议在 round6 一并评估。

产物清理与保留说明：

- 保留协议对照和测试记录
