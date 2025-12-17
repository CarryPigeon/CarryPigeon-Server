# CarryPigeon Backend 概览

> CarryPigeon 是一个面向即时通讯场景的后端服务，提供用户、频道、消息、文件等核心能力，  
> 并支持多端同步、长连接推送和可插拔扩展。

---

## 1. 功能范围

当前服务端主要提供：

- **用户体系**
  - 注册 / 登录（邮箱 + 验证码、token 登录）
  - 用户基础资料维护（昵称、头像、简介等）
- **频道与成员管理**
  - 频道创建 / 删除 / 基本信息维护
  - 频道成员管理（加入、退出、管理员、禁言）
  - 频道申请 / 审批流程
- **消息系统**
  - 频道消息的发送、删除、列表拉取
  - 未读消息数量查询
  - 读状态同步（多端之间同步“已读到哪”）
- **文件服务**
  - 依托 MinIO 的文件上传 / 下载 token 申请
  - 文件元信息管理
- **服务集成**
  - 邮件发送服务（验证码、通知等）
  - 统一通知通道（基于长连接的服务端推送）

---

## 2. 技术栈与关键组件

- 语言与框架
  - Java 21
  - Spring Boot 3.x
  - MyBatis-Plus（数据库访问）
  - Redis（缓存与部分业务存储）
  - Netty（长连接、TCP 服务）
  - LiteFlow（业务编排与责任链）

- 安全与协议
  - 自定义 TCP 帧协议（2 字节长度前缀）
  - ECC + AES-GCM 握手与加密
  - AAD（附加认证数据）用于防重放、防错包和时间窗口校验
  - 统一 JSON 协议：`CPPacket`（请求） + `CPResponse`（响应）

- 可扩展性
  - DAO 接口与默认实现分离，支持替换底层存储（侵入性插件）
  - 基于 LiteFlow 的节点扩展与链路配置，支持业务级插件（拓展性插件）
  - 插件开发文档与 API 参考（`doc/plugins/*`）

---

## 3. 模块划分

项目采用 Maven 多模块结构，主要模块：

- `api`  
  定义领域模型（BO）、DAO 接口、连接协议对象、控制器与节点基类等，是“公共契约”模块。

- `common`  
  公共工具与通用组件，例如时间工具类、通用配置等。

- `dao`  
  数据访问实现模块：
  - 基于 MyBatis-Plus 的 DAO 默认实现；
  - 数据库映射 PO / Mapper；
  - Redis 缓存配置与 CacheManager。

- `chat-domain`  
  核心业务模块：
  - Netty 控制器（`@CPControllerTag`）；
  - LiteFlow 节点（`cmp` 包）及链路；
  - 业务服务（通知、会话中心、消息服务等）。

- `connection`  
  底层连接与协议模块：
  - Netty 服务器启动与 Pipeline 配置；
  - 编解码器、心跳处理；
  - AES/ECC 安全握手与 AAD 协议。

- `external-service`  
  外部服务集成：
  - 邮件发送（例如 SMTP）；
  - 未来可扩展更多第三方服务。

- `application-starter`  
  应用入口：
  - Spring Boot 启动类；
  - 统一配置（`application.yaml`）；
  - LiteFlow 规则文件（`config/*.xml`）。

- `distribution`  
  打包与部署相关工件（如 Docker 镜像、脚本等）。

---

## 4. 关键数据流（高层）

从客户端到业务处理的大致流程：

1. **连接与握手**
   - 客户端与 Netty 服务器建立 TCP 连接；
   - 通过“客户端本地生成 AES 密钥 + 使用服务器 ECC 公钥加密上传”的方式建立会话级对称密钥，服务器用 ECC 私钥解密并发回一条 `route="handshake"` 的加密通知确认；
   - 后续所有业务包均使用 AES-GCM 加密，并携带 AAD（包序号 + sessionId + 时间戳）。

2. **请求转发与协议解析**
   - 解密后得到 JSON 文本（`CPPacket`）；
   - `CPControllerDispatcherImpl` 根据 `route` 找到对应 Controller（`@CPControllerTag`）；
   - 根据 Controller 的 `voClazz` 解析请求体，填充 LiteFlow 上下文。

3. **LiteFlow 业务链执行**
   - 以 `route` 为链名执行对应 LiteFlow 链；
   - 节点（继承 `CPNodeComponent`）通过 DAO 接口访问数据库、调用业务服务；
   - 最终在上下文中写入 `CPResponse` 对象。

4. **响应与通知**
   - 将 `CPResponse` 序列化为 JSON，并加密后通过 Netty 写回客户端；
   - 对于需要广播的场景（如消息发送、读状态更新），使用 `CPNotificationService` + `CPSessionCenterService`
     向所有相关用户的会话推送 `CPNotification`。

---

## 5. 文档导航

如果你是：

- **业务 / 产品 / 客户端开发者**，推荐从：
  - `doc/api.md`
  - `doc/api/api-chat-message.md`
  - `doc/features/feature-message-read-state.md`

- **后端开发者**，推荐阅读：
  - `doc/architecture.md`
  - `doc/modules/chat-domain.md`（待补充）
  - `doc/modules/dao-and-cache.md`
  - `doc/modules/connection.md`（待补充）

- **插件开发者**，推荐阅读：
  - `doc/plugins/README.md`
  - `doc/plugins/plugin-architecture.md`
  - `doc/plugins/plugin-dev-guide.md`

后续文档会在此基础上逐步完善其他模块与运维、安全相关的细节说明。 
