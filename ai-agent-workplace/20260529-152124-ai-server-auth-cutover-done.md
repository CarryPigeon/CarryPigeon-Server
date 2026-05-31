任务名称：
Server/Auth 协议切换实现

任务目标：
将 `server` / `auth` family 的对外 HTTP 协议切换到 `docs/t` 基准，删除残留的 `CPResponse` 成功包装和不再保留的旧端点，为后续业务 family 重写提供稳定入口基线。

任务背景：
用户已明确确认：
- 成功响应彻底移除 `CPResponse`
- 对外 API 以 `docs/t` 为唯一基准，不保留旧端点
- WebSocket 继续保留当前独立 Netty 模型

任务类型：
实现类任务

影响模块：
- `chat-domain`
- `application-starter`
- `ai-agent-workplace/`

允许修改范围：
- `chat-domain` 中与 `server` / `auth` family 直接相关的控制器、DTO、应用服务与测试
- 如有必要，更新 `application-starter` 中与该 family 文档装配直接相关的最小内容
- `ai-agent-workplace/` 任务材料

禁止修改范围：
- 不改 `channel` / `user` / `message` / `ws` family 的正式协议实现
- 不调整模块依赖方向
- 不新增第三方依赖
- 不修改长期 `docs/`

依赖限制：
- 仅使用现有依赖

配置限制：
- 不扩大全局配置体系

文档依据：
- `docs/AI协作开发规范.md`
- `docs/变更审核清单.md`
- `docs/架构文档.md`
- `docs/API.md`
- `docs/t/SERVER_API.md`
- `docs/t/10-http-ws-protocol-v1.md`
- `docs/t/11-http-endpoints-v1.md`
- `docs/t/13-error-model-and-reasons-v1.md`

任务分解 / 执行计划：
1. 清理 `AuthController` 中不再属于目标协议的旧成功包装与旧端点。
2. 清理 `ServerController` / `ServerWellKnownController` 中不再属于目标协议的旧成功包装与旧端点。
3. 收拢 `ServerApplicationService` 的公开发现叙事，使其与当前协议一致。
4. 更新受影响契约测试，删除旧端点断言，补新协议断言。
5. 运行 Server/Auth 相关测试并根据结果修复。

关键假设与依赖：
- `GET /api/server`、gate、plugin catalog、domain catalog、`POST /api/auth/*` 为保留主入口。
- `GET /api/auth/me`、`GET /api/server/presence/me` 以及任何残留 `CPResponse` 成功出口不再保留。
- `/.well-known/carrypigeon-server` 若 `docs/t` 无要求，则本轮一并删除。

实现要求：
- 对外成功响应必须直接返回资源对象或 `204`。
- 不保留双轨控制器行为。
- 删除的旧 DTO / 旧控制器 / 旧测试不应继续残留为可编译死代码。

测试要求：
- 更新 `AuthControllerTests`
- 更新 `ServerControllerTests`
- 若涉及公共应用服务语义变化，更新 `ServerApplicationServiceTests`
- 至少执行受影响 family 的契约测试

质量门禁：
- 相关控制器契约测试通过
- 不再存在本 family 对外成功响应的 `CPResponse`
- 不再存在本 family 的旧端点实现

复审要求：
- 完成后对照 `docs/t` 自检路径、状态码、字段与错误 reason

文档要求：
- 本轮不修改长期 `docs/`

验收标准：
- Server/Auth family 成为后续客户端连接链路的唯一新基线

完成定义：
- 代码改动完成
- 目标测试通过
- 任务单记录结果、验证和残留风险

实际结果：
- `AuthController` 已移除 `GET /api/auth/me`，并完全切换为 `docs/t` 基准下的邮箱验证码、token 签发、refresh、revoke 入口。
- `ServerController` 已移除 `GET /api/server/presence/me`；`ServerWellKnownController` 与 `/.well-known/carrypigeon-server` 已删除。
- `ServerApplicationService` 已删除 well-known / presence 相关 DTO 与服务方法，仅保留服务发现与 required gate 能力。
- 旧的 `CPResponse` 成功包装 DTO 与本 family 不再使用的旧 DTO 已清理。
- 受影响的 `application-starter` 装配测试与 OpenAPI 测试已同步到新路径集。

验证记录：
- 残留引用检索：
  - 已检索 `/api/auth/me`、`/api/server/presence/me`、`/.well-known/carrypigeon-server` 及对应旧 DTO / 控制器类名。
  - 当前仅剩 `RevokeRefreshTokenRequest` 这类合法新名称命中，不再存在目标旧端点或旧类残留。
- 已执行定向测试：
  - `mvn -q -pl chat-domain -am -Dtest=AuthControllerTests,ServerControllerTests,ServerApplicationServiceTests,ServerPluginCatalogControllerTests,HttpRequestMdcFilterTests -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false test`
  - `mvn -q -pl application-starter -am -Dtest=OpenApiConfigurationTests,MessageAttachmentRegressionTests -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false test`
- 关键结果：
  - `AuthControllerTests`: 11 passed
  - `ServerControllerTests`: 3 passed
  - `ServerApplicationServiceTests`: 2 passed
  - `ServerPluginCatalogControllerTests`: 1 passed
  - `HttpRequestMdcFilterTests`: 2 passed
  - `OpenApiConfigurationTests`: 2 passed
  - `MessageAttachmentRegressionTests`: 4 passed

残留风险：
- 当前仅完成 `server` / `auth` family 切换，其它 family 仍存在旧协议与新协议并存的重写过程风险，需要按 `docs/t` 继续逐批收口。
- `POST /api/auth/revoke` 当前保留为隐藏接口；若客户端基准协议后续要求公开文档或字段收紧，需要在下一批统一校正。

知识沉淀 / 是否回写 docs：
- 暂不回写 `docs/`，待更多 family 收口后统一处理。

产物清理与保留说明：
- 保留本任务单作为第一批实现记录。

补充说明：
- 本轮只做 Server/Auth family，不混入 WS 协议切换。
