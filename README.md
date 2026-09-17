# CarryPigeon Backend

CarryPigeon Backend 是一个基于 **Java 21 + Spring Boot + Maven 多模块** 的开源聊天后端项目。

当前仓库处于深度优化迭代阶段：已经具备较清晰的模块边界、Swagger API 门户、测试与分发链路，但当前仍以**本地开发 / 自建测试环境 / 持续重构**为主要使用场景，而不是已经完全稳定的通用生产发行版。

## 当前状态
- 当前阶段：深度优化迭代中
- 主要目标：稳定聊天内核、HTTP API、标准错误模型、基础鉴权与可替换基础设施边界
- 当前交付方式：`thin jar + libs` 分发
- 当前外部依赖：MySQL、Redis、MinIO
- 当前文档入口：`docs/api/API.md` 与运行时 Swagger / OpenAPI 门户

## 主要特性

- Maven 多模块后端结构，模块职责边界清晰
- 直接资源成功响应与统一错误响应模型
- 基于 Bearer Token 的受保护接口访问
- Swagger / OpenAPI 门户，包含分组、字段说明、成功/失败示例
- thin-jar + libs 分发模式
- 插件级拓展
- 当前已覆盖较完整的模块级与协议级测试

## 模块结构

- `application-starter`：启动与运行时装配模块
- `chat-domain`：核心业务域模块
- `infrastructure-basic`：固定全局基础设施模块
- `infrastructure-service`：可拔插外部服务基础设施父模块
  - 各级子模块承当具体的外部服务功能
- `distribution`：当前 thin jar + libs 打包与分发模块

## 快速开始

### 1. 准备外部依赖

当前项目依赖以下外部服务，外部服务的启动脚本已经写入了`docker-compose.yaml`，但是该文件中包含nginx相关配置，本地配置推荐使用启动脚本启动：

- MySQL
- Redis
- MinIO
- nginx

```bash
bash bin/linux/docker-up.sh
```

更多环境准备请参考：

- `docs/operations/部署手册.md`
- `docs/operations/数据库部署手册.md`
- `docs/sql/README.md`

### 2. 运行测试

```bash
mvn test -DskipTests=false
```

### 3. 启动应用

请参考：

- `docs/operations/启动脚本索引.md`
- `docs/operations/部署手册.md`

## Swagger / OpenAPI

当前项目已接入 Springdoc OpenAPI，推荐作为日常联调入口。

- Swagger UI：`/swagger-ui/index.html`
- 兼容入口：`/swagger-ui.html`
- OpenAPI JSON：`/v3/api-docs`
- OpenAPI YAML：`/v3/api-docs.yaml`

### 人工调试

推荐联调流程：

1. 启动外部依赖与应用本身
2. 打开 Swagger UI 查看接口分组、请求/响应示例与错误示例
3. 若接口需要认证，先调用登录接口获取 `accessToken`
4. 在 Swagger UI 的 `Authorize` 中填写 `Bearer <access-token>`
5. 调用受保护接口时，按 HTTP 状态码判断成功或失败；失败响应读取 `error.reason` 和 `error.message`

### 智能体调试1

推荐使用ai智能体对接口进行测试，后续会在项目中加入智能体真实api测试规范文档
TODO 智能体真实API测试规范完善

更多接口细节请参考 `docs/api/API.md`。

## 文档索引

推荐优先阅读：

- `docs/api/API.md`：当前 HTTP API 文档与使用说明
- `docs/architecture/架构文档.md`：模块职责与依赖边界
- `docs/operations/部署手册.md`：运行方式、分发与部署边界
- `docs/operations/数据库部署手册.md`：MySQL、Flyway 与数据库初始化说明
- `docs/sql/README.md`：按领域划分的数据库聚合 SQL
- `docs/standards/测试规范.md`：测试分层与质量门禁
- `docs/standards/AI协作开发规范.md`：AI / 协作开发规则

脚本与平台相关说明：

- `docs/operations/启动脚本索引.md`
- `bin/linux/README.md`
- `bin/windows/README.md`

## 当前边界说明

- 当前项目仍处于迭代期，不应默认视为生产级稳定发行版
- Docker 当前只承接外部依赖，不承接应用容器化（由于插件化性质决定）
- 当前部署手册不覆盖 Kubernetes、多节点高可用或生产级进程托管方案由部署方自行优化实现
- AI 中间产物统一放入 `ai-agent-workplace/`，其仅用于本地审查，禁止上传到代码仓库污染代码

## 开发与协作

- 请遵循仓库根目录 `AGENTS.md`
- 正式规则以 `docs/` 为准
- 长期边界变更需要先更新文档，再改代码
