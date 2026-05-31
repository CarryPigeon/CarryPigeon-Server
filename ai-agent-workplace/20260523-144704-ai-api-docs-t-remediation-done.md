任务名称：依据 `docs/t` 规范整改服务端 API

任务目标：
将当前代码中的对外 HTTP API、错误响应模型、OpenAPI 示例与相关 DTO 调整为与 `docs/t/` 下的 v1 API 规范一致，消除旧协议、旧字段命名和旧错误模型的残留。

任务背景：
`docs/t/` 已定义新的服务端对外协议基线，包括 `/api` 入口、`snake_case` JSON、十进制字符串雪花 ID、统一错误模型、统一分页结构，以及 HTTP/WS 的端点与事件清单。当前代码仍存在旧风格的 API 示例、错误包装和字段命名，需要按协议文档收敛。

影响模块：
- `chat-domain`
- `application-starter`
- `docs/`（仅在需要补充或修正长期协议说明时）

允许修改范围：
- `chat-domain/src/main/java/**/controller/**`
- `chat-domain/src/main/java/**/controller/error/**`
- `chat-domain/src/main/java/**/controller/advice/**`
- `chat-domain/src/main/java/**/dto/**`、`**/wire/**`、`**/response/**` 等对外模型
- `application-starter/src/main/java/**/OpenApiConfiguration.java` 及相关 OpenAPI 配置
- 与上述改动直接相关的测试
- 必要时补充 `docs/` 中与长期协议直接相关的说明

禁止修改范围：
- 不修改模块边界和依赖方向
- 不新增无必要的第三方依赖
- 不回退到旧的模块结构或旧 API 习惯
- 不修改与本次 API 对齐无关的业务逻辑
- 不把实现迁移到 `application-starter` 承载业务

依赖限制：
- 维持现有 Spring Boot / Lombok / Springdoc 基线
- 不引入新的协议栈或网关方案
- 仅按 `docs/t` 规范对齐当前实现，不擅自扩展协议

配置限制：
- 保持最小配置原则
- 不新增未来占位配置
- 若必须调整 OpenAPI 展示，只改文档装配，不改变运行时协议语义

文档依据：
- `docs/t/SERVER_API.md`
- `docs/t/10-http-ws-protocol-v1.md`
- `docs/t/11-http-endpoints-v1.md`
- `docs/t/12-ws-events-v1.md`
- `docs/t/13-error-model-and-reasons-v1.md`
- `docs/t/14-pagination-and-cursor-v1.md`
- `docs/架构文档.md`
- `docs/包结构规范.md`
- `docs/测试规范.md`
- `docs/AI协作开发规范.md`
- `docs/任务单模板.md`

任务分解 / 执行计划：
1. 对照 `docs/t` 梳理当前代码里所有对外 HTTP 接口、错误模型、分页响应和 OpenAPI 示例。
2. 逐个修正 API DTO、异常映射与示例，统一为协议要求的字段命名、ID 编码和错误结构。
3. 校正 OpenAPI 门户中的说明、示例和鉴权展示，使其不再泄露旧协议语义。
4. 补充或更新针对协议层的测试，验证成功路径、失败路径和关键字段约束。
5. 完成后做一次依赖边界与响应契约复核，确认没有把实现移出既定模块边界。

关键假设与依赖：
- 假设当前服务端对外 API 以 `chat-domain` 中的 HTTP controller 和 `application-starter` 的 OpenAPI 装配为主要入口。
- 假设 `docs/t/` 是本次整改的唯一协议基准，旧 `docs/API.md` 的内容不作为优先对齐对象。
- 若发现 WS 相关实现尚未进入当前代码，则仅记录为未覆盖项，不擅自补做超范围实现。

实现要求：
- 所有对外 JSON 统一为 `snake_case`
- 雪花 ID 在 JSON 中统一为十进制字符串
- 错误响应统一为 `error { status, reason, message, details? }`
- 分页统一为 `items / next_cursor / has_more`
- API 路径、参数名和错误 reason 必须与 `docs/t` 一致

测试要求：
- 补充或更新 API 层测试，覆盖成功与失败路径
- 对错误响应与分页结构增加断言
- 对 OpenAPI 展示层增加必要的文档/示例测试

质量门禁：
- 相关模块诊断无新增错误
- 相关测试通过
- 受影响模块可正常构建
- 变更后 API 示例与文档基线一致

复审要求：
- 需要复审
- 重点检查协议一致性、错误模型、字段命名、ID 编码和模块边界

文档要求：
- 如实现过程中发现 `docs/t` 与现有长期规则存在冲突，先更新任务单并确认，再决定是否回写 `docs/`

验收标准：
- 当前代码对外暴露的 API 与 `docs/t` 基线一致
- 旧 API 示例和旧错误语义不再出现在 OpenAPI 门户中
- 相关测试覆盖关键协议约束并通过

完成定义：
- 验收标准已满足
- 质量门禁已执行并记录
- 任务单状态可改为 `done`

实际结果：
- OpenAPI 门户已切换到 `docs/t` 的 HTTP/WS 协议基线说明，去掉了旧 `CPResponse` 成功包装叙述。
- 兼容性路由（如 `/.well-known`、`/api/auth/me`、`/api/server/presence/me` 等）已从 OpenAPI 公共面隐藏。
- 消息历史与搜索的单页上限已收敛到 `50`，并同步更新了相关校验文案。

验证记录：
- `lsp_diagnostics` 对所有改动文件无新增错误。
- `mvn -pl chat-domain -am -Dtest=AuthControllerTests,ServerControllerTests,ChannelMessageQueryControllerTests -Dsurefire.failIfNoSpecifiedTests=false test` 通过。
- `mvn -pl application-starter,chat-domain -am -Dtest=OpenApiConfigurationTests,AuthControllerTests,ServerControllerTests,ChannelMessageQueryControllerTests -Dsurefire.failIfNoSpecifiedTests=false test` 通过。

残留风险：
- `around_mid` / `after_mid` 这类 docs/t 里要求的更完整消息上下文语义，当前服务层还没有对应仓储能力，后续需要单独补齐。

知识沉淀 / 是否回写 docs：
- 默认不回写 `docs/`；仅在发现长期协议变更时再补充

产物清理与保留说明：
- 任务期间保留在 `ai-agent-workplace/`
- 完成后按命名规范改为 `done`

补充说明：
- 本任务单仅定义整改边界与验收，不直接包含实现代码
