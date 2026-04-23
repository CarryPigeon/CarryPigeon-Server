任务名称：
附件链路收口优化

任务目标：
在不改变现有 file / voice 可用化外部行为、不改变数据库 payload 持久化语义的前提下，优化附件链路的局部设计质量：统一 objectKey 规则来源、统一 resolver Bean 装配，并为 presign 回退增加最小可观测性。

任务背景：
上一轮已经完成 file / voice 消息“真实可用化”，包括：
1. 附件上传入口；
2. 历史查询 / 搜索 / realtime 下发时派生 presigned URL；
3. canonical objectKey 入库；
4. 发送链路中基于 channelId / messageType / senderId 的 objectKey 范围约束。

当前剩余的主要问题已收敛为三类局部优化：
1. objectKey 规则在上传和 plugin 校验中重复；
2. `MessageAttachmentPayloadResolver` 已有 Bean，但 realtime 配置仍手工 new 一份；
3. presign 失败会静默回退原始 payload，不利于运行时排查。

影响模块：
- `chat-domain`
- `ai-agent-workplace`

允许修改范围：
- 允许修改 `chat-domain/features/message/**` 中与附件 key 规则、payload resolver、测试直接相关的代码
- 允许修改 `chat-domain/features/server/**` 中与 realtime publisher 配置直接相关的代码
- 允许新增 message feature 内部局部 helper / policy
- 允许补充或改写相关测试

禁止修改范围：
- 不允许修改模块依赖方向
- 不允许新增第三方依赖
- 不允许改变上传/发送/历史查询/realtime 的对外协议字段
- 不允许修改数据库 schema 或消息 payload 持久化语义
- 不允许扩展为新的媒体中心、文件中心或通用资源域模型

依赖限制：
- 继续使用现有 Spring Boot / Jackson / Log4j2 / 项目基础设施能力
- `chat-domain` 只能继续依赖 `storage-api`，不得越界到 impl

配置限制：
- 不新增配置项
- `MessageAttachmentPayloadResolver` 的 TTL 行为保持现状，不在本轮扩展为配置化能力

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
1. 抽取 message feature 内部的附件 objectKey policy/helper，统一上传构建规则和 plugin 校验规则。
2. 调整 realtime 配置，复用已有 `MessageAttachmentPayloadResolver` Bean，避免 HTTP / realtime 行为未来漂移。
3. 在不改变 fallback 语义的前提下，为 presign 失败回退增加最小 warning 日志。
4. 补充/更新 helper、resolver、config 与相关 plugin/service 测试。
5. 运行定向测试与受影响模块完整门禁。
6. 记录结果、自检与残留风险，将任务单改为 `done`。

关键假设与依赖：
- 当前外部行为已可接受，本轮只做局部优化，不再新增功能。
- presign 回退仍保持“返回原始 payload”这一可用性优先策略。
- objectKey 格式必须与当前已上线实现保持兼容：`channels/{channelId}/messages/{messageType}/accounts/{accountId}/{id}-{sanitizedFilename}`。

实现要求：
- helper / policy 必须保持在 message feature 本地，不引入新的跨 feature 抽象。
- file / voice plugin 现有错误消息保持稳定，不因重构改变对外可见校验文案。
- realtime 配置必须复用已装配 Bean，而不是重新 new resolver。
- logging 只能增强可观测性，不得改变协议返回或异常映射。

测试要求：
- 覆盖 objectKey helper 的构建与 sender-scope 判断。
- 覆盖 file / voice plugin 在 helper 重构后仍保持既有校验语义。
- 覆盖 realtime 配置复用 resolver Bean。
- 覆盖 presign 回退日志 / fallback 语义。
- 保持既有 file / voice 主链路测试继续通过。

质量门禁：
- 受影响 Java 文件无新增编译错误
- 定向测试通过
- `mvn -pl chat-domain -am test -DskipTests=false` 通过
- 不引入新的模块边界违规

复审要求：
- 重点复审 objectKey 规则是否真正收口为单一来源
- 重点复审 realtime 是否复用共享 resolver Bean
- 重点复审 presign 回退行为是否保持不变且新增日志不会破坏测试稳定性

文档要求：
- 若未形成新的长期仓库规则，则不修改 `docs/`
- 任务过程与结论记录在本任务单

验收标准：
- objectKey 构建与范围判断规则在代码中有统一来源
- realtime 使用已装配的 `MessageAttachmentPayloadResolver` Bean
- presign 失败时仍回退原始 payload，但可被日志观测到
- 现有上传/发送/读取/realtime 对外行为不变

完成定义：
- 任务范围内代码、测试、验证、自检全部完成
- 任务单已补充实际结果、验证记录与残留风险
- 当前任务单可从 `current` 改为 `done`

实际结果：
- 已在 `chat-domain` 内新增本地 `MessageAttachmentObjectKeyPolicy`，统一附件 objectKey 构建、文件名归一化与 sender-scope 前缀判断规则。
- `MessageApplicationService` 上传链路已改为复用该 policy 生成 canonical objectKey，保持既有格式 `channels/{channelId}/messages/{messageType}/accounts/{accountId}/{id}-{sanitizedFilename}` 不变。
- `FileChannelMessagePlugin` 与 `VoiceChannelMessagePlugin` 已改为复用同一 policy 做 sender-scoped objectKey 校验，现有校验错误文案保持不变。
- `RealtimeServerConfiguration` 已停止手工 new `MessageAttachmentPayloadResolver`，改为复用已装配 Bean。
- `MessageAttachmentPayloadResolver` 已在 presign 异常回退路径增加最小 warning logging，同时继续保持“返回原始 payload”的 fallback 语义与出站 payload 结构不变。
- 已补充/更新 helper、resolver、plugin、service、realtime config 与相关 realtime handler 测试。

验证记录：
- `mvn -pl chat-domain -am -Dtest=MessageAttachmentObjectKeyPolicyTests,MessageAttachmentPayloadResolverTests,FileChannelMessagePluginTests,VoiceChannelMessagePluginTests,MessageApplicationServiceTests,RealtimeServerConfigurationTests,NettyMessageRealtimePublisherTests -Dsurefire.failIfNoSpecifiedTests=false test -DskipTests=false` ✅
- `mvn -pl chat-domain -am test -DskipTests=false` ✅
- 全项目回归评估：
  - `mvn test -DskipTests=false` ✅
  - 结果：全仓测试通过，涉及 `infrastructure-basic`、`database-api`、`storage-api`、`chat-domain`、`database-impl`、`cache-api`、`cache-impl`、`storage-impl`、`application-starter` 等模块
  - 结论：当前与本轮修改直接相关的回归/装配/模块级测试已经覆盖到位，未发现必须在本轮继续补充项目级回归测试或新增集成测试的缺口
- 尝试对修改文件执行 `lsp_diagnostics`，但当前环境中的 Java LSP 初始化超时 / 退出，未能返回有效诊断结果；最终以 Maven 编译与测试门禁完成验证。

残留风险：
- 当前未发现本轮范围内残留风险。
- `MessageAttachmentPayloadResolver` 的 warning logging 已通过测试子类覆盖 presign fallback 记录路径；运行时仍使用实际 Log4j2 warning 输出。
- 全项目回归评估后，当前未发现由于本轮优化引出的额外项目级测试缺口。

知识沉淀 / 是否回写 docs：
- 本轮仅为局部优化，未形成新的长期仓库规则，不回写 `docs/`。

产物清理与保留说明：
- 保留当前任务单作为附件链路优化追溯记录。

补充说明：
- 若优化中发现必须改变 payload 结构、配置模型或异常策略，应先更新任务单再继续。
