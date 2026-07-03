任务名称：

domain 能力端口包结构规则改进

任务目标：

将 `chat-domain` 中领域能力端口从 `domain/service` 移出，明确区分：

- `domain/api`：向 controller、server、其他 feature 暴露 domain 能力的入口接口。
- `domain/port`：domain 服务依赖的外部/技术/跨边界能力端口。

同时使 `domain/service` 只保留业务服务与业务协作实现。

任务背景：

当前 auth feature 中 `PasswordHasher`、`TokenHasher`、`AuthTokenService`、`EmailVerificationCodeService` 等能力端口位于 `domain/service`。这虽然能表达领域依赖的抽象能力，但会让 `service` 目录同时包含业务服务实现和能力端口接口，降低目录语义纯度。

同时，controller 等调用方如果直接依赖具体 `*DomainService`，也会让领域能力的外部入口不够稳定。用户明确要求增加一个接口目录，用于向外暴露 domain 能力，而不是直接暴露 service 实现。

影响模块：

- `chat-domain`
- `docs`
- `ai-agent-workplace`

允许修改范围：

- `docs/包结构规范.md`
- `chat-domain/src/main/java/team/carrypigeon/backend/chat/domain/features/auth/domain/api/**`
- `chat-domain/src/main/java/team/carrypigeon/backend/chat/domain/features/auth/domain/service/**`
- `chat-domain/src/main/java/team/carrypigeon/backend/chat/domain/features/auth/domain/port/**`
- `chat-domain/src/main/java/team/carrypigeon/backend/chat/domain/features/auth/support/**`
- `chat-domain/src/main/java/team/carrypigeon/backend/chat/domain/features/auth/config/**`
- `chat-domain/src/test/java/team/carrypigeon/backend/chat/domain/features/auth/**`
- 与上述迁移直接相关的 import 和测试引用
- 当前任务单本身

禁止修改范围：

- 不改变业务行为、接口协议、错误码和响应字段。
- 不新增第三方依赖。
- 不修改 Maven 模块结构。
- 不移动 repository 抽象；repository 仍保留在 `domain/repository`。
- 不扩大到全 feature 全量重组，除非发现编译必须同步。

拟定约束：

- 新增 `domain/api`：放向外暴露 domain 能力的入口接口，例如 `AuthApi` / `AuthTokenApi` 这类由 domain service 实现、供 controller 或其他 feature 注入使用的接口。
- 新增 `domain/port`：放领域服务依赖的非持久化能力端口，例如密码哈希、token 签发、验证码服务、跨 feature 语义边界等。
- 保持 `domain/repository`：只放持久化语义端口。
- 收紧 `domain/service`：只放领域服务、业务用例入口、业务策略/协作实现，不放纯能力端口接口，也不作为对外接口类型。
- `support`：放 `domain/port` 或 `domain/repository` 的技术适配实现。
- `config`：负责选择并装配具体实现。

实施步骤：

1. 更新 `docs/包结构规范.md`，加入 `domain/api`、`domain/port` 目录与约束。
2. 将 auth 下非持久化能力端口从 `domain/service` 移到 `domain/port`。
3. 为 auth 对外能力新增 `domain/api` 接口，并让具体 domain service 实现该接口。
4. 更新 auth controller/config/tests 的注入类型，使外部调用优先依赖 `domain/api`，不是具体 `domain/service` 实现。
5. 更新 auth domain/support/config/tests 的 import。
6. 扫描 `domain/service` 是否仍残留 auth 能力端口接口，扫描 controller 是否仍直接依赖 auth 具体 domain service。
7. 运行 auth 相关测试，必要时运行 `chat-domain` 模块测试。

验收标准：

- `domain/service` 下不再保留 auth 非持久化能力端口接口。
- auth 对外入口接口位于 `domain/api`，外部调用方不直接依赖具体 `AuthDomainService`。
- `support` 实现依赖 `domain/port` 端口。
- `docs/包结构规范.md` 清楚说明 `domain/api`、`domain/port`、`domain/repository`、`domain/service` 的边界。
- 受影响测试通过。
- 任务单记录实际结果、验证命令、残留风险，并关闭为 `done`。

