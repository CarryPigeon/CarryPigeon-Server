任务名称：
文件消息与语音消息真实可用化

任务目标：
在不突破既有模块边界和 storage-api 契约的前提下，让文件消息与语音消息具备最小真实可用闭环：调用方可以先上传对象，再发送 file / voice 消息，并且在历史查询与 realtime 下发中拿到可直接访问的临时 URL。

任务背景：
上一轮任务已经把 file / voice 消息接入统一消息插件骨架，并打通发送、持久化、历史查询、搜索和 realtime 下发。但当前仍存在三处缺口：
1. 没有业务上传入口，调用方必须自行先获得有效 objectKey；
2. 历史查询与 realtime 下发只返回 canonical objectKey，不返回可直接访问的 URL；
3. 异步 transcript 任务流尚不存在。

用户本轮明确要求“让文件消息和语音消息达到真实能用的水平”。结合当前代码和仓库边界，本轮默认范围收敛为：
1. 增加面向消息附件的上传入口；
2. 增加 file / voice 在历史查询与 realtime 下发时的 presigned URL 派生；
3. 继续保持数据库中只存 canonical objectKey；
4. 不在本轮引入异步 transcript 流程。

影响模块：
- `chat-domain`
- `application-starter`
- `infrastructure-service/storage-api`
- `infrastructure-service/storage-impl`
- `ai-agent-workplace`

允许修改范围：
- 允许修改 `chat-domain/features/message/**` 中与消息读取、出站整形、上传入口直接相关的代码
- 允许修改 `chat-domain/features/server/**` 中与 realtime 消息下发直接相关的代码
- 允许在不突破模块边界的前提下复用既有 `storage-api` 能力
- 允许补充或改写相关测试
- 允许在 `application-starter` 中使用既有最小运行配置进行装配验证

禁止修改范围：
- 不允许修改模块依赖方向
- 不允许让 `chat-domain` 直接依赖任何 `*-impl`
- 不允许新增第三方依赖或新的中间件
- 不允许把数据库中的消息 payload 改成持久化临时 URL
- 不允许在本轮引入异步 transcript 回写链路
- 不允许演化成仓库级通用文件中心或媒体处理架构

依赖限制：
- 继续使用现有 Spring Boot / Jackson / MyBatis-Plus / 项目基础设施能力
- 对象上传与 URL 派生只能通过既有 `ObjectStorageService` 完成
- 若需要消息出站整形，只能在 `chat-domain` 内通过已有应用层 / 协议层边界完成

配置限制：
- 不新增未来占位配置
- 默认复用现有 `cp.infrastructure.service.storage.*` 配置
- 若需要 URL TTL，应优先以内建常量或最小局部配置处理，避免扩张全局配置体系

文档依据：
- `AGENTS.md`
- `docs/架构文档.md`
- `docs/包结构规范.md`
- `docs/依赖引入规范.md`
- `docs/注释规范.md`
- `docs/测试规范.md`
- `docs/AI协作开发规范.md`
- `docs/变更审核清单.md`
- `docs/任务单模板.md`

任务分解 / 执行计划：
1. 核对 message 出站整形边界、storage-api 上传能力与 controller / DTO 既有模式。
2. 在 message feature 内设计并实现最小上传入口，使调用方可以得到 canonical objectKey 与对象元信息。
3. 为 file / voice 消息增加统一的出站附件访问信息派生，使历史查询与 realtime 下发都能返回可直接消费的临时 URL。
4. 保持消息持久化结构仍以 canonical objectKey 为准，不把临时 URL 写入数据库。
5. 补充应用层、协议层与 realtime 相关测试，并执行定向验证。
6. 按变更审核清单完成自检与复审，记录结果、命令和残留风险。

关键假设与依赖：
- 已确认 file / voice 消息现有 send / persist / history / search / realtime 主链路可复用，无需重做消息插件骨架。
- 已确认当前 storage-api 提供 `put(...)` 与 `createPresignedUrl(...)`，其中预签名 URL 能力当前是 GET 下载语义。
- 本轮默认采用“服务端接收上传并调用 `ObjectStorageService.put(...)`”作为最小上传入口方向，避免先扩张 storage-api 为 PUT 预签名上传协议。
- 本轮不要求语音转写异步补写；voice 的 transcript 仍保持发送时可选传入。

实现要求：
- 上传入口必须遵循仓库既有 HTTP controller / DTO / application service 模式。
- 上传结果必须返回 canonical objectKey，供 file / voice 消息继续沿用现有发送链路。
- file / voice 消息在 HTTP 历史查询、HTTP 搜索和 realtime 下发中，应返回包含临时访问 URL 的可消费结构。
- 临时访问 URL 必须由读取 / 下发阶段派生，而不是持久化入库。
- 非 file / voice 消息不得被错误附加附件访问字段。
- 所有改动必须维持 `chat-domain -> storage-api` 的边界，不得越过到 impl。

