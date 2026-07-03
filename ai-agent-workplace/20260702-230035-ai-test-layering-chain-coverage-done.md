# 测试分包与链路补足任务单

## 任务类型

实现类任务。本任务补足测试链路验证能力，整理测试分包，并把后续开发者可执行的测试命令写入正式文档。

## 任务目标

- 区分 mock 协议测试与真实业务链路测试的测试包结构。
- 补足一批能够验证真实业务链路的测试，不只停留在 controller mock 映射断言。
- 将对应测试分类、标签和 Maven 执行命令写入 `docs/测试规范.md`。
- 保持生产代码、模块职责、依赖方向和对外协议不变。

## 受影响模块

- `chat-domain`
- 必要时只读查看 `application-starter`、`infrastructure-service/*`
- `docs/测试规范.md`
- `ai-agent-workplace/` 当前任务单

## 允许修改范围

- `chat-domain/src/test/java`
- 必要时各模块 `src/test/java`
- `docs/测试规范.md`
- `ai-agent-workplace/` 当前任务单

## 禁止边界

- 不修改生产代码行为。
- 不调整正式模块职责、包结构规范或依赖方向。
- 不新增外部服务、中间件或重量级依赖。
- 不引入新的测试框架。
- 不修改对外 HTTP 协议。
- 不把 `chat-domain` 测试改为依赖 `*-impl`。

## 依据文档

- `docs/测试规范.md`
- `docs/注释规范.md`
- `docs/架构文档.md`
- `docs/包结构规范.md`
- `docs/依赖引入规范.md`
- `docs/AI协作开发规范.md`
- `docs/变更审核清单.md`

## 执行计划

1. 阅读现有测试结构、pom 配置和关键认证/用户链路代码。
2. 定义并落地保守分包方式：mock 协议测试放在 `controller/http/mock`，真实业务链路测试放在 `features/*/chain`。
3. 迁移已 mock 的 controller 测试类到 mock 包，保持类名、方法名和标签稳定。
4. 补充真实业务链路测试，优先覆盖认证注册/登录/刷新和用户资料更新等不依赖真实外部服务的领域链路。
5. 更新 `docs/测试规范.md`，写明分包规则、标签含义和常用 Maven 命令。
6. 运行与改动匹配的 Maven 测试，记录结果。
7. 自检并关闭任务单为 `done`。

## 质量门禁

- 迁移后的测试仍能编译通过。
- 新增测试类命名符合 `<Name>Tests`。
- 新增测试方法命名符合 `methodName_condition_expected()`。
- 新增测试注释说明验证的真实链路、边界和限制。
- mock 测试仍主要验证协议映射与错误响应。
- 真实业务链路测试不得依赖真实外部服务或 `*-impl`。
- 文档中的命令可直接执行，且与标签/模块结构一致。
- 执行受影响 Maven 测试并记录结果。

## 验收标准

- 完成测试分包。
- 补足至少一组真实业务链路测试。
- `docs/测试规范.md` 包含后续开发者可使用的分类说明和命令。
- 验证命令通过，或明确记录无法通过的原因。
- 任务单关闭为 `done`。

## 执行结果

### 实际变更

- `chat-domain`
  - 将 `AuthControllerTests` 移入 `features/auth/controller/http/mock` 包，并标记为 `@Tag("mock")`。
  - 将 `UserProfileControllerTests` 移入 `features/user/controller/http/mock` 包，并标记为 `@Tag("mock")`。
  - 新增 `features/auth/chain/AuthUserBusinessChainTests`，标记为 `@Tag("business")`。
- `docs/测试规范.md`
  - 补充 mock 测试与真实业务链路测试分包规则。
  - 补充 `mock`、`business` 标签定义。
  - 补充后续开发者可直接执行的 Maven 命令。

### 新增链路覆盖

- 邮箱验证码创建会话链路：
  - `AuthController`
  - `AuthSessionDomainApi`
  - 内存账户仓储、refresh session 仓储、用户资料仓储、频道成员仓储
  - `AuthAccessTokenInterceptor`
  - `UserProfileController`
  - `UserProfileDomainApi`
- 用户名密码注册、登录、刷新、撤销链路：
  - 验证注册创建账户与资料
  - 验证登录写入 refresh session
  - 验证刷新撤销旧 session 并创建新 session
  - 验证撤销 refresh token 后 session 状态变更

### 文档命令

- 全量受影响模块测试：
  - `mvn -pl chat-domain -am test -DskipTests=false`
- mock 协议测试：
  - `mvn -pl chat-domain -am test -DskipTests=false -Dtest.groups=mock`
- 真实业务链路测试：
  - `mvn -pl chat-domain -am test -DskipTests=false -Dtest.groups=business`
- 快速排除业务链路测试：
  - `mvn test -DskipTests=false -Dtest.excludedGroups=business`

### 验证结果

- 已执行：`mvn -pl chat-domain -am test -DskipTests=false -Dtest.groups=business`
  - 结果：通过。
  - 汇总：2 tests, 0 failures, 0 errors, 0 skipped。
- 已执行：`mvn -pl chat-domain -am test -DskipTests=false -Dtest.groups=mock`
  - 结果：通过。
  - 汇总：23 tests, 0 failures, 0 errors, 0 skipped。
- 已执行：`mvn -pl chat-domain -am test -DskipTests=false`
  - 结果：通过。
  - 汇总：304 tests, 0 failures, 0 errors, 0 skipped。

### 自检结论

- 未修改生产代码行为。
- 未调整正式模块职责或依赖方向。
- 未新增外部依赖或测试框架。
- `chat-domain` 没有依赖 `infrastructure-service/*-impl`。
- mock 测试和真实业务链路测试已按包结构和标签区分。
- 文档已记录可执行命令，且命令已实际验证。

### 残留风险

- `business` 测试验证的是 `chat-domain` 内真实业务链路与内存替身，不验证 MySQL、Redis、MinIO、SMTP 等真实外部服务。
- 真实 Spring 容器装配仍主要由 `application-starter` 的 smoke/config 测试覆盖；本轮没有新增 `@SpringBootTest` 端到端测试。
- 当前只为认证到用户资料补足了业务链路样例，其它 feature 可按同一规则逐步补齐。
