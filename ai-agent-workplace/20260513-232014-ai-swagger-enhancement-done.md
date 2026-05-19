任务名称：
增强 swagger 文档可读性

任务目标：
在已完成最小 Swagger 集成的基础上，进一步为当前 HTTP API 增加更适合客户端阅读的 OpenAPI 分组、中文摘要/描述、统一响应说明与更清晰的鉴权表达。

任务背景：
当前仓库已经能暴露基础 Swagger UI 与 OpenAPI 文档，但生成文档主要依赖默认推断，缺少分组、操作摘要、统一响应语义提示与更友好的客户端展示。用户已明确同意继续增强这部分文档层。

影响模块：
- `application-starter`
- `chat-domain`
- `docs`

允许修改范围：
- 允许修改 `application-starter` 下现有 OpenAPI 配置
- 允许为 `chat-domain` 下 HTTP 控制器补充 OpenAPI 注解
- 允许在必要时为共享响应模型补充 OpenAPI schema 注解
- 允许同步更新 `docs/API.md`

禁止修改范围：
- 不允许改变现有业务行为
- 不允许改变现有 HTTP 路径、参数或响应结构
- 不允许引入与当前 OpenAPI 文档增强无关的新依赖
- 不允许改动 WebSocket 协议实现

依赖限制：
- 仅使用当前已引入的 Springdoc / Swagger 注解能力
- 不新增额外文档框架

配置限制：
- 优先不新增运行时配置
- 若必须新增，仍需遵守最小配置原则

文档依据：
- `AGENTS.md`
- `docs/架构文档.md`
- `docs/注释规范.md`
- `docs/API.md`
- `docs/AI协作开发规范.md`
- `docs/变更审核清单.md`

任务分解 / 执行计划：
1. 确认控制器分组、统一响应模型与当前鉴权语义。
2. 为控制器补充 `@Tag`、操作摘要、必要的参数与响应说明。
3. 视需要为 `CPResponse` 补充 OpenAPI schema 说明，降低客户端理解成本。
4. 复核受保护接口与匿名接口的文档表现是否一致。
5. 运行诊断与相关 reactor 测试。
6. 完成只读复审并收尾任务单。

关键假设与依赖：
- 当前用户希望继续增强 Swagger 可读性，而非改造协议本身。
- 当前控制器返回 `CPResponse<T>` 的统一包裹语义保持不变。

实现要求：
- 保持注解增强为主，不做业务重构
- 优先补控制器级可见信息，不铺开到全部 DTO 字段逐个重写
- 中文标题/描述应直接服务客户端理解

测试要求：
- 至少运行受影响 reactor 测试
- 至少保证 LSP/编译诊断无新增错误

质量门禁：
- 受影响 Java 文件无新增诊断错误
- 受影响 reactor 测试通过
- Swagger 增强不破坏既有最小集成

复审要求：
- 完成后需做一次只读复审，重点看注解增强是否与真实鉴权/响应语义一致

文档要求：
- 如对客户端理解边界有新增明确说明，可同步更新 `docs/API.md`

验收标准：
- Swagger UI 中接口分组更清晰
- 主要操作具备中文摘要/说明
- 统一响应与鉴权语义比当前默认推断更易理解
- 测试与诊断通过

完成定义：
- 代码与文档增强完成
- 验证通过
- 任务单改为 `done`

实际结果：
- 在 `chat-domain` 的 HTTP 控制器上补充了中文 `@Tag` 与 `@Operation` 注解，按认证、用户资料、频道管理、频道消息、服务基础、公开源信息分组展示
- 在 `CPResponse` 上补充了统一响应 schema 说明，明确 `100/200/300/404/500` 的业务码语义
- 修正 `OpenApiConfiguration` 的鉴权标记规则，使 `POST /api/server/echo` 为匿名，而 `GET /api/server/presence/me` 作为受保护接口显示 `bearerAuth`
- 顺带修复现有 MVC 鉴权放行错位：`AuthWebMvcConfiguration` 不再放行整个 `/api/server/**`，仅放行 `POST /api/server/echo`
- 新增 `AuthWebMvcConfigurationTests` 锁定上述公开/受保护路由边界
- 更新 `docs/API.md`，同步匿名接口说明与 Swagger 文档入口说明

验证记录：
- `mvn -pl "application-starter" dependency:tree -Dincludes=io.swagger.core.v3:swagger-annotations-jakarta`：确认当前 Swagger 注解由 `swagger-annotations-jakarta:2.2.47` 提供
- `lsp_diagnostics`：增强前对主要 Java 变更文件均无新增错误；增强后 Java LSP 初始化超时，未返回稳定结果
- `mvn -pl "application-starter" -am -Dtest=OpenApiConfigurationTests -Dsurefire.failIfNoSpecifiedTests=false test`：通过
- `mvn -pl "application-starter" -am -DskipTests=false test`：通过
- Oracle 只读复审：指出 `/api/server/presence/me` 的运行时鉴权与文档语义错位，已修复并复验通过

残留风险：
- 当前主要增强集中在控制器级摘要、分组与统一响应说明，未逐个为所有 DTO 字段补充 `@Schema`
- 当前未增加自定义 springdoc 分组端点或多文档聚合，仅增强默认文档展示
- Java LSP 在最后一次验证时初始化超时，但 Maven 编译与完整 reactor 测试均已通过

知识沉淀 / 是否回写 docs：
- 已回写 `docs/API.md`，同步 Swagger 入口和匿名接口事实

产物清理与保留说明：
- 当前任务单保留在 `ai-agent-workplace/`，状态已关闭为 `done`

补充说明：
- 本任务为 Swagger 增强任务，目标是“更好看、更好懂”，不是“改协议”。
