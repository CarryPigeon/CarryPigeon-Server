任务名称：
MDC 接线与跨模块 get/set 审查

任务目标：
为当前 HTTP / WebSocket 运行链路补齐生产可用的 MDC 日志上下文写入与清理，并审查除已处理的 `database-impl` 之外其它模块是否仍存在可疑的手写 getter/setter 样板代码。

任务背景：
上一轮审查已确认 Log4j2 主配置存在，但 `LogContexts`/`LogKeys` 仅定义与测试了 MDC 操作，尚未接入生产请求链路。用户同时要求继续审查其它模块是否也存在“滥用手写 get/set 而不是 Lombok”的情况。

影响模块：
- `chat-domain`
- `infrastructure-basic`
- `docs`（如仅需在最终结论中说明则不修改）
- `ai-agent-workplace`

允许修改范围：
- 在 `chat-domain` 内新增或调整最小范围的 HTTP / WebSocket MDC 接线代码与测试
- 在 `ai-agent-workplace/` 记录本轮续做任务单
- 输出对其它模块手写 getter/setter 的审查结论

禁止修改范围：
- 不改模块职责与依赖方向
- 不为 accessor 审查盲目新增 Lombok 依赖
- 不把日志接线扩展成新的观测架构
- 不顺手重构无关业务逻辑

依赖限制：
- 仅使用现有 Spring Boot、Netty、Log4j2、Lombok 基线能力
- 若其它模块需要引入 Lombok，仅审查记录，不在本任务内默认落地

配置限制：
- 不新增未来占位配置
- 沿用现有 `cp` 配置体系和现有日志字段命名

文档依据：
- `AGENTS.md`
- `docs/AI协作开发规范.md`
- `docs/变更审核清单.md`
- `docs/任务单模板.md`
- `docs/基建文档.md`

任务分解 / 执行计划：
1. 在已知 HTTP / WebSocket 入口上定位最小可行 MDC 接线点。
2. 为 HTTP 请求补齐 `trace_id/request_id/route`，并在已认证请求中补齐 `uid`。
3. 为 WebSocket 握手与消息处理补齐会话级 MDC 上下文装配与清理。
4. 补充/更新针对性测试，确保 MDC 写入与清理行为可验证。
5. 汇总其它模块剩余手写 getter/setter 情况，区分安全候选与不应替换对象。
6. 运行诊断与针对性测试；必要时做一次 Oracle 复审。

关键假设与依赖：
- HTTP 请求线程与当前 Spring MVC 处理链共享 MDC，可通过 Filter + Interceptor 完成最小接线。
- WebSocket 使用 Netty 事件循环线程，需要在处理器内部显式写入与清理 MDC，避免线程复用污染。
- 其它模块的 accessor 审查以主源码为主，不把测试桩/匿名类误判为正式样板代码。

实现要求：
- MDC 接线必须显式清理，避免线程本地变量泄漏
- 不修改现有日志字段名
- 其它模块 accessor 审查必须基于实际文件，不凭印象下结论

测试要求：
- 至少覆盖 HTTP MDC 写入与清理
- 至少覆盖 WebSocket 握手或生命周期中的 MDC 相关行为

质量门禁：
- 改动文件编译通过
- 相关模块测试通过
- 若无法获得 LSP 结果，以实际 Maven 编译/测试作为替代验证

复审要求：
- 完成后进行 Oracle 复审，并在最终答复前收集结果

文档要求：
- 默认不新增长期规则文档；若审查结论形成稳定规则，再考虑回写 `docs/`

验收标准：
- 生产 HTTP / WebSocket 路径中存在实际 MDC 接线与清理
- 给出除 `database-impl` 外其它模块 accessor 使用情况的明确审查结论

完成定义：
- 代码、测试、验证、审查结论全部完成并记录

实际结果：
- 已在 `chat-domain` 新增 `shared/controller/support/HttpRequestMdcFilter`，为 HTTP 请求写入 `trace_id`、`request_id`、`route` 并在请求结束后统一清理。
- 已在 `AuthAccessTokenInterceptor` 中为已认证请求写入 `uid`，并在 `afterCompletion` 中移除该字段。
- 已在 `RealtimeAccessTokenHandshakeHandler` 与 `RealtimeChannelHandler` 中接入 WebSocket 级 MDC 上下文，覆盖握手、欢迎消息、心跳、逐帧处理、异常与断开清理。
- 已根据 Oracle 复审意见进一步收紧实现：`route` 仅记录路径、不再带 query string；`uid` 清理改为复用 `LogKeys.UID`；未完成握手前不会发送 heartbeat；`RealtimeChannelSession` 注释已与当前职责对齐。
- 已完成除 `database-impl` 外其它模块的 accessor 审查：真正仍带明显手写 getter/setter 样板的生产类主要是 `MessagePluginGovernanceProperties`；其余仅剩少量异常/门面类的低收益访问器，不构成明显“滥用 get/set”。

验证记录：
- `mvn -pl chat-domain -am compile -DskipTests=true`：成功。
- `mvn -pl chat-domain test -DskipTests=false -Dtest=AuthAccessTokenInterceptorTests,RealtimeAccessTokenHandshakeHandlerTests,RealtimeChannelHandlerLifecycleTests,HttpRequestMdcFilterTests`：成功，10 个测试全部通过。
- `mvn -pl chat-domain test -DskipTests=false -Dtest=RealtimeChannelHandlerMessageDispatchTests`：成功，7 个测试全部通过。
- `mvn -pl chat-domain test -DskipTests=false -Dtest=AuthAccessTokenInterceptorTests,RealtimeAccessTokenHandshakeHandlerTests,RealtimeChannelHandlerLifecycleTests,HttpRequestMdcFilterTests,RealtimeChannelHandlerMessageDispatchTests`：成功，18 个测试全部通过。
- `mvn -pl infrastructure-basic test -DskipTests=false -Dtest=LogContextsTests`：成功，4 个测试全部通过。
- LSP：Java LSP 初始化超时；本轮继续以实际 Maven 编译与测试结果作为替代验证依据。
- Oracle 复审：完成。结论为无阻塞问题，仅指出 `route` 粒度、共享常量、未握手心跳与注释一致性等非阻塞细节，均已修复。

残留风险：
- HTTP `trace_id` / `request_id` 当前默认来自请求头或本地生成 UUID，已足以满足最小上下文接线，但未与更高层统一链路追踪体系对齐。
- WebSocket MDC 当前基于握手期保存在 channel attribute 中的上下文值；若未来扩展更多通道级字段，需要继续沿用显式清理策略。
- `MessagePluginGovernanceProperties` 仍是一个真实存在的手写 accessor 类，但它同时承担 `@ConfigurationProperties` 绑定与现有 fluent reader API，不适合在本轮直接机械替换为 Lombok。

知识沉淀 / 是否回写 docs：
- 本轮尚未形成新的长期项目规则，暂不回写 `docs/`
- accessor 审查结论以最终输出说明，不单独生成长期文档

产物清理与保留说明：
- 任务单已改名为 `done`
- 暂留在 `ai-agent-workplace/` 根目录，便于当前阶段追溯；后续可按仓库规则移入 `archive/`

补充说明：
- 本轮在上一轮任务基础上续做，不重复改动已完成的 `database-impl` 实体。
