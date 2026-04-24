任务名称：

well-known server public info endpoint

任务目标：

按 `docs/产品需求文档.md` 的 P0 优先项，在不突破当前模块边界的前提下实现 `GET /.well-known/carrypigeon-server`，向匿名调用方暴露最小可用的服务端公开源信息。

任务背景：

- `docs/产品需求文档覆盖矩阵.md` 明确 `GET /.well-known/carrypigeon-server` 仍为未实现能力。
- `docs/产品需求文档优先级待办清单.md` 已将其列为 P0 第一项。
- 当前仓库只有 `GET /api/server/summary`，返回内容较弱，且不满足 PRD 对 URL 添加服务端的标准公开源信息契约要求。

影响模块：

- `chat-domain`
- `application-starter`（仅当测试装配需要同步调整时）
- `ai-agent-workplace`

允许修改范围：

- 允许在 `chat-domain/features/server` 内新增或调整 controller / application / dto / tests。
- 允许在测试装配中补齐必要的 bean 或替身。
- 允许维护本任务单。

禁止修改范围：

- 不修改模块依赖方向。
- 不引入新依赖。
- 不把核心协议逻辑放进 `application-starter`。
- 不一次性实现 PRD 中全部公开源字段与插件能力模型。

文档依据：

- `AGENTS.md`
- `docs/AI协作开发规范.md`
- `docs/任务单模板.md`
- `docs/产品需求文档.md`
- `docs/产品需求文档覆盖矩阵.md`
- `docs/产品需求文档优先级待办清单.md`
- `docs/变更审核清单.md`

任务分解 / 执行计划：

1. 阅读当前 server feature 的公共接口、应用服务和测试模式。
2. 在任务边界内确定最小字段集与响应形状。
3. 实现 `GET /.well-known/carrypigeon-server`。
4. 补充 controller / service / starter 装配相关测试。
5. 执行受影响模块与全量 verify 验证。
6. 复审并关闭任务单。

关键假设与依赖：

- 默认采用当前仓库统一的 `CPResponse` 包装，而不是额外引入第二套裸 JSON 协议风格。
- 当前最小字段集仅覆盖仓库已有稳定来源：`server_id`、`server_name`、`register_enabled`、`login_methods`。
- `public_plugins`、`public_capabilities`、`public_base_url`、`realtime_endpoint`、`server_version` 暂不在本任务中实现。

实现要求：

- 新接口必须匿名可读。
- 必须放在 `chat-domain/features/server` 下完成，不新建无关模块。
- 优先复用现有 server 应用服务和 DTO 风格。
- 保持最小实现，不为未来字段预埋过重结构。

测试要求：

- 至少补充：匿名调用成功、字段返回正确、受保护接口匿名仍返回 300 的既有边界不被破坏。
- 相关测试命名、注释和分级标签需符合现有规范。

质量门禁：

- 受影响模块测试通过。
- 全量 `mvn -pl application-starter -am verify -DskipTests=false` 通过。
- 任务单记录实际结果、验证记录、残留风险。

实际结果：

- 已在 `chat-domain/features/server` 内新增 `GET /.well-known/carrypigeon-server`。
- 新增公开源信息 DTO：`WellKnownServerDocument`，当前最小字段集为：
  - `server_id`
  - `server_name`
  - `register_enabled`
  - `login_methods`
- 当前实现继续沿用仓库现有 `CPResponse` 包装，不引入第二套裸 JSON 响应风格。
- 公开字段来源：
  - `server_id` -> `ServerIdentityProperties`
  - `server_name` -> `spring.application.name`
  - `register_enabled` -> 固定 `true`
  - `login_methods` -> 固定 `username_password`
- 现有 `/api/server/summary` 保留不变，作为基础摘要接口继续存在。
- 已同步更新：
  - `docs/产品需求文档覆盖矩阵.md`
  - `docs/产品需求文档优先级待办清单.md`

验证记录：

- 运行命令：`mvn -pl chat-domain -am -Dtest=ServerControllerTests,ServerApplicationServiceTests test -DskipTests=false -Dsurefire.failIfNoSpecifiedTests=false`
- 结果：通过。
- 运行命令：`mvn -pl application-starter -am verify -DskipTests=false`
- 结果：通过。
  - `chat-domain`：155 个测试通过，JaCoCo 门禁通过。
  - `database-impl`：49 个测试通过，JaCoCo 门禁通过。
  - `application-starter`：11 个测试通过，JaCoCo 门禁通过。
- 本次实现过程中的真实问题与修复：
  - 修复了 standalone MockMvc 环境下 JSON 命名断言与实际输出不一致的问题。

残留风险：

- 当前只实现了 PRD 最小公开源信息字段，`public_plugins`、`public_capabilities`、`public_base_url`、`realtime_endpoint`、`server_version` 等仍待后续补齐。
- 当前使用 `CPResponse` 包装而非裸 JSON；这与现有仓库协议风格一致，但若未来客户端协议需要独立 well-known 原始文档，可能需要再评估。

知识沉淀 / 是否回写 docs：

- 若公开源信息契约形成稳定实现边界，应同步更新覆盖矩阵与优先级待办清单。

产物清理与保留说明：

- 本任务单已关闭为 `done`。
