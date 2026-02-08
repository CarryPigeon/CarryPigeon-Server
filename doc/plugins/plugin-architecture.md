# 插件架构说明

> 插件体系由“稳定接口 + 可替换实现 + 可插拔编排”三部分组成。

## 1. 架构分层

- 稳定边界：`api` 模块（领域模型、DAO/Service 抽象）
- 宿主实现：`dao`、`chat-domain`、`external-service`
- 装配层：Spring Bean + LiteFlow chain

## 2. 扩展路径

### 2.1 拓展性扩展（默认推荐）

- 新增 `@LiteflowComponent` 节点
- 插入 `application-starter/src/main/resources/config/api_*.xml`
- 适合审计、风控、消息加工、通知增强

### 2.2 侵入性扩展

- 实现并替换 `api` 抽象接口
- 用 `@Primary` 或条件装配控制启用
- 适合存储替换、外部服务替换

## 3. 设计原则

- 单一职责：每个插件只解决一个能力域问题
- 显式开关：可按环境/租户开关插件
- 失败可控：插件异常可降级，不阻断核心业务
- 兼容优先：遵守 API 错误模型与字段语义

## 4. 与对外协议的关系

- 插件可扩展服务端能力，但不可破坏 `doc/api/*` 既定语义
- 对消息 domain 的扩展必须提供可校验的 contract 约束
