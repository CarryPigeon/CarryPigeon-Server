任务名称：

auth-mvp-foundation

任务目标：

在遵循当前架构、AI 协作开发规范与产品需求文档的前提下，为 CarryPigeon 落地最小用户名密码鉴权闭环。当前范围包含注册、登录、JWT access token、refresh token 和最小会话撤销能力，不包含验证码、OAuth、SSO 或复杂权限模型。

任务背景：

当前 `chat-domain` 已完成注册、用户名密码登录、JWT、refresh token 与 refresh session 撤销基础落地。用户继续要求完善鉴权能力，因此当前追加范围为 HTTP access token 校验、请求身份上下文与当前用户查询。

影响模块：

- `chat-domain`
- `application-starter`
- `infrastructure-service/database-api`
- `infrastructure-service/database-impl`
- `ai-agent-workplace`

允许修改范围：

- 在 `chat-domain` 下新增 `features/auth` 相关正式源码与测试
- 在 `ai-agent-workplace/` 新增本任务单与后续阶段性分析材料
- 在明确确认后新增最小数据库迁移文件
- 在现有 auth 注册和登录基础上继续补充 token 与会话相关正式源码与测试
- 在已有 `auth_account` 迁移基础上新增最小 refresh session 迁移
- 在现有 JWT 基础上补充 HTTP access token 校验、请求身份上下文和 `/api/auth/me`

禁止修改范围：

- 不修改既有模块职责与依赖方向
- 不让 `chat-domain` 直接依赖任何 `*-impl`
- 不把具体 JWT、数据库、缓存或密码学实现放入 `infrastructure-basic`
- 不提前扩展 OAuth、SSO、跨服务端身份、复杂权限模型、多端会话治理
- 不实现验证码
- 不实现角色权限、管理后台、多端设备管理或审计日志
- 不实现细粒度授权规则，只区分匿名与已认证
- 不新增未来占位配置
- 不引入新的 JWT 第三方库，优先使用 JDK crypto 与项目既有 JSON 能力完成最小 HS256 JWT

依赖限制：

- 优先复用现有 Spring Boot 与项目已存在依赖
- 当前已存在的 Argon2 相关依赖继续按既有落地使用
- JWT 采用 JDK crypto + 项目既有 JSON 能力，不新增 JWT 相关依赖
- 不允许为了方便将实现型依赖放入 `chat-domain`

配置限制：

- 新增当前真实使用的最小 `cp.chat.auth.jwt.*` 配置
- 配置必须包含访问令牌 TTL、刷新令牌 TTL、issuer 与 HMAC secret

文档依据：

- `docs/产品需求文档.md`
- `docs/AI协作开发规范.md`
- `docs/任务单模板.md`
- `docs/变更审核清单.md`
- `docs/架构文档.md`
- `docs/包结构规范.md`
- `docs/依赖引入规范.md`
- `docs/配置规范.md`
- `docs/异常与错误码规范.md`
- `docs/测试规范.md`
- `docs/注释规范.md`

实现要求：

- `chat-domain` 采用 `features/auth` 按 feature 优先、层次次之的结构落地
- 应用层负责注册、登录、刷新令牌与注销/撤销会话用例编排
- 协议层负责对受保护 HTTP API 执行 Bearer access token 校验并绑定请求身份
- 领域层只保留业务语义、仓储抽象与规则，不承载具体中间件细节
- 协议层通过现有 `CPResponse` 与 `ProblemException` 维持稳定响应语义
- 当前实现范围收敛为：用户名密码注册、用户名密码登录、JWT access token、refresh token、refresh session 撤销
- 登录成功返回 access token、refresh token 与最小账户标识信息
- 刷新成功轮换 refresh token，并撤销旧 refresh session

测试要求：

- `chat-domain` 测试覆盖注册、登录、刷新和注销/撤销的成功路径与失败路径
- 响应码至少覆盖 `100`、`200`、`300`、`404`、`500` 中与鉴权场景相关的分支
- 测试类与测试方法命名遵循项目规范
- 测试注释说明验证的契约边界
- 数据库相关验证继续以 mock / fake / standalone 为主，不将真实数据库联调作为当前任务完成前提

