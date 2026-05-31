任务名称：

以 `docs/t` 为基准的 API 协议整改任务单

任务目标：

以 `docs/t` 下的客户端基准 API 为唯一整改基准，对当前代码中仍与其不一致的 HTTP / WebSocket API 进行系统性修正，优先消除阻塞客户端联调的协议差异，并形成可分轮执行、可验证、可关闭的整改闭环。

任务背景：

当前仓库已经完成 round1-6 的重写主线实现与文档收口，但深度审查结果显示：

- 当前项目 API 与 `docs/t` 只能算“部分一致”，尚未达到完整一致
- 当前 `docs/API.md` 已基本反映代码现状，但它记录的是“当前实现事实”，不是 `docs/t` 的完整目标协议
- 对客户端真正构成阻塞的不一致主要集中在：
  - WS 连接地址与能力声明
  - 消息搜索查询参数与分页语义
  - 频道治理与申请相关接口族
  - 文件上传 / 下载模型
  - mentions / read state / unreads 等接口缺失

因此需要单独创建整改任务单，明确：

- 哪些差异属于阻塞级，需要优先改正
- 哪些差异属于过渡兼容面，可后置处理
- 允许修改哪些模块与文件
- 如何验证“与 `docs/t` 一致”而不是仅“代码能跑”

影响模块：

- `chat-domain`
- `application-starter`
- `infrastructure-service/database-api`
- `infrastructure-service/database-impl`
- `infrastructure-service/storage-api`
- `infrastructure-service/storage-impl`
- `docs/`
- `ai-agent-workplace/`

允许修改范围：

- 与 `docs/t` 基准协议直接相关的 controller、DTO、application service、repository 适配器、realtime handler / publisher / dispatcher
- 为协议一致性所必需的 database-api / storage-api 最小契约扩展
- 与整改项直接对应的正式测试
- `docs/API.md` 及必要的正式文档更新
- 任务单与关闭记录

禁止修改范围：

- 不引入未确认的新架构模式
- 不因为整改方便而重写无关业务逻辑
- 不新增与当前协议整改无关的配置、依赖或中间件
- 不把“为了兼容旧接口”扩张成长期双协议并行体系
- 不在未明确必要性的情况下扩大到 Kubernetes / 多节点 / 生产部署方案

依赖限制：

- 严格遵守现有模块依赖方向
- 原则上不新增依赖；如必须新增，需单独确认

配置限制：

- 仅允许为协议一致性、测试可执行性或运行时路径对齐所需的最小配置调整
- 不新增未来占位配置

文档依据：

- `docs/AI协作开发规范.md`
- `docs/变更审核清单.md`
- `docs/任务单模板.md`
- `docs/架构文档.md`
- `docs/包结构规范.md`
- `docs/异常与错误码规范.md`
- `docs/测试规范.md`
- `docs/API.md`
- `docs/t/SERVER_API.md`
- `docs/t/10-http-ws-protocol-v1.md`
- `docs/t/11-http-endpoints-v1.md`
- `docs/t/12-ws-events-v1.md`
- `docs/t/13-error-model-and-reasons-v1.md`
- `docs/t/14-pagination-and-cursor-v1.md`

任务分解 / 执行计划：

1. 基于审查矩阵，把差异按“阻塞级 / 高风险 / 兼容过渡”分层归类。
2. 先处理阻塞客户端联调的协议差异：
   - WS 地址与连接模型
   - 消息搜索接口参数与响应语义
   - 频道治理资源路径
   - 文件上传 / 下载路径与模型
3. 再处理高风险但可分步推进的协议差异：
   - `resume` 持久化 / 跨实例语义
   - `message.created` 发送者快照补全
   - 旧过渡接口的下线策略
4. 为每类整改项补齐对应 controller / application / persistence / ws 契约测试。
5. 更新 `docs/API.md`，确保其反映整改后的实际代码，而不是整改前过渡状态。
6. 按变更审核清单执行最终自检，记录残留风险和关闭结论。

关键假设与依赖：

- 当前 round1-6 主线实现已经作为整改基线存在，无需回滚到旧协议
- 本次整改以 `docs/t` 为目标真值，`docs/API.md` 只是当前实现文档，需要跟随代码更新
- 若整改过程中发现 `docs/t` 某些条目本身与产品/实现现实冲突，需要单独显式确认，而不是默认以代码现状覆盖 `docs/t`

