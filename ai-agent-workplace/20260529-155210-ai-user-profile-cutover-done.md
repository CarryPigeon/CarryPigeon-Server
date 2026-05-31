任务名称：
User Profile 协议切换实现

任务目标：
将 `users` family 的对外 HTTP 协议切换到 `docs/t` 基准，删除残留 `CPResponse` 成功包装与不再保留的旧分页/搜索/更新端点。

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
- `chat-domain` 中与 `users` family 直接相关的控制器、DTO、应用服务与测试
- 如有必要，更新 `application-starter` 中与该 family 文档装配直接相关的最小内容
- `ai-agent-workplace/` 任务材料

禁止修改范围：
- 不改 `channels` / `messages` / `ws` family 的正式协议实现
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
- `docs/t/10-http-ws-protocol-v1.md`
- `docs/t/11-http-endpoints-v1.md`
- `docs/t/13-error-model-and-reasons-v1.md`

任务分解 / 执行计划：
1. 对照 `docs/t` 收口 `UserProfileController` 的保留端点与返回模型。
2. 删除旧成功包装、旧分页/搜索端点与旧 `PUT /api/users/me`。
3. 清理受影响 DTO / 测试 / 装配残留。
4. 运行 user family 定向测试并修复。

关键假设与依赖：
- `GET /api/users/me`
- `GET /api/users/{uid}`
- `GET /api/users?ids=...`
- `PUT /api/users/me/email`
- `PATCH /api/users/me`
- `POST /api/users/me/background`
  为本轮保留端点。
- `GET /api/users/page`
- `GET /api/users/search`
- 旧 `PUT /api/users/me`
  不再保留。

实现要求：
- 对外成功响应必须直接返回资源对象、列表对象或 `204`。
- 不保留双轨控制器行为。
- 删除的旧 DTO / 旧测试不应继续残留为可编译死代码。

测试要求：
- 更新 `UserProfileControllerTests`
- 如有需要，补充或更新相关 starter / OpenAPI 测试
- 至少执行受影响 family 的契约测试

质量门禁：
- `users` family 不再存在成功响应 `CPResponse`
- 不再存在本 family 的旧分页/搜索/更新端点实现
- 相关契约测试通过

复审要求：
- 完成后对照 `docs/t` 自检路径、状态码、字段与错误 reason

文档要求：
- 本轮不修改长期 `docs/`

验收标准：
- `users` family 成为新协议基线的一部分

完成定义：
- 代码改动完成
- 目标测试通过
- 任务单记录结果、验证和残留风险

实际结果：
- `UserProfileController` 已移除旧 `GET /api/users/page`、`GET /api/users/search` 与旧 `PUT /api/users/me`。
- `users` family 成功响应已不再使用 `CPResponse` 成功包装。
- `GET /api/users` 已收口为 `docs/t` 基准下的 `ids` 批量查询语义；缺失 `ids` 时返回校验错误。
- 控制器级 `@Hidden` 已移除，使 OpenAPI 暴露的用户接口与真实对外协议一致。
- 仅服务旧更新端点的 `UpdateCurrentUserProfileRequest` 与 `UserProfileResponse` DTO 已删除。

验证记录：
- 残留引用检索：
  - 已检索 `UpdateCurrentUserProfileRequest`、`UserProfileResponse`、`/api/users/page`、`/api/users/search`、旧 `PUT /api/users/me` 与 `users` family 下的 `CPResponse`。
  - 当前 `users` family 未再命中上述旧入口和旧 DTO 残留。
- 已执行定向测试：
  - `mvn -q -pl chat-domain -am -Dtest=UserProfileControllerTests,UserProfileApplicationServiceTests,AuthWebMvcConfigurationTests -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false test`
- 关键结果：
  - `UserProfileControllerTests`: 8 passed
  - `UserProfileApplicationServiceTests`: 13 passed
  - `AuthWebMvcConfigurationTests`: 1 passed

残留风险：
- `UserProfileApplicationService` 内部仍保留分页/搜索相关用例与 DTO，但当前已不再对外暴露；后续若继续收敛内部边界，可在统一清理阶段再处理。
- `GET /api/users/{uid}` 当前仍沿用现有服务层公开资料语义，若后续客户端要求更严格的跨用户可见性规则，需要在业务层补齐，而不是恢复旧包装。

知识沉淀 / 是否回写 docs：
- 暂不回写 `docs/`

产物清理与保留说明：
- 保留本任务单作为 `users` family 切换记录。