文档要求：

- 本任务执行中的 AI 中间产物统一放入 `ai-agent-workplace/`
- 若未引入新的长期规则，不修改 `docs/`
- 若后续确认产生长期规则变化，必须先回写 `docs/` 再继续扩展实现

验收标准：

- 注册、登录、刷新和注销/撤销接口均已落地并通过 mock-oriented 测试验证
- `/api/auth/me` 能返回当前 access token 对应的账户标识
- 未携带 access token 或 access token 无效的受保护 API 返回 `CPResponse.code = 300`
- 代码实现严格限制在 auth token/session 边界内，不包含验证码、OAuth、SSO、角色权限或管理能力
- 编码完成后需能按审核清单说明改动内容、原因、影响范围、测试与剩余风险

补充说明：

- 当前已确认：正式产品来源为 `docs/产品需求文档.md`
- 当前不处理：验证码、OAuth、SSO、复杂权限模型、多端设备管理
- 当前阶段数据库真实运行环境仍可能因本地外部服务条件受限而无法完成 HTTP 级联调，但这不阻塞 mock-oriented 任务验收

## 当前实现结果

- 已完成用户名密码注册接口：`POST /api/auth/register`
- 已完成用户名密码登录接口：`POST /api/auth/login`
- 已完成 JWT access token 与 refresh token 签发
- 已完成 refresh token 轮换：`POST /api/auth/refresh`
- 已完成 refresh session 撤销：`POST /api/auth/logout`
- 已完成 Bearer access token 请求身份校验与受保护 HTTP API 接入
- 已完成当前用户查询：`GET /api/auth/me`

## 当前影响文件（摘要）

- `chat-domain/src/main/java/team/carrypigeon/backend/chat/domain/features/auth/**`
- `chat-domain/src/test/java/team/carrypigeon/backend/chat/domain/features/auth/**`
- `infrastructure-service/database-api/src/main/java/team/carrypigeon/backend/infrastructure/service/database/api/**`
- `infrastructure-service/database-impl/src/main/java/team/carrypigeon/backend/infrastructure/service/database/impl/**`
- `infrastructure-service/database-impl/src/test/java/team/carrypigeon/backend/infrastructure/service/database/impl/config/DatabaseServiceAutoConfigurationTests.java`
- `application-starter/src/main/resources/application.yaml`
- `application-starter/src/main/resources/db/migration/V1__create_auth_account.sql`
- `application-starter/src/main/resources/db/migration/V2__create_auth_refresh_session.sql`

## 自检与验收记录

- 架构边界：已满足。`chat-domain` 未直接依赖任何 `*-impl`，数据库读写仍通过 `database-api` 抽象进入 `database-impl`
- 范围边界：已满足。未引入验证码、OAuth、SSO、角色权限、管理后台、多端设备治理或审计日志
- 配置边界：已满足。仅新增最小 `cp.chat.auth.jwt.*` 配置，包含 issuer、secret、access-token-ttl、refresh-token-ttl
- 数据迁移：已满足。新增 `auth_account` 与 `auth_refresh_session` 两个最小迁移脚本
- Mock-oriented 验证：已满足。应用层、控制层、拦截器层均通过 fake/mock/standalone 测试验证，无需真实数据库联调作为完成前提
- 受保护接口审计点：已补齐。当前存在直接测试覆盖：
  - `/api/auth/me` 已认证成功返回 `100`
  - `/api/auth/me` 匿名访问返回 `300`
  - 缺失 Bearer token 返回认证失败语义
  - 无效 Bearer access token 返回认证失败语义
- 构建验证：已执行并通过 `mvn test -DskipTests=false`
- 构建验证：已执行并通过 `mvn clean install -DskipTests=false`
- LSP 诊断：当前环境缺少 `jdtls`，无法执行 Java LSP 诊断；已以 Maven 编译、测试、打包通过作为替代证据

## 残留说明

- 当前未做实时通道鉴权绑定
- 当前未做细粒度授权，仅区分匿名与已认证
- 当前真实 HTTP 运行环境仍可能受本地 MySQL 凭据影响，仓库内验收以 mock-oriented 结果为准
