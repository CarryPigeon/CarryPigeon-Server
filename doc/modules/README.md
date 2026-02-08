# 模块文档导航

> 本文用于说明模块职责与阅读路径，避免在各文档重复描述同一实现细节。

## 1. `api`

- 职责：领域模型、协议对象、DAO/Service 抽象、流程上下文约定
- 参考：`doc/api/文档索引.md`

## 2. `common`

- 职责：通用工具与基础配置（如序列化约定）

## 3. `dao`

- 职责：数据访问默认实现（MyBatis-Plus + Redis）
- 文档：`doc/modules/dao-and-cache.md`

## 4. `chat-domain`

- 职责：HTTP API 控制器、WebSocket 事件处理、LiteFlow 业务节点
- 文档：`doc/modules/chat-domain.md`

## 5. `external-service`

- 职责：外部系统集成（如邮件）

## 6. `application-starter`

- 职责：Spring Boot 启动入口、配置与规则装配
- 文档：`doc/modules/application-starter.md`

## 7. `distribution`

- 职责：打包与分发工件
- 参考：`doc/ops/config-and-deploy.md`

## 8. 关联文档

- 系统总览：`doc/overview.md`
- 架构说明：`doc/architecture.md`
- 插件体系：`doc/plugins/README.md`