执行前确认点：

- 本任务涉及包结构规范和源码迁移，需用户确认后再开始修改正式代码与 docs。

实际结果：

- 更新 `docs/包结构规范.md`：
  - 新增 `domain/api`：对外暴露 domain 能力的入口接口。
  - 新增 `domain/port`：领域服务依赖的非持久化能力端口。
  - 收紧 `domain/service`：只放领域服务、业务用例实现、业务策略与协作对象。
  - 明确 `domain/port` 实现应位于对应 feature 的 `support` 或 `config` 附近。
- auth feature 落地新结构：
  - 新增 `domain/api/AuthApi`，作为 controller 访问 auth domain 能力的入口接口。
  - `AuthDomainService` 实现 `AuthApi`，仍保留在 `domain/service` 作为业务服务实现。
  - 将 `AuthTokenService`、`EmailVerificationCodeService`、`PasswordHasher`、`TokenHasher` 从 `domain/service` 移到 `domain/port`。
  - `AuthController` 与 controller 测试改为依赖 `AuthApi`，不再直接依赖 `AuthDomainService`。
  - auth support/config/test 中相关 import 更新为 `domain/port`。

验证记录：

- 结构扫描：
  - `chat-domain/src/main/java/team/carrypigeon/backend/chat/domain/features/auth/domain/service` 下未发现 `public interface`。
  - `chat-domain/src/main/java/team/carrypigeon/backend/chat/domain/features/auth/domain/port` 下包含 4 个能力端口。
  - `chat-domain/src/main/java/team/carrypigeon/backend/chat/domain/features/auth/domain/api` 下包含 `AuthApi`。
  - auth controller 与 controller 测试未发现对 `features.auth.domain.service.AuthDomainService` 的直接依赖。
  - 未发现旧端口包 `features.auth.domain.service.(AuthTokenService|PasswordHasher|TokenHasher|EmailVerificationCodeService)` 的引用残留。
- `mvn -pl chat-domain -am -Dtest='team.carrypigeon.backend.chat.domain.features.auth.controller.http.AuthControllerTests,team.carrypigeon.backend.chat.domain.features.auth.controller.support.AuthAccessTokenInterceptorTests,team.carrypigeon.backend.chat.domain.features.auth.config.AuthWebMvcConfigurationTests,team.carrypigeon.backend.chat.domain.features.auth.domain.service.AuthDomainServiceAuthenticationTests,team.carrypigeon.backend.chat.domain.features.auth.domain.service.AuthDomainServiceRegistrationTests,team.carrypigeon.backend.chat.domain.features.auth.domain.service.AuthDomainServiceTokenFlowTests,team.carrypigeon.backend.chat.domain.features.auth.support.verification.CacheBackedEmailVerificationCodeServiceTests,team.carrypigeon.backend.chat.domain.features.auth.support.verification.InMemoryEmailVerificationCodeServiceTests' -Dsurefire.failIfNoSpecifiedTests=false test -DskipTests=false`
  - 首次运行因 `AuthTokenIssuer` 缺少迁移后的端口 import 编译失败；已补正。
  - 复跑通过：37 tests, 0 failures, 0 errors, 0 skipped。
- `mvn -pl chat-domain -am test -DskipTests=false`
  - 通过：chat-domain 302 tests, 0 failures, 0 errors, 0 skipped；依赖模块同时通过。

残留风险：

- 本轮仅在 auth feature 落地新目录规则；其他 feature 如需完全一致，还需要后续按同一规则逐步迁移。
- 工作树存在大量本任务前已有的未提交变更，本轮未回滚或清理无关变更。

知识沉淀 / 是否回写 docs：

- 已回写 `docs/包结构规范.md`，新增 `domain/api` 与 `domain/port` 长期规则。

产物清理与保留说明：

- 保留本任务单作为包结构规则变更追踪记录，完成后由 `current` 重命名为 `done`。
