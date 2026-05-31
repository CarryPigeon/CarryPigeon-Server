任务名称：

第 3 轮：公开服务接口与鉴权接口重写

任务目标：

按客户端基准 API 重写公开服务发现、required gate 与鉴权相关 HTTP 接口。

任务背景：

`/api/server`、gate、auth token 流程是客户端连接与登录前置条件，必须优先完成。

影响模块：

- `chat-domain`
- `application-starter`

允许修改范围：

- `server` feature
- `auth` feature
- 对应 DTO、应用服务、配置与测试

禁止修改范围：

- 不在本轮重写用户/频道/消息主业务接口
- 不在本轮重写 WS 协议

依赖限制：

- 遵守现有模块依赖方向

配置限制：

- 不新增未来占位配置

文档依据：

- `docs/t/10-http-ws-protocol-v1.md`
- `docs/t/11-http-endpoints-v1.md`
- `docs/t/13-error-model-and-reasons-v1.md`

任务分解 / 执行计划：

1. 重写 `GET /api/server`。
2. 落地 `POST /api/gates/required/check`。
3. 重写 auth session/token 流程。
4. 补齐成功与失败场景测试。

关键假设与依赖：

- 依赖第 2 轮协议底座完成。

实现要求：

- 严格对齐客户端字段命名与错误 reason。

测试要求：

- 至少覆盖成功、校验失败、required gate 失败、鉴权失败。

质量门禁：

- 相关 controller 测试与应用层测试通过。

复审要求：

- 重点复审对外协议字段与错误 reason。

文档要求：

- 需要在最终收口轮次同步更新对外 API 文档。

验收标准：

- 客户端握手、预检查、登录主链路可对齐。

完成定义：

- 服务发现与鉴权相关 HTTP 协议完成。

实际结果：

- 已将 `/api/server` 从旧的 echo 入口重写为匿名服务发现入口，返回 `server_id / name / brief / avatar / api_version / min_supported_api_version / ws_url / required_plugins / capabilities / server_time`。
- 已新增 `POST /api/gates/required/check`，可基于客户端上报的 `installed_plugins` 返回 `missing_plugins` 列表。
- 已将公开鉴权入口重写为 `POST /api/auth/email_codes`、`POST /api/auth/tokens`、`POST /api/auth/refresh`、`POST /api/auth/revoke`，并保留 `GET /api/auth/me` 作为当前受保护身份查询入口。
- 已在 `AuthController` 中把 required gate 阻断接入到 token 创建前置检查；当缺失必需插件时，返回 `412 Precondition Failed + error.reason=required_plugin_missing`，并带 `details.missing_plugins`。
- 已在 `auth` feature 内新增最小邮箱验证码能力承接件：`EmailVerificationCodeService` 抽象与 `InMemoryEmailVerificationCodeService` 实现，用于支撑 round3 会话创建链路；当前实现只保证同进程内验证码签发与校验闭环，不承接真实邮件投递。
- 已在 `AuthApplicationService` 中新增邮箱验证码会话创建流程，并复用现有账户、refresh session、默认资料和默认频道初始化逻辑，避免把 round3 扩散成 round4 业务改造。
- 已同步更新 `AuthWebMvcConfiguration` 的匿名放行边界与 `OpenApiConfiguration` 的鉴权标注规则，使 `/api/server`、`/api/gates/required/check` 和新的公开 auth 路由不再被误标为受保护接口。
- 已同步修正 `application-starter` 回归测试装配，使新的 `AuthController` 依赖链在 starter 级测试上下文中可正常装配。

验证记录：

- 诊断：尝试使用 `lsp_diagnostics` 校验相关源码与测试目录，但当前 Java LSP 在该仓库初始化超时，未能提供可用结果；因此改以 Maven 编译与契约测试作为本轮主要验证手段。
- 测试命令：`mvn -q -pl chat-domain,application-starter -am -Dtest=ServerControllerTests,AuthControllerTests,AuthWebMvcConfigurationTests,OpenApiConfigurationTests,ServerApplicationServiceTests,GlobalExceptionHandlerTests,MessageAttachmentRegressionTests -Dsurefire.failIfNoSpecifiedTests=false test`
- 结果：通过
- surefire 结果：`chat-domain/target/surefire-reports/team.carrypigeon.backend.chat.domain.features.auth.controller.http.AuthControllerTests.txt`
- surefire 结果：`chat-domain/target/surefire-reports/team.carrypigeon.backend.chat.domain.features.server.controller.http.ServerControllerTests.txt`
- surefire 结果：`chat-domain/target/surefire-reports/team.carrypigeon.backend.chat.domain.features.server.application.service.ServerApplicationServiceTests.txt`
- surefire 结果：`chat-domain/target/surefire-reports/team.carrypigeon.backend.chat.domain.features.auth.config.AuthWebMvcConfigurationTests.txt`
- surefire 结果：`application-starter/target/surefire-reports/team.carrypigeon.backend.starter.config.OpenApiConfigurationTests.txt`
- surefire 结果：`application-starter/target/surefire-reports/team.carrypigeon.backend.starter.MessageAttachmentRegressionTests.txt`

残留风险：

- 若 gate 和 auth 模型未对齐，客户端无法进入主业务链路。
- 当前邮箱验证码服务仍是 round3 最小承接实现，仅保证进程内验证码会话语义，不包含真实邮件发送与多实例共享存储；若后续需要真实邮件登录体验，应在不破坏当前 public contract 的前提下于后续轮次引入正式外部服务实现。
- 当前 `/api/auth/me` 仍保留旧的成功 envelope 语义，作为过渡期受保护身份查询入口；真正与 `docs/t` 用户资源面完全对齐仍需在 round4 继续收口。

知识沉淀 / 是否回写 docs：

- 当前已形成一条稳定实现经验：当公共协议已切到 v1 形态而底层域模型仍偏旧时，可以先通过“public contract 重写 + 应用层复用旧域逻辑”的方式推进，而不是一次性强拆全部内部模型。是否把这条经验沉淀到 `docs/`，建议在 round6 收口时统一评估。

产物清理与保留说明：

- 保留必要分析与接口对照草稿
