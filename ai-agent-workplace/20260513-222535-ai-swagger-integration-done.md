任务名称：
引入 swagger 文档能力

任务目标：
在不破坏现有模块边界、鉴权规则和最小配置原则的前提下，为当前 Spring Boot HTTP API 引入可访问的 Swagger UI 与 OpenAPI 文档端点。

任务背景：
当前仓库已有 `docs/API.md` 人工整理版接口文档，但源码中尚未接入 Swagger / OpenAPI 自动文档能力。用户明确要求引入 swagger，便于后续接口查看与联调。

影响模块：
- `application-starter`
- `chat-domain`（仅在必要时最小化补充控制器注解或放行规则；优先避免触碰）

允许修改范围：
- 允许修改 `application-starter/pom.xml`
- 允许修改 `application-starter/src/main/resources/application.yaml`
- 允许在 `application-starter/src/main/java/team/carrypigeon/backend/starter/config/` 下新增最小 OpenAPI 配置类
- 如确有必要，允许最小化修改现有 MVC 鉴权放行配置
- 允许同步更新对外 API 文档说明

禁止修改范围：
- 不允许改变模块依赖方向
- 不允许把 Swagger 依赖放入 `chat-domain`
- 不允许重构现有控制器业务逻辑
- 不允许新增与当前 Swagger 集成无关的配置项
- 不允许顺带修改 WebSocket 协议或业务接口行为

依赖限制：
- 仅允许引入当前任务确有需要的 Springdoc 依赖
- 新依赖应放在 `application-starter`，因为其职责是启动与运行时装配

配置限制：
- 只允许新增当前 Swagger 集成实际使用的最小配置
- 配置应保留在 `application-starter/src/main/resources/application.yaml`
- 不新增未来占位配置

文档依据：
- `AGENTS.md`
- `docs/架构文档.md`
- `docs/配置规范.md`
- `docs/依赖引入规范.md`
- `docs/API.md`
- `docs/AI协作开发规范.md`
- `docs/变更审核清单.md`

任务分解 / 执行计划：
1. 阅读现有启动装配、MVC 鉴权和配置边界，确认 Swagger 的最小落点。
2. 选择 Spring Boot 3 兼容的 Springdoc 依赖与最小配置方式。
3. 在 `application-starter` 中新增依赖与 OpenAPI 配置。
4. 如受 `/api/**` 鉴权拦截影响，则补齐 Swagger 相关公开端点放行。
5. 更新文档，说明 Swagger 访问路径与当前边界。
6. 运行相关测试 / 启动验证 / 诊断，并记录结果。
7. 复审变更，确认无越模块依赖、无多余配置、无接口行为回归。

关键假设与依赖：
- 已确认当前 HTTP API 由 Spring MVC 暴露，Swagger 应接入 MVC 而非 WebFlux。
- 已确认当前项目对外 HTTP 鉴权使用 MVC 拦截器而非 Spring Security 过滤链。
- 若实际扫描结果显示 Swagger 端点不会进入 `/api/**` 拦截范围，则无需额外放行配置。

实现要求：
- 必须保持 starter 负责运行时装配的职责边界
- 必须优先采用最小可运行集成，不先做全量注解美化
- 必须保持现有匿名/鉴权接口行为不变

测试要求：
- 至少验证相关模块测试 / 启动验证可通过
- 至少验证 Swagger JSON 端点和 UI 端点可访问

质量门禁：
- 受影响文件 LSP/编译诊断无新增错误
- 受影响 Maven reactor 测试或验证命令通过
- Swagger UI 与 OpenAPI 文档端点可访问
- 不破坏既有 `/api/**` 鉴权规则

复审要求：
- 该任务涉及新依赖、配置扩展和对外协议可见面变化，完成后需做独立复审

文档要求：
- 若引入 Swagger 后事实已改变，应同步更新 `docs/API.md` 中“当前未发现 Swagger/OpenAPI”的描述

验收标准：
- 项目启动后可访问 Swagger UI
- 可访问 OpenAPI JSON 文档端点
- 现有 HTTP API 行为未被 Swagger 接入破坏
- 改动符合模块边界和最小配置原则

完成定义：
- 代码、配置、必要文档修改完成
- 验证记录写入任务单
- 复审结论明确
- 当前任务单可在结束时改名为 `done`

实际结果：
- 在 `application-starter` 中引入 `springdoc-openapi-starter-webmvc-ui`
- 新增 `OpenApiConfiguration`，注册基础 OpenAPI 信息与 `bearerAuth` 鉴权方案
- 新增 `OpenApiConfigurationTests`，验证 OpenAPI Bean、Bearer 鉴权方案，以及按当前 `/api/**` 拦截规则自动标记受保护操作
- 更新 `docs/API.md`，补充 Swagger / OpenAPI 访问入口，并移除“当前未发现 Swagger/OpenAPI”的过时表述
- 未修改现有业务控制器与 `/api/**` 鉴权拦截规则

验证记录：
- `lsp_diagnostics application-starter/src/main/java/team/carrypigeon/backend/starter/config/OpenApiConfiguration.java`：通过
- `lsp_diagnostics application-starter/src/test/java/team/carrypigeon/backend/starter/config/OpenApiConfigurationTests.java`：通过
- `mvn -pl "application-starter" -am -Dtest=OpenApiConfigurationTests -Dsurefire.failIfNoSpecifiedTests=false test`：通过
- `mvn -pl "application-starter" -am -DskipTests=false test`：通过
- Oracle 只读复审：通过；指出初版缺少“哪些接口需要 bearerAuth”的表达，已通过 OpenAPI 自定义器补齐并复验
- 说明：首次尝试 `mvn -pl "application-starter" -Dtest=OpenApiConfigurationTests test` 因未带 `-am` 导致本地模块依赖未解析；随后改为 reactor 方式验证通过

残留风险：
- 当前通过 starter 层 OpenAPI 自定义器按路径规则补充 `bearerAuth`，未在各控制器上逐个声明 OpenAPI 注解
- 当前未增加自定义 `springdoc` 路径或分组配置，保持默认端点路径
- 当前未在真实运行进程上单独手工探测 `/v3/api-docs` 与 `/swagger-ui/index.html` HTTP 端点，主要依赖 reactor 测试、编译与配置装配验证

知识沉淀 / 是否回写 docs：
- 已回写 `docs/API.md`，说明 Swagger / OpenAPI 入口与当前未补充能力

产物清理与保留说明：
- 当前任务单保留在 `ai-agent-workplace/`，状态已关闭为 `done`

补充说明：
- 本任务为实现类任务，需执行与改动匹配的验证与复审。
