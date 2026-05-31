任务名称：

基于客户端基准 API 的服务端 API 重写总控任务单

任务目标：

把“按 `docs/t` 客户端基准 API 重写服务端 API 设计与实现”的大任务拆解为多轮可控子任务，明确每轮目标、依赖、边界与停止条件，避免一次任务过重导致协议改造失控。

任务背景：

当前仓库现有 API 与 `docs/t` 客户端基准 API 存在系统性差异，后续重写不适合以单轮大任务推进。需要先形成总控任务单，统一管理多轮子任务单。

影响模块：

- `ai-agent-workplace/`
- 后续可能影响 `chat-domain`
- 后续可能影响 `application-starter`
- 后续可能影响 `docs/API.md`

允许修改范围：

- 当前仅允许修改 `ai-agent-workplace/` 下任务单

禁止修改范围：

- 当前不修改正式源码
- 当前不修改正式测试
- 当前不修改正式配置
- 当前不修改 `docs/` 长期规则

依赖限制：

- 当前不新增依赖

配置限制：

- 当前不修改配置

文档依据：

- `docs/AI协作开发规范.md`
- `docs/任务单模板.md`
- `docs/架构文档.md`
- `docs/包结构规范.md`
- `docs/API.md`
- `docs/t/SERVER_API.md`
- `docs/t/10-http-ws-protocol-v1.md`
- `docs/t/11-http-endpoints-v1.md`
- `docs/t/12-ws-events-v1.md`
- `docs/t/13-error-model-and-reasons-v1.md`
- `docs/t/14-pagination-and-cursor-v1.md`

任务分解 / 执行计划：

1. 第 0 轮：完成项目理解与协议差距基线梳理。
2. 第 1 轮：产出服务端 API 重写方案与迁移边界，确认是否兼容迁移或直接切换协议。
3. 第 2 轮：落地统一协议基础设施，包括错误模型、响应模型、ID/时间/分页编码规则。
4. 第 3 轮：重写公开接口与鉴权接口，包括 `/api/server`、gate、auth token 流程。
5. 第 4 轮：重写用户、频道、消息 HTTP 端点。
6. 第 5 轮：重写 WebSocket 实时协议、事件 envelope 与 resume 流程。
7. 第 6 轮：补齐测试、文档、联调核对与收口。

关键假设与依赖：

- 第 1 轮完成前，不进入正式协议实现。
- 任何会改变对外协议的编码工作，都必须建立在第 1 轮确认结果之上。
- 各轮默认按顺序推进；若某轮暴露更大的边界问题，应先回写任务单再继续。

实现要求：

- 每轮任务单必须可独立执行、可独立验证、可独立停止。
- 每轮应优先控制在单一主题内，不混杂协议设计、基础设施改造、业务端点重写和实时链路重写。

测试要求：

- 当前总控任务单不直接执行测试。
- 各实现轮次自行定义匹配的测试要求与质量门禁。

质量门禁：

- 已完成总控拆解。
- 子任务单的顺序、边界与依赖关系清晰。
- 能作为后续多轮推进的索引入口。

复审要求：

- 第 1 轮方案任务完成后，需要你确认迁移策略，再进入实现轮次。

文档要求：

- 当前仅维护任务单，不修改长期文档。

验收标准：

- 已形成多轮任务结构。
- 每轮任务目标与边界明确。
- 后续可以按子任务单逐轮执行而非一次性大改。

完成定义：

- 总控任务单已创建。
- 分轮子任务单已创建。
- 已保留第 0 轮基线任务单作为已完成输入。

实际结果：

- 已将大任务拆解为总控任务单与多轮子任务单。

验证记录：

- 文件检查：`sed -n '1,260p' ai-agent-workplace/20260519-202120-ai-project-api-baseline-current.md`
- 本任务仅调整任务单，不涉及代码实现与测试

残留风险：

- 若第 1 轮未先收敛迁移策略，后续实现轮次仍可能范围膨胀。

知识沉淀 / 是否回写 docs：

- 暂不回写 `docs/`

产物清理与保留说明：

- 保留本总控任务单作为后续子任务的统一索引入口。

补充说明：

- 子任务单列表：
- `20260519-202344-ai-api-redesign-round1-solution-current.md`
- `20260519-202344-ai-api-redesign-round2-protocol-foundation-current.md`
- `20260519-202344-ai-api-redesign-round3-server-auth-current.md`
- `20260519-202344-ai-api-redesign-round4-http-business-apis-current.md`
- `20260519-202344-ai-api-redesign-round5-ws-realtime-current.md`
- `20260519-202344-ai-api-redesign-round6-tests-docs-current.md`
