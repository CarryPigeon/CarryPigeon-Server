# WS 默认开启与 TLS 状态核查任务单

## 任务目标

- 将当前项目的 WebSocket / realtime 默认状态调整为开启。
- 核查当前 TLS / WSS 是否具备真实加密能力，并向用户说明可用状态与风险边界。

## affected modules

- `application-starter`
- `chat-domain`
- `docs`
- `ai-agent-workplace`

## 允许修改范围

- realtime 默认配置值、环境示例、启动配置文档中与默认开关直接相关的内容。
- 反映默认值变化所需的最小测试调整。
- 本任务单。

## 禁止边界

- 不引入新的 TLS 终止方案、证书管理方案或反向代理部署架构。
- 不新增依赖。
- 不改变 realtime 模块职责或协议模型。
- 不扩大到重构 WebSocket 入站/出站消息处理。

## governing docs

- `AGENTS.md`
- `docs/架构文档.md`
- `docs/API.md`
- `docs/配置规范.md`
- `docs/AI协作开发规范.md`
- `docs/变更审核清单.md`

## acceptance criteria

- 未显式配置时 realtime / WS 默认为开启。
- `.env.example` 与相关文档不再声明旧的默认关闭语义。
- 相关测试通过或明确说明无法验证项。
- TLS / WSS 状态有代码证据支撑，区分“discovery 返回 wss URL”和“服务端真实 TLS 加密监听”。

## 实际结果

- 已将 `application-starter/src/main/resources/application.yaml` 中 `CP_CHAT_REALTIME_ENABLED` 的默认值从 `false` 改为 `true`。
- 已将 `.env.example` 中 `CP_CHAT_REALTIME_ENABLED=false` 改为 `CP_CHAT_REALTIME_ENABLED=true`。
- 已更新 OpenAPI Apifox 导入提示，说明 WS 开关默认开启，设置为 `false` 才关闭。
- 已新增 `ApplicationYamlDefaultsTests` 锁定 application.yaml 中 realtime 默认开启表达式。
- 已更新 `docs/API.md` 与 `docs/架构文档.md`，同步默认开启语义，并说明当前内置 Netty realtime 未装配 TLS handler。

## TLS / WSS 核查结论

- 当前内置 Netty pipeline 只装配了 `HttpServerCodec`、`HttpObjectAggregator`、握手鉴权、`WebSocketServerProtocolHandler` 和业务 handler。
- 未发现 `SslContext`、`SslHandler` 或等价 TLS handler 装配。
- 因此，直接连接内置 realtime 端口时是明文 `ws://`，不是服务端内置 TLS 加密的 `wss://`。
- 当前 `wss://` discovery URL 只能在前置网关、反向代理或负载均衡完成 TLS 终止并转发到内置 realtime 端口时成立。

## 验证记录

- `mvn -pl application-starter,chat-domain -am test -DskipTests=false '-Dtest=ApplicationYamlDefaultsTests,OpenApiConfigurationTests,RealtimeServerPropertiesTests,RealtimeServerConfigurationContextTests,ServerEntranceDomainApiTests,ServerControllerTests' -Dsurefire.failIfNoSpecifiedTests=false`
  - 结果：`BUILD SUCCESS`
  - 关键结果：`chat-domain` realtime/server 相关测试 10 个通过；`application-starter` OpenAPI 与 application.yaml 默认值测试 5 个通过。

## 自检结论

- 模块边界：通过。未改变 realtime 模块职责。
- 依赖：通过。未新增依赖。
- 配置：通过。只调整既有配置默认值。
- 文档：通过。已同步默认开启与 TLS 现状。
- 残留风险：生产环境如需要真实 WSS，仍需单独设计并确认 TLS 终止方案；本次未实现内置 TLS。
