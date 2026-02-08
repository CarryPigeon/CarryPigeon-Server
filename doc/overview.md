# CarryPigeon Backend 概览

> CarryPigeon Backend 提供面向即时通讯场景的 HTTP API 与 WebSocket 事件流，覆盖用户、频道、消息、文件与插件目录发现能力。

## 1. 核心能力

- 用户体系：邮箱验证码登录、token 刷新与吊销、用户资料维护
- 频道体系：频道创建、成员管理、管理员与禁言治理
- 消息体系：发送、分页拉取、删除、读状态同步、未读统计
- 文件体系：上传/下载授权与文件元信息管理
- 插件能力：插件目录发现、Domain 目录发现、Contract 约束暴露

## 2. 对外协议基线

- HTTP Base：`/api`
- WebSocket：`/api/ws`
- JSON：`snake_case`
- 实体 ID：十进制字符串
- 错误模型：`error.reason` 为客户端主分支字段

## 3. 代码模块

- `api`：稳定契约层
- `chat-domain`：业务编排层
- `dao`：持久化实现层
- `application-starter`：应用启动与配置
- `external-service`：第三方集成
- `distribution`：打包与分发

## 4. 关键链路

- HTTP 请求：Controller → Filter → LiteFlow chain → Result
- WebSocket：`auth` → 事件推送 → `resume` 恢复
- 数据访问：业务节点经 DAO 抽象访问数据库与缓存

## 5. 推荐阅读

- API 索引：`doc/api/文档索引.md`
- 架构说明：`doc/architecture.md`
- 模块导航：`doc/modules/README.md`
- 运维安全：`doc/ops/*`
