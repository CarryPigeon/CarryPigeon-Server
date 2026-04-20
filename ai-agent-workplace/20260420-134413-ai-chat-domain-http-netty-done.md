任务名称：

chat-domain 第一阶段 HTTP 与 Netty 实时通道基础落地

任务目标：

在不引入具体聊天业务仓储与外部服务依赖的前提下，为 `chat-domain` 建立可扩展的 HTTP 基础接口、统一响应与异常映射，并以 Netty 建立独立实时通道基础骨架，由 `application-starter` 完成启动装配。

任务背景：

当前 `chat-domain` 基本为空模块，尚未形成按 `features` 分层的协议入口、统一响应约定和业务问题异常。项目目标是即时聊天软件后端，需要先建立稳定的 HTTP 接口骨架与实时通信入口骨架，为后续用户、会话、频道、消息等 feature 落地提供统一入口。

影响模块：

- `pom.xml`
- `chat-domain`
- `application-starter`
- `ai-agent-workplace`

允许修改范围：

- 修改 `chat-domain/pom.xml`
- 修改 `application-starter/pom.xml`
- 修改 `application-starter/src/main/resources/application.yaml`
- 新增 `chat-domain` 下的 `shared`、`features/server` 相关源码与测试
- 新增 `application-starter` 下的 Netty 装配配置类与测试
- 在 `ai-agent-workplace/` 记录任务单与自检结论

禁止修改范围：

- 不修改既有模块职责与依赖方向
- 不在 `application-starter` 承载核心业务规则
- 不新增数据库、缓存、对象存储等业务适配
- 不新增 `chat-domain` 对 `*-impl` 的依赖
- 不提前落用户、频道、消息等具体业务 feature
- 不将 HTTP 协议入口切换为纯 Netty

依赖限制：

- 允许在 `chat-domain` 引入 `infrastructure-basic`
- 允许在 `chat-domain` 引入 `netty-all`
- 保留 `spring-boot-starter-web` 与 `spring-boot-starter-validation`
- 移除未使用的 `spring-boot-starter-websocket`
- 不新增额外重量级通信框架

配置限制：

- HTTP 仍由 Spring MVC 承接
- Netty 实时通道配置归 `chat-domain.features.server.config`
- 最终运行配置值统一写入 `application-starter/src/main/resources/application.yaml`
- 自定义配置前缀保持 `cp`
- 仅保留当前真实使用的最小配置项

文档依据：

- `docs/架构文档.md`
- `docs/包结构规范.md`
- `docs/依赖引入规范.md`
- `docs/配置规范.md`
- `docs/异常与错误码规范.md`
- `docs/测试规范.md`
- `docs/注释规范.md`
- `docs/AI协作开发规范.md`
- `docs/变更审核清单.md`
- `docs/任务单模板.md`

实现要求：

- `chat-domain` 按 `features` 优先组织
- 先建立 `shared` 中的统一响应与统一问题异常
- 建立 `features/server` 的 HTTP 基础接口
- 建立 `features/server` 的 Netty WebSocket 通道骨架
- Netty 具体生命周期由 `application-starter` 装配
- WebSocket 基础阶段只提供连接、欢迎消息、心跳与文本消息回显骨架
- 所有关键类与边界方法补齐职责、边界、输入、输出注释

测试要求：

- 补充 HTTP 成功路径测试，覆盖 `CPResponse.code = 100`
- 补充参数错误、权限错误、资源不存在、内部错误测试，覆盖 `200/300/404/500`
- 补充 Netty 配置属性或启动装配的最小测试
- 测试类命名使用 `<Name>Tests`
- 测试方法命名使用 `methodName_condition_expected()`

文档要求：

- AI 过程材料进入 `ai-agent-workplace/`
- 当前任务执行中使用 `current` 状态
- 若未新增长期规则，不修改 `docs/`

验收标准：

- `chat-domain` 建立统一响应、统一异常映射与 `features/server` 基础骨架
- HTTP 基础接口可返回统一 `CPResponse`
- 业务问题异常可稳定映射 `100/200/300/404/500`
- Netty WebSocket 服务可由 `application-starter` 装配并具备最小启动能力
- 关键配置进入 `application.yaml`
- 相关测试通过

补充说明：

- 2026-04-20 用户已明确确认：HTTP 采用 Spring MVC，实时通道采用 Netty
- 2026-04-20 用户已明确批准新增 Netty 相关依赖与必要配置扩展

## 实际结果

- 已在 `chat-domain` 建立统一响应 `CPResponse`、统一问题异常 `ProblemException` 与全局异常映射 `GlobalExceptionHandler`
- 已新增 `features/server` 的 HTTP 基础接口、服务概览 DTO 与 Netty WebSocket 通道骨架
- 已在 `application-starter` 新增 Netty 实时通道装配配置与运行时托管类
- 已新增 `cp.chat.server.realtime.*` 最小运行配置
- 已移除生产环境中的演示型错误端点，失败响应码改由异常处理器测试直接验证
- 已补充 HTTP、异常映射、配置属性与 starter 装配测试

## 实际影响文件

- `chat-domain/pom.xml`
- `chat-domain/src/main/java/team/carrypigeon/backend/chat/domain/shared/**`
- `chat-domain/src/main/java/team/carrypigeon/backend/chat/domain/features/server/**`
- `chat-domain/src/test/java/team/carrypigeon/backend/chat/domain/features/server/**`
- `chat-domain/src/test/java/team/carrypigeon/backend/chat/domain/shared/controller/advice/GlobalExceptionHandlerTests.java`
- `application-starter/pom.xml`
- `application-starter/src/main/java/team/carrypigeon/backend/starter/config/**`
- `application-starter/src/test/java/team/carrypigeon/backend/starter/config/RealtimeServerConfigurationTests.java`
- `application-starter/src/main/resources/application.yaml`
- `ai-agent-workplace/20260420-134413-ai-chat-domain-http-netty-done.md`

## 自检结论

- 已明确任务目标、影响模块、允许范围、禁止边界、依赖限制、配置限制与验收标准
- 模块边界符合当前约束：`chat-domain` 未依赖任何 `*-impl`，`application-starter` 只负责 Netty 装配与生命周期托管
- 新增依赖已在用户批准边界内执行
- 生产代码中不再保留仅为测试响应码而暴露的 demo HTTP 端点
- 测试已覆盖 `CPResponse.code` 的 `100/200/300/404/500`
- 当前未引入新的长期项目规则，因此未修改 `docs/`

## 残留风险与未完成项

- 当前 Netty 实时通道仅提供欢迎消息、心跳和文本回显骨架，尚未接入认证、会话绑定、消息路由与命令分发
- `ProblemException` 当前未携带 `cause`，后续若用于包装更深层异常，建议补充原始异常链
- 当前 `features/server` 仍是协议层基础 feature，后续需要进入 `user/session/channel/message` 等实际聊天域建模
