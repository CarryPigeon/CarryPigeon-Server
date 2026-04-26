任务名称：
深度探索结果整改优先级清单

任务目标：
基于上一轮“深度探索式缺口审查”的发现，整理一份可直接用于排期和整改的优先级清单，明确先做什么、为什么先做、涉及哪些模块、建议如何收敛。

任务背景：
仓库已经完成一轮只读深度挖掘，并形成了缺口任务单。为了便于后续落地，需要把“问题清单”压缩为“带优先级的整改 backlog”。

影响模块：
- `application-starter`
- `chat-domain`
- `infrastructure-basic`
- `infrastructure-service/*`
- `distribution`
- `docs`
- `docker-compose.yaml`
- `ai-agent-workplace`

允许修改范围：
- 仅允许在 `ai-agent-workplace/` 内新增整改清单任务单

禁止修改范围：
- 不修改任何正式源码
- 不修改任何 `docs/` 正式文档
- 不修改测试、配置、POM、Docker 文件

依赖限制：
- 不引入新依赖
- 仅基于已有探索结论和仓库证据整理 backlog

配置限制：
- 不新增配置
- 不调整配置值

文档依据：
- `AGENTS.md`
- `docs/AI协作开发规范.md`
- `docs/变更审核清单.md`
- `docs/任务单模板.md`
- `ai-agent-workplace/20260425-230126-ai-deep-exploration-gap-audit-done.md`

任务分解 / 执行计划：
1. 提炼上一轮探索中的高优先级问题。
2. 以“立即处理 / 近期补齐 / 后续增强”三层方式整理 backlog。
3. 为每项给出影响模块、问题说明、优先级原因和建议动作。
4. 落地到 AI 工作目录，供后续排期使用。

关键假设与依赖：
- 本清单用于后续实施排期，不等于立即执行。
- 优先级排序以风险、影响范围、工程收益和与现有规则冲突程度综合判断。

实现要求：
- 不把“可优化项”与“当前存在风险的问题”混在同一优先级。
- 每一项都必须尽量指向明确模块与建议动作方向。
- 优先级说明必须可读、可执行、可复盘。

测试要求：
- 本任务为整理 backlog，不新增测试

质量门禁：
- 结果可直接被后续任务拆解使用
- 结构清晰，优先级明确
- 不修改正式代码与文档

复审要求：
- 与上一轮探索结论保持一致，不擅自新增未验证问题

文档要求：
- 仅在 `ai-agent-workplace/` 记录
- 不回写 `docs/`

验收标准：
- 输出一份按优先级组织的整改 backlog
- 每项都包含问题、影响范围、建议动作和优先级理由

完成定义：
- 任务单已写入 AI 工作目录
- backlog 已足够支持下一步拆解为多个正式实施任务

实际结果：
- 已完成基于深度探索结论的整改优先级清单整理。

验证记录：
- 依据任务单 `ai-agent-workplace/20260425-230126-ai-deep-exploration-gap-audit-done.md` 中已确认的问题进行整理，无新增未经验证的问题。

残留风险：
- 本清单仍需人工或后续 AI 按业务节奏做最终排期裁剪。
- 部分问题之间存在依赖关系，拆任务时需要避免交叉返工。

知识沉淀 / 是否回写 docs：
- 当前不回写 `docs/`
- 若后续某项整改形成长期规则，再单独沉淀进正式文档

产物清理与保留说明：
- 保留本任务单于 `ai-agent-workplace/`
- 后续如 backlog 被拆分并正式吸收，可移入 `archive/`

补充说明：
- 建议整改优先级清单如下：

## P0：应尽快处理（结构性风险 / 影响启动与交付可靠性）

### 1. 收紧 realtime 启动时序，确保初始化检查先于监听放流量
- 影响模块：`application-starter`、`chat-domain`
- 问题：当前 realtime listener 的启动方式与共享初始化检查执行时序之间存在冲突迹象，可能违背“先检查后放流量”的架构规则。
- 为什么优先：这是明确的运行时风险，一旦开启 realtime，可能直接影响服务对外暴露安全性。
- 建议动作：统一 realtime listener 生命周期入口，只保留一种清晰受控的启动路径，并让它显式依赖 initialization checks 成功。

### 2. 决定 `distribution` 的命运：修正为可用，或明确废弃
- 影响模块：`distribution`、根 `pom.xml`、`application-starter`
- 问题：`distribution` 模块存在，但不在根 reactor 中，assembly 规则与当前可执行产物也存在漂移。
- 为什么优先：这是典型“看起来存在、实际不可靠”的交付链路问题，容易误导后续打包或发布。
- 建议动作：二选一：
  - 若保留，就让它进入正式构建链，并修正 assembly 规则；
  - 若当前不用，就明确标记废弃或冻结，避免继续形成错误预期。