实现要求：

- 所有对外协议判断以 `docs/t` 为准，而不是以当前过渡实现为准
- 优先修复“客户端会直接调用失败或解析失败”的接口差异
- 对于仍需保留的过渡接口，必须在任务单和最终输出中明确列出，不得默默保留
- WebSocket 整改优先保证：
  - `/api/ws` 路径一致性
  - `auth/reauth` 一致性
  - `event` envelope 一致性
  - `resume` 语义一致性
- HTTP 整改优先保证：
  - 路径 / 方法 / 成功响应对象一致
  - 错误 HTTP 状态与 `error.reason` 一致
  - 分页与 cursor 语义一致
  - snowflake ID / 时间 / snake_case 一致

测试要求：

- 至少覆盖每个阻塞级整改项的成功与失败路径
- 对涉及 controller 路径、参数和响应模型的整改，必须有 `contract` 级测试
- 对涉及仓储契约或基础设施适配的整改，必须有 repository / database service 测试
- 对涉及 WS 协议的整改，必须覆盖：
  - auth
  - reauth
  - ping/pong
  - resume success / failure
  - message.created
  - message.deleted
  - 关键路径错误输出
- 完整改造完成前，需至少执行一轮受影响 reactor 的完整 Maven 测试

质量门禁：

- 与整改范围匹配的 controller / application / persistence / ws 契约测试通过
- 受影响 reactor 的 `mvn test -DskipTests=false` 通过
- `docs/API.md` 与整改后代码一致
- 残留的 `current` / `done` 任务单状态明确且可追踪
- 最终结论能明确回答：当前项目 API 是否已经与 `docs/t` 一致

复审要求：

- 必须进行一次面向客户端兼容性的深度复审
- 复审重点：
  - 是否仍存在阻塞级不一致项
  - 是否只是 `docs/API.md` 同步了代码，但代码仍未对齐 `docs/t`
  - 是否存在旧过渡接口未标注的隐性兼容债务

文档要求：

- `docs/API.md` 必须跟随整改结果更新
- 若整改结果形成稳定长期规则（例如旧过渡接口下线策略、WS replay 范围、文件模型固定方向），需评估是否回写到正式规则文档

验收标准：

- 以 `docs/t` 为基准审查时，所有阻塞级差异已消除
- 高风险差异若未完全消除，必须显式标记并给出明确剩余边界
- `docs/API.md`、代码、测试、任务单状态一致
- 最终可以产出一份更新后的“接口一致性矩阵”，说明当前仍有哪些差异（若存在）

完成定义：

- 阻塞客户端联调的协议差异整改完成
- 匹配整改范围的测试通过并留有证据
- 正式 API 文档完成更新
- 任务单记录实际结果、验证证据、残留风险与是否回写 `docs/`
- 任务单状态可从 `current` 改为 `done`

实际结果：
- 已按多批次完成 docs/t backlog 整改与实现收口，后续能力分别落入 batch4-batch15。
- 已消除最初盘点中的阻塞级不一致项，并通过分批定向 Maven 验证完成收口。
- 已覆盖并落地的主要能力包括：files、read_state/unreads、channel applications、bans list、pins、forward、mentions、message edit / model / WS、base contract cleanup、notification preferences、remote discover、audit logs。

验证记录：
- 各能力批次的定向 Maven 验证已分别记录在对应 `*-done.md` 任务单中。
- 当前 docs/t backlog 的主要整改批次均已收口并归档为 `done`。

残留风险：
- 当前仍可能存在非 docs/t 范围内的历史 OpenAPI/Swagger 整理任务单，但 docs/t backlog 已完成实现与验证收口。
- 若后续产品继续扩展新的客户端协议，不属于本轮 docs/t 收口范围。

知识沉淀 / 是否回写 docs：

- 待执行后评估

产物清理与保留说明：

- 保留本整改任务单、差异矩阵与最终关闭记录
- 不在源码目录中保留临时分析草稿

补充说明：

- 当前深度审查结论已明确：当前项目 API 与 `docs/t` 不是完整一致，而是“部分一致”。
- 本任务单的目标不是重新讨论是否要对齐，而是以 `docs/t` 为基准执行整改。
