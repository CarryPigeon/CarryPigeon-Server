任务名称：OpenAPI 与 Apifox 导入体验规范化

任务目标：规范化当前 OpenAPI 输出，使 Apifox 导入后能自动识别本地服务地址、Bearer JWT 鉴权和关键接口请求示例，减少手动整理请求 Body 的工作。

任务背景：用户希望通过 Swagger/OpenAPI 规范化配置实现 Apifox 一键导入，自动获得可直接调试的 HTTP 接口示例。

影响模块：`application-starter`

允许修改范围：仅允许修改 `application-starter` 的 OpenAPI 文档配置与对应测试。

禁止修改范围：不修改业务 Controller 行为，不修改鉴权拦截规则，不新增依赖，不修改模块依赖方向，不修改数据库、Docker 或运行配置语义。

依赖限制：使用现有 springdoc / swagger OpenAPI 依赖，不新增第三方依赖。

配置限制：不新增运行时配置项；OpenAPI server 示例使用当前本地默认地址。

文档依据：`AGENTS.md`、`docs/架构文档.md`、`docs/配置规范.md`、`docs/注释规范.md`、`docs/测试规范.md`、`docs/变更审核清单.md`。

任务分解 / 执行计划：
1. 阅读现有 `OpenApiConfiguration` 和测试，确认已有 Bearer 鉴权与错误响应逻辑。
2. 在 OpenAPI Bean 中补充本地 HTTP server、WebSocket 扩展说明和 Apifox 导入说明。
3. 在 OpenAPI customizer 中集中补充关键请求体示例。
4. 扩展 `OpenApiConfigurationTests` 验证 server、鉴权与请求示例。
5. 运行相关测试并记录结果。

关键假设与依赖：OpenAPI 只能描述 HTTP 接口；WebSocket 推送测试仍需要 Apifox 单独创建 WS 请求或未来补 AsyncAPI。

实现要求：示例集中在 starter 文档装配层，不把测试工具语义泄漏到业务 Controller 逻辑。

测试要求：至少运行 `OpenApiConfigurationTests`。

质量门禁：相关测试通过，OpenAPI customizer 对已有匿名/受保护接口认证语义不产生回归。

复审要求：检查示例字段命名必须符合项目全局 `snake_case` JSON 约定。

文档要求：本次不引入长期规则，暂不修改 `docs/`。

验收标准：Apifox 导入 `/v3/api-docs` 后能看到本地 server、Bearer 鉴权和登录/发消息等关键请求体示例。

完成定义：代码与测试修改完成、测试通过、任务单改名为 `done`。

实际结果：待填写。

验证记录：待填写。

残留风险：待填写。

知识沉淀 / 是否回写 docs：待填写。

产物清理与保留说明：保留任务单作为协作追踪材料。

补充说明：无。

## 执行结果补充

实际结果：已在 `application-starter` 的 `OpenApiConfiguration` 中补充本地 HTTP server、Apifox 导入扩展信息、关键写接口 JSON 请求体示例，并保持原有 Bearer JWT 鉴权与通用错误响应补充逻辑。示例覆盖登录、注册、邮箱验证码、token 创建/刷新/撤销、gate 检查、频道创建/更新、频道通知偏好、文本消息发送/编辑/转发、已读状态、服务通知偏好等常用集成测试入口。

验证记录：
- 首次执行 `mvn -pl application-starter -am test -DskipTests=false -Dtest=OpenApiConfigurationTests -Dsurefire.failIfNoSpecifiedTests=false` 发现 `OpenAPI.addExtension` 链式调用编译失败，已修复为局部变量方式。
- 第二次执行发现测试泛型断言编译失败，已修复为明确 `Map<String, Object>` 转型。
- 最终执行 `mvn -pl application-starter -am test -DskipTests=false -Dtest=OpenApiConfigurationTests -Dsurefire.failIfNoSpecifiedTests=false`：通过，`OpenApiConfigurationTests` 3 个测试全部成功。

残留风险：OpenAPI 仍只能规范 HTTP 接口；WebSocket 推送测试需要 Apifox 单独创建 WS 请求，或未来引入 AsyncAPI/端到端测试补充。

知识沉淀 / 是否回写 docs：不引入长期规则，暂不回写 `docs/`。

产物清理与保留说明：任务单保留在 `ai-agent-workplace/`，并关闭为 `done`。
