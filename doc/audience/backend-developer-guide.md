# 后端开发者文档（项目实现视角）

> 目标读者：维护 CarryPigeon Backend 的后端同学。  
> 覆盖：工程结构、运行链路、命名/编码规范、扩展方式、测试与运维关注点。

## 1. 快速导航

- 概览：`doc/overview.md`
- 架构与请求链路：`doc/architecture.md`
- 模块说明入口：`doc/modules/README.md`
- 分发包打包与启动：`doc/package-and-start.md`
- 运维与安全：`doc/ops/*`

## 2. 工程结构（Maven 多模块）

仓库根目录为 Maven multi-module（Java 21 / Spring Boot 3.5.x），核心模块：

- `api/`：稳定边界（协议对象、领域模型、DAO/Service 抽象、Controller/LiteFlow 基类）
- `chat-domain/`：业务域（Netty Controller + LiteFlow 节点 + 会话/通知服务）
- `connection/`：Netty TCP 接入、帧协议、握手与加解密、心跳
- `dao/`：持久化默认实现（MyBatis-Plus + Redis 缓存）
- `common/`：公共工具与基础配置（例如 Jackson）
- `external-service/`：外部集成（例如邮件）
- `application-starter/`：可运行 Spring Boot 应用入口
- `distribution/`：打包与分发工件

## 3. 本地开发（构建 / 运行 / 测试）

- 构建全部模块：`mvn clean install`
- 运行应用：`mvn -pl application-starter -am spring-boot:run`
- 运行测试（根 POM 默认跳过）：`mvn test -DskipTests=false`

## 3.1 配置入口（最常用的几个）

- 配置文件（示例）：`application-starter/src/main/resources/application.yaml`
- 连接端口与 ECC key：
  - 配置类：`api/src/main/java/team/carrypigeon/backend/api/starter/connection/ConnectionConfig.java`
  - 配置项：`connection.port`、`connection.ecc-public-key`、`connection.ecc-private-key`
- LiteFlow 规则：`application-starter/src/main/resources/config/*.xml`

## 4. 关键运行链路（从 TCP 到业务响应）

1. `connection`：Netty 收包 → `NettyDecoder` 长度帧拆包 →（握手后）AES-GCM 解密 + AAD 校验
2. `chat-domain`：`CPControllerDispatcherImpl` 解析 `CPPacket` → 按 `route` 找到 `@CPControllerTag(path=...)` 的 controller
3. `chat-domain`：VO 反序列化 + `insertData` 写入 `CPFlowContext`
4. `LiteFlow`：以 `route` 作为 chain name 执行业务链（规则文件在 `application-starter/src/main/resources/config/*.xml`）
5. `chat-domain`：Result 组装 `CPResponse` 写入上下文
6. `connection`：序列化 `CPResponse` → AES-GCM 加密 → `NettyEncoder` 编码回包

## 5. 对外协议与错误码约定（必须统一）

### 5.1 JSON 字段命名

- 对外 JSON（请求/响应/推送）统一 `snake_case`（见 `common` 的 Jackson 配置）。
- VO/Result 的 Java 字段通常为驼峰，但序列化后对外为下划线命名。

### 5.2 请求 / 响应

- 请求：`CPPacket { id, route, data }`
- 响应：`CPResponse { id, code, data }`
- `code` 约定：
  - `200`：成功
  - `100`：参数/业务错误
  - `300`：权限错误（典型：未登录）
  - `404`：路由不存在
  - `500`：服务端错误

### 5.3 推送（通知）

- 推送通过 `CPResponse` 外层统一封装：`id = -1` 且 `code = 0`
- `data` 为 `CPNotification { route, data }`

推送样例与字段说明见：`doc/push.md`

## 6. 命名与编码规范（项目内约定）

### 6.1 包名与模块边界

- 包名模式：`team.carrypigeon.backend.<module>[.<feature>]`
- `api` 是稳定边界：业务模块与插件优先依赖 `api`，避免依赖实现细节

### 6.2 Controller / VO / Result

- Controller 使用 `@CPControllerTag(path = "/core/...", voClazz = ..., resultClazz = ...)`
- 协议/VO/Result 命名约定：
  - VO：`CP*VO`
  - Result：`CP*Result` / `CP*ResultItem`
- VO 负责参数校验与写入上下文：`insertData(context)` 返回 `false` 表示参数不合法

### 6.3 代码风格

- Java 21；4 空格缩进；UTF-8；Unix 行尾
- 优先构造器/Builder 初始化，避免公共字段可变
- 日志建议英文 + 关键字段（uid/cid/route/id/session_id 等），便于集中检索

## 7. 新增路由的标准步骤（建议按顺序）

1. 在 `chat-domain/controller/netty/...` 新增 Controller（`@CPControllerTag`）
2. 新增 VO（实现 `CPControllerVO`）与 Result（实现 `CPControllerResult`）
3. 在 `application-starter/src/main/resources/config/*.xml` 中新增/修改 LiteFlow chain（chain name 与 `route` 一致）
4. 复用/新增 LiteFlow 节点（优先使用 `attribute` 中的 key 常量，避免魔法字符串）
5. 按需接入通知：
   - 构建 `CPNotification` 的 `data`
   - 使用 `CPNotifier.bind("route","...")` 推送
6. 更新文档：
   - 客户端对接主文档：`doc/audience/client-developer-guide.md`
   - 按领域拆分文档：`doc/api/*`
7. 补充测试（JUnit 5），覆盖成功与常见错误码（100/300/404/500）

## 8. 数据、缓存与性能

- DAO 与缓存说明：`doc/modules/dao-and-cache.md`
- 性能建议：`doc/ops/performance-guidelines.md`
- 数据库结构：`doc/domain/database-schema.md`

## 9. 安全与运维关注点

- 传输安全（ECC + AES-GCM + AAD）：`doc/ops/security-guidelines.md`
- 日志与可观测性：`doc/ops/logging-and-observability.md`
- 部署与配置：`doc/ops/config-and-deploy.md`
