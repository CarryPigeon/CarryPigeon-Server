# 模块说明：`chat-domain`

> `chat-domain` 承载业务编排核心：Controller、LiteFlow 节点、会话与通知服务。

## 1. 主要目录

源码根目录：`chat-domain/src/main/java/team/carrypigeon/backend/chat/domain`

- `controller/web/api`：HTTP `/api` 控制器
- `controller/web/api/ws`：WebSocket 接入与命令处理
- `cmp/api`：面向 API 的 LiteFlow 节点
- `cmp/biz`：可复用业务节点（用户、频道、消息、文件）
- `attribute`：上下文 key 常量
- `service`：目录扫描、预览、通知、会话等服务

## 2. HTTP API 处理流程

1. Controller 解析请求并写入 `ApiFlowKeys.REQUEST`
2. 需要鉴权的端点通过 `ApiAccessTokenFilter`
3. `ApiFlowRunner` 执行对应 `api_*` chain
4. Result 节点写入 `ApiFlowKeys.RESPONSE`
5. `ApiExceptionHandler` 统一错误输出

## 3. WebSocket 事件流程

1. 客户端连接 `/api/ws` 并发送 `auth`
2. 服务端校验 token 与版本后建立用户会话
3. 业务变化写入事件流（`message.created` 等）
4. 客户端基于 `event_id` 做 resume/去重

## 4. LiteFlow 规则组织

- 运行规则：`application-starter/src/main/resources/config/api_*.xml`
- 推荐拆分：`api_public.xml`、`api_auth_users.xml`、`api_channels.xml`、`api_messages.xml`、`api_files.xml`
- 统一命名：`api_<resource>_<action>`

## 5. 节点开发规范

- 节点优先复用既有 `cmp/biz` 能力
- 上下文 key 必须来自 `attribute` 或 `ApiFlowKeys`
- 错误统一抛 `CPProblemException`，由全局异常处理器映射
- 对高频路径避免阻塞 IO 与重复查询

## 6. 常见扩展点

- 新增端点：Controller + chain + Result 节点
- 新增业务校验：`Checker` 节点
- 新增返回组装：`Result` 节点
- 新增通知策略：`service/ws` 或通知构建节点
