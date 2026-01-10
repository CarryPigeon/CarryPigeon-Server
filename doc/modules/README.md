# 模块文档导航

> 本文作为模块级文档的入口，简要说明各模块职责，并指向更详细的说明文档。

---

## 1. API 模块（`api`）

- 路径：`api/`
- 职责：
  - 定义所有领域模型（`CPUser`, `CPChannel`, `CPMessage`, `CPChannelReadState` 等）；
  - 定义 DAO 接口、协议对象（`CPPacket`, `CPResponse`）、通知模型等；
  - 提供 LiteFlow 节点基类与 Controller 接口。
- 文档：
  - 参考：`doc/api.md`、`doc/api/api-chat-message.md`

---

## 2. 公共模块（`common`）

- 路径：`common/`
- 职责：
  - 提供通用工具类（时间处理、通用配置等）；
  - 放置不依赖业务上下文的可复用代码。
- 文档：
  - 暂无单独文档，如有需要可扩展。

---

## 3. DAO 模块（`dao`）

- 路径：`dao/`
- 职责：
  - 实现 `api` 模块中定义的 DAO 接口；
  - 基于 MyBatis-Plus 与数据库交互；
  - 使用 Redis + Spring Cache 缓存热点数据。
- 文档：
  - `doc/modules/dao-and-cache.md`

---

## 4. 业务域模块（`chat-domain`）

- 路径：`chat-domain/`
- 职责：
  - 聊天业务核心逻辑所在模块；
  - 包含：
    - Netty 控制器（`controller/netty`）；
    - LiteFlow 业务节点（`cmp`）；
    - 上下文常量池（`attribute`）；
    - 通知与会话服务（`service`）；
  - 将 DAO 与连接层组合成完整的业务能力。
- 文档：
  - `doc/modules/chat-domain.md`
  - 相关：`doc/features/feature-message-read-state.md`

---

## 5. 连接模块（`connection`）

- 路径：`connection/`
- 职责：
  - 提供基于 Netty 的 TCP 服务；
  - 实现应用层帧协议、AES/ECC 握手与 AAD 校验；
  - 将解密后的业务 JSON 委托给 `chat-domain` 处理。
- 文档：
  - `doc/modules/connection.md`
  - 相关：`doc/architecture.md` 中的运行时架构说明

---

## 6. 外部服务模块（`external-service`）

- 路径：`external-service/`
- 职责：
  - 与外部系统集成，例如邮件发送；
  - 对上层提供统一接口，对下封装第三方 SDK。
- 文档：
  - 暂无单独文档，如有复杂外部集成可在此基础上补充。

---

## 7. 应用启动模块（`application-starter`）

- 路径：`application-starter/`
- 职责：
  - Spring Boot 应用入口；
  - 汇总配置（`application.yaml`）；
  - 定义 LiteFlow 规则文件（`src/main/resources/config/*.xml`）；
  - 绑定端口、连接数据库和 Redis 等基础设施。
- 文档：
  - `doc/modules/application-starter.md`
  - 相关：`doc/overview.md`、`doc/architecture.md`

---

## 8. 分发模块（`distribution`）

- 路径：`distribution/`
- 职责：
  - 打包、部署相关的构建配置和脚本；
  - 可以承载 Docker 镜像构建、CI/CD 配置等。
- 文档：
  - 建议与运维文档配合：`doc/ops/config-and-deploy.md`（待补充）

---

## 9. 特性与插件文档

本目录不重复特性与插件细节，只做索引：

- 特性类：
  - `doc/features/feature-message-read-state.md`
- 插件类：
  - `doc/plugins/README.md`
  - `doc/plugins/plugin-architecture.md`
  - `doc/plugins/plugin-dev-guide.md`

后续如新增更多模块文档，可在此文件中继续补充导航条目。 
