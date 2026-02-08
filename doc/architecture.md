# 系统架构说明

> 本文描述 CarryPigeon Backend 的模块关系与运行时主链路，聚焦当前对外协议（HTTP `/api` + WebSocket `/api/ws`）。

## 1. 模块分层

1. 契约层：`api`
2. 基础设施层：`common`、`dao`、`external-service`
3. 业务编排层：`chat-domain`
4. 启动与部署层：`application-starter`、`distribution`

## 2. HTTP 链路

1. Spring MVC 控制器接收请求（`chat-domain/controller/web/api`）
2. `ApiAccessTokenFilter` 完成访问令牌校验（需要鉴权的端点）
3. `ApiFlowRunner` 执行 LiteFlow chain（`config/api_*.xml`）
4. Result 节点输出响应对象
5. `ApiExceptionHandler` 输出统一错误模型

## 3. WebSocket 链路

1. 客户端连接 `wss://{host}/api/ws`
2. 发送 `auth` 命令完成会话认证
3. 服务端推送标准事件 envelope（含 `event_id/event_type/payload`）
4. 客户端基于 `event_id` 做断线恢复与幂等处理

## 4. 业务编排（LiteFlow）

- chain 按领域拆分在 `application-starter/src/main/resources/config/api_*.xml`
- 节点职责分层：`Checker`（校验）→ `Selector/Builder`（装配）→ `Saver`（持久化）→ `Result`（输出）
- 上下文 key 统一定义在 `ApiFlowKeys` 与 `chat-domain/attribute`

## 5. 安全与一致性

- 认证：HTTP Bearer Token + WS `auth`
- 权限：链路前置权限节点（成员、管理员、禁言等）
- 错误：统一 `{ error: { status, reason, message, details } }`
- 一致性：WS 负责增量事件，HTTP 负责补拉兜底

## 6. 可扩展性

- 侵入性扩展：替换 DAO/Service 默认实现
- 拓展性扩展：新增 LiteFlow 节点并接入 `api_*` chain
- 插件规范：见 `doc/plugins/*`
