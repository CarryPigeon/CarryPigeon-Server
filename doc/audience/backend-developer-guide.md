# 后端开发者文档（实现与维护视角）

> 面向维护 CarryPigeon Backend 的开发者。

## 1. 快速导航

- 总览：`doc/overview.md`
- 架构：`doc/architecture.md`
- 模块入口：`doc/modules/README.md`
- API 规范：`doc/api/文档索引.md`
- 运维与安全：`doc/ops/*`

## 2. 核心模块

- `api/`：领域模型、DAO/Service 抽象、协议对象
- `chat-domain/`：HTTP 控制器、WebSocket 会话处理、LiteFlow 节点
- `dao/`：MyBatis-Plus + Redis 默认实现
- `application-starter/`：应用启动与配置聚合

## 3. 本地开发命令

- 构建：`mvn clean install`
- 运行：`mvn -pl application-starter -am spring-boot:run`
- 测试：`mvn test -DskipTests=false`

## 4. 请求链路（HTTP `/api`）

1. Controller 接收请求并组装 DTO
2. `ApiAccessTokenFilter`（按需）完成鉴权
3. `ApiFlowRunner` 执行 `api_*` LiteFlow chain
4. Result 节点写入 `ApiFlowKeys.RESPONSE`
5. `ApiExceptionHandler` 输出统一错误模型

## 5. LiteFlow 规则与命名

- 规则目录：`application-starter/src/main/resources/config/api_*.xml`
- chain 命名：`api_<resource>_<action>`（例如 `api_channels_messages_create`）
- 节点命名：按职责区分 `Checker/Selector/Builder/Saver/Result`

## 6. 新增 API 的标准流程

1. 在 `chat-domain/controller/web/api` 增加端点
2. 复用或新增 `cmp/api` 节点
3. 在 `api_*.xml` 中声明 chain
4. 补充测试（成功 + 常见错误）
5. 更新文档：`doc/api/11-HTTP端点清单.md`

## 7. 强制一致性约束

- 对外 JSON 统一 `snake_case`
- 错误结构统一 `{ error: { status, reason, message, ... } }`
- 实体 ID 对外统一十进制字符串
- 文档以 `doc/api/*` 为协议唯一事实来源