测试要求：
- 覆盖上传入口成功路径与失败路径。
- 覆盖 file / voice 历史查询返回可访问 URL。
- 覆盖 file / voice realtime 下发返回可访问 URL。
- 覆盖 text 消息不受影响。
- 覆盖对象不存在、上传非法输入、URL 派生失败等失败语义。
- 测试命名、注释与断言必须符合 `docs/测试规范.md`。

质量门禁：
- 受影响 Java 文件无新增编译错误
- 相关单测通过
- 受影响 Maven reactor 测试通过
- 若改动涉及运行时装配或协议入口，至少完成对应模块的定向验证
- 不引入新的模块边界违规

复审要求：
- 重点复审上传入口是否落在正确 feature 边界
- 重点复审 presigned URL 是否只在出站阶段派生且未入库
- 重点复审 realtime 与 HTTP 是否保持一致的附件可用语义
- 重点复审 text / file / voice 三类消息的兼容性与回归风险

文档要求：
- 若未形成新的长期仓库规则，则不修改 `docs/`
- 任务过程、验证记录与残留风险记录在本任务单

验收标准：
- 调用方可以通过新增上传入口获得可用于 file / voice 消息发送的 canonical objectKey
- file / voice 消息历史查询结果包含可直接访问的临时 URL
- file / voice 消息 realtime 下发结果包含可直接访问的临时 URL
- 数据库中消息 payload 仍以 canonical objectKey 为准
- text 消息读写行为不受破坏

完成定义：
- 任务范围内代码、测试、验证、自检全部完成
- 任务单已补充实际结果、验证记录与残留风险
- 当前任务单可从 `current` 改为 `done`

实际结果：
- 在 `chat-domain` 的 message feature 内新增了附件上传入口：`POST /api/channels/{channelId}/messages/attachments`。
- 新增上传命令/结果/响应模型，使调用方可以先上传 file / voice 附件，再拿到 canonical `objectKey` 继续走既有消息发送链路。
- 上传对象键采用 `channels/{channelId}/messages/{messageType}/accounts/{accountId}/{id}-{filename}` 形式，绑定频道、消息类型和上传账户范围。
- 在 `MessageApplicationService` 的出站结果整形与 `NettyMessageRealtimePublisher` 的 realtime 下发中接入统一 `MessageAttachmentPayloadResolver`，为 file / voice payload 派生 `access_url` 与 `access_url_expires_at`。
- 数据库存储语义保持不变：消息 payload 仍只持久化 canonical `objectKey` 与原始元信息，不持久化临时 URL。
- 在 file / voice plugin 中新增 objectKey 范围校验，要求发送时使用的对象键必须匹配当前 `channelId + messageType + senderId` 约束，避免绕过上传入口约束复用任意对象键。
- 已补充/更新应用层、协议层、payload resolver、plugin 与 realtime 相关测试。

验证记录：
- 定向验证：
  - `mvn -pl chat-domain -am -Dtest=MessageAttachmentPayloadResolverTests,MessageApplicationServiceTests,ChannelMessageControllerTests,NettyMessageRealtimePublisherTests -Dsurefire.failIfNoSpecifiedTests=false test`
  - 结果：通过
- 范围收紧后的附加验证：
  - `mvn -pl chat-domain -am -Dtest=MessageAttachmentPayloadResolverTests,MessageApplicationServiceTests,ChannelMessageControllerTests,NettyMessageRealtimePublisherTests,FileChannelMessagePluginTests,VoiceChannelMessagePluginTests,SendFileMessageRealtimeHandlerTests,SendVoiceMessageRealtimeHandlerTests -Dsurefire.failIfNoSpecifiedTests=false test`
  - 结果：通过
- 受影响模块完整门禁：
  - `mvn -pl chat-domain -am test -DskipTests=false`
  - 结果：通过（`chat-domain` 103 tests passed）
- 诊断补充：尝试执行 LSP diagnostics，但当前环境 Java LSP 初始化超时；以 Maven 编译与完整测试通过作为替代验证依据。

残留风险：
- 当前附件访问 URL 仍为读取/下发时动态派生，属于短时有效链接；前端若长时间持有旧消息结果，需要自行处理过期后的重新拉取。
- `MessageAttachmentPayloadResolver` 在 presign 失败时回退原始 payload，以保证读取链路可用；这更偏可用性优先，后续如需更强可观测性，可考虑补充日志或监控。
- 本轮没有引入异步 transcript 流程；voice 的 transcript 仍仅支持发送时同步传入。

知识沉淀 / 是否回写 docs：
- 本轮未引入新的长期仓库级规则，暂不回写 `docs/`。
- 可复用经验已体现在本任务单中：对于附件型消息，建议保持“入库存 canonical objectKey，出站派生临时访问 URL”的边界分离方式。

产物清理与保留说明：
- 保留当前任务单作为本轮“真实可用化”工作的追溯记录。

补充说明：
- 若实现中发现必须扩张为 PUT 预签名上传协议、消息 payload 结构升级或新的配置入口，应先更新本任务单并重新评估边界。