### 3. 建立可见的全仓自动化验证入口
- 影响模块：仓库根、全部模块
- 问题：仓库里没有可见的 CI workflow 证据；当前只能看到本地 Maven 门禁，不足以证明每次变更都会自动跑全仓验证。
- 为什么优先：这是工程可靠性的基础，没有它，局部通过但整体损坏会持续发生。
- 建议动作：建立最基本的自动化链路，至少覆盖：
  - `mvn test -DskipTests=false`
  - `mvn clean install`

## P1：近期补齐（高收益质量改进，能明显减少回归）

### 4. 提升测试门禁强度，逐步抬高覆盖率底线
- 影响模块：根 `pom.xml`
- 问题：JaCoCo 仅 30% line / branch，门槛过低。
- 为什么优先：门槛过低会让“测试存在”失去质量约束意义。
- 建议动作：不要一步拉满；先补关键缺口，再逐步提高门槛到更有约束力的水平。

### 5. 补齐 `ChannelControllerTests` 失败路径矩阵
- 影响模块：`chat-domain`
- 问题：频道控制器端点多，但失败路径覆盖明显不足。
- 为什么优先：这是用户可直接感知的接口面，且 endpoint 数量多、治理动作复杂，最容易漏回归。
- 建议动作：按端点补齐以下最小矩阵：匿名、参数非法、权限不足、not found、unexpected failure。

### 6. 补齐 `AuthControllerTests`、消息附件/查询、ServerController 的失败路径
- 影响模块：`chat-domain`
- 问题：auth、message、server 控制器都还有负路径缺口，但严重度次于 channel controller。
- 为什么优先：这些接口直接决定认证稳定性、消息上传体验和基础服务可观察性。
- 建议动作：优先覆盖：
  - login / refresh / logout 异常路径
  - 附件上传的匿名、文件缺失、读取失败路径
  - message search / recall 的 forbidden / not found / 500
  - presence / well-known 的更完整 HTTP 断言

### 7. 为 Realtime 关键分支补齐系统级验证
- 影响模块：`chat-domain`
- 问题：WS 测试虽已有基础，但生命周期、异常、未初始化分支仍偏局部。
- 为什么优先：Realtime 路径容易因线程模型、会话状态、异常回写而出现边界 bug。
- 建议动作：补齐至少以下验证：
  - missing principal
  - null service / dispatcher
  - malformed body
  - exception / disconnect cleanup
  - 更少依赖 `contains(...)` 字符串断言，更多用结构化解析

### 8. 给 `application-starter` 增加更真实的启动级验证
- 影响模块：`application-starter`
- 问题：目前 smoke/context 类测试存在，但缺少真正贴近运行入口的启动验证。
- 为什么优先：starter 是最终装配层，不验证真实启动路径，很多 wiring 问题会滞后暴露。
- 建议动作：增加至少一类更接近真实运行的启动测试，证明核心装配链可工作。

## P2：后续增强（短期不一定阻塞，但应纳入路线图）

### 9. 将数据库 / 缓存 / 存储从 mock-heavy 测试逐步推进到真实集成验证
- 影响模块：`infrastructure-service/database-impl`、`cache-impl`、`storage-impl`
- 问题：当前很多验证仍停留在 auto-config、property、mock mapper 层。
- 为什么不是更高优先：短期内不一定立刻出事故，但长期会掩盖 wiring / SQL / client integration 问题。
- 建议动作：逐步引入更真实的集成验证，而不是一次性重做全部测试。

### 10. 完善运维就绪度：健康、readiness、配置暴露与本地 bootstrap
- 影响模块：`application-starter`、`infrastructure-service/*-impl`、`docker-compose.yaml`、相关 docs
- 问题：当前有 startup checks，但缺少更完整的运维暴露面；本地 MinIO bucket readiness/bootstrap 也不完整。
- 为什么不是 P0：已有内部检查机制，但还不够“部署友好”和“观测友好”。
- 建议动作：后续统一考虑：
  - readiness / liveness 暴露
  - 更完整的 health surface
  - 本地依赖 bootstrap（特别是 MinIO bucket）
  - 结构化日志中的上下文一致性

### 11. 收敛抽象层中“迁移中设计”的痕迹
- 影响模块：`chat-domain`、`database-api` 等抽象层
- 问题：个别接口仍通过 `UnsupportedOperationException("... not supported")` 表达“当前不支持”。
- 为什么不是 P0：短期可运行，但说明抽象面仍未完全自然收敛。
- 建议动作：后续重构时检查这些接口是否应缩窄职责，避免“名义抽象 > 实际能力”。

### 12. 继续观察但暂不处理：其它模块的手写 accessor
- 影响模块：`chat-domain`、`infrastructure-basic`
- 问题：除 `database-impl` 外，剩余的手写 accessor 主要是低收益或不适合机械替换的场景。
- 为什么是低优先：当前没有大面积样板代码污染，收益明显低于前述结构性问题。
- 建议动作：暂时不做批量 Lombok 化；只在后续局部重构时顺手整理。
