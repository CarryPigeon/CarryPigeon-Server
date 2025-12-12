# 系统架构说明

> 本文从架构视角描述 CarryPigeon Backend 的模块关系与运行时结构，  
> 帮助理解“一条请求从哪里来、去到哪里、经过了什么”。

---

## 1. 模块架构（静态视图）

### 1.1 Maven 模块依赖关系（简要）

核心模块及主要依赖：

- `api`
  - 无业务依赖，作为“公共契约”模块被其他所有模块依赖。

- `common`
  - 依赖 `api`（部分工具类会引用领域模型）。

- `dao`
  - 依赖 `api`（实现 API 中定义的 DAO 接口）；
  - 使用 MyBatis-Plus 与数据库交互；
  - 使用 Redis 作为缓存实现。

- `chat-domain`
  - 依赖 `api`（控制器、LiteFlow 节点基类、领域模型）；
  - 依赖 `dao`（通过 DAO 接口访问数据）；
  - 依赖 `common`（工具类等）。

- `connection`
  - 依赖 `api`（协议对象、会话描述）；
  - 依赖 `chat-domain`（接入 Controller 分发器）。

- `external-service`
  - 依赖 `api`（服务接口与模型）；
  - 为邮件等外部服务提供实现。

- `application-starter`
  - 依赖上述所有模块；
  - 提供 Spring Boot 入口与配置聚合。

### 1.2 层级关系（逻辑）

可按以下层次理解：

1. **协议与契约层**：`api`
2. **基础设施层**：`common`, `dao`, `external-service`
3. **业务领域层**：`chat-domain`
4. **连接与接入层**：`connection`
5. **应用启动与部署层**：`application-starter`, `distribution`

---

## 2. 运行时架构（动态视图）

### 2.1 请求处理路径（Netty -> LiteFlow）

一条典型的业务请求（以频道消息为例）的处理路径如下：

1. **TCP 接入（connection 模块）**
   - Netty 接收客户端 TCP 连接；
   - 使用 `NettyDecoder` 根据帧格式（2 字节长度前缀）拆包；
   - 在安全握手完成后，使用 AES-GCM 解密 payload，获得 JSON 文本。

2. **协议解析与分发（chat-domain 控制层）**
   - 将 JSON 文本反序列化为 `CPPacket { id, route, data }`；
   - 交给 `CPControllerDispatcherImpl`：
     - 根据 `route` 查找 `@CPControllerTag(path = ...)` 标注的 Controller；
     - 构造对应 VO（`voClazz`），调用其 `insertData` 将请求参数写入 LiteFlow 上下文；
     - 以 `path` 为链名，调用 LiteFlow 执行链路。

3. **业务编排（LiteFlow + cmp 节点）**
   - LiteFlow 根据 XML 中的定义（`config/*.xml`）执行节点序列：
     - 校验节点（登录、权限、参数）；
     - 业务节点（查询/构建/更新实体）；
     - 通知节点（收集 uid、构建通知数据、发送通知）。
   - 节点基类 `CPNodeComponent` 帮助处理：
     - 从上下文获取 `CPSession`；
     - 必填参数校验（`requireContext` / `requireBind`）；
     - 错误响应封装（`businessError` / `argsError`）。
   - 业务链执行完成后，在上下文中写入 `CPResponse`。

4. **响应与推送**
   - `CPControllerDispatcherImpl` 从 LiteFlow 上下文取出 `CPResponse`；
   - 使用 JSON 序列化后，通过 AES-GCM 加密；
   - 使用 Netty 将响应写回客户端。
   - 如需通知其他会话（例如频道消息广播、读状态变更），由 `CPNotificationService`：
     - 根据 uid 列表从 `CPSessionCenterService` 获取所有 `CPSession`；
     - 对每个会话发送一条 `CPNotification` 包裹在 `CPResponse` 中的推送消息。

### 2.2 会话与状态管理

- `CPSessionCenterService` 维护 uid → List\<CPSession\> 的映射；
- 登录成功后，通过 `CPSessionRegisterNode` 注册会话，并把 `CHAT_DOMAIN_USER_ID` 记在 session 属性上；
- 读状态、通知等功能通过 session 中的用户信息实现 “同一用户多会话”的同步。

---

## 3. 安全架构

### 3.1 传输安全

1. **握手阶段**
   - 客户端发送 ECC 公钥；
   - 服务器生成 AES 会话密钥并用 ECC 公钥加密后返回；
   - 双方持有相同对称密钥，进入加密通信阶段。

2. **数据加密与完整性**
   - 使用 AES-GCM 加密业务 JSON 数据；
   - AAD（附加认证数据）包含：
     - 包序号；
     - sessionId；
     - 时间戳（毫秒）。
   - 服务器端校验：
     - 包序号单调递增（防重放、防乱序）；
     - sessionId 一致；
     - 时间戳位于允许的时间窗口内（防止过期包）。

### 3.2 业务安全与权限

在 LiteFlow 链路中通过节点实现业务安全：

- 登录校验：`UserLoginChecker`；
- 权限校验：例如频道管理员、频道所有者检查、禁言检查等；
- 参数校验：各业务节点在执行前检查上下文是否存在必需参数。

错误时统一返回：

- `CPResponse.code` 取 100/300/500 等，对应参数错误、权限错误、服务器错误。

---

## 4. 插件与扩展架构

插件体系依托模块分层与 LiteFlow/DAO 的抽象：

- **侵入性插件**：
  - 通过实现 `api` 中定义的 DAO 或服务接口，并使用 `@Primary` / 条件装配，替换默认实现；
  - 适合底层设施（DAO、外部服务）的替换与增强。

- **拓展性插件**：
  - 通过新增 `@LiteflowComponent` 节点、Service Bean 与 XML 链路配置，实现业务扩展；
  - 不替换现有模块，实现更安全、可控的增量功能。

具体细节参考：

- `doc/plugins/README.md`
- `doc/plugins/plugin-architecture.md`

---

## 5. 典型架构视角下的关注点

1. **性能**
   - DAO 层：合理的索引与缓存策略（见 `doc/ops/performance-guidelines.md`）；
   - LiteFlow：避免链路过长与节点重 IO；
   - 通知与会话：控制单用户会话数、避免高频广播带来的写压力。

2. **可观测性**
   - 关键路径上的节点与 DAO 实现均带有日志；
   - 统一使用英文日志、包括关键字段（uid/cid/route/id 等），便于集中检索与排查。

3. **演进性**
   - API 模块作为稳定契约层，保持接口兼容性；
   - DAO 与 LiteFlow 节点允许通过插件和重构逐步演进；
   - 连接层（Netty + AES/ECC）在不改变协议的情况下可以独立优化实现。

这份文档与 `doc/overview.md` 一起提供了宏观架构视图，细节可参考对应模块文档与特性文档。 

