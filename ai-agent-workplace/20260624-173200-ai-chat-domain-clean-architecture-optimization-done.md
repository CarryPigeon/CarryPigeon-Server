任务名称：
chat-domain 整洁架构优化落地

任务目标：
在不调整 Maven 模块边界的前提下，对 chat-domain 落地一轮整洁架构优化，收紧跨 feature 公共边界，拆分核心大服务的高耦合职责，并补齐相应测试与验证。

任务背景：
用户已确认以 chat-domain 为中心持续推进架构优化，不要求中途逐项确认。前序审查已识别出跨 feature 直接依赖内部包、核心应用服务过大、部分 feature 边界不清等问题。

影响模块：
- chat-domain
- ai-agent-workplace

允许修改范围：
- 允许修改 chat-domain 主代码与测试代码
- 允许新增稳定共享契约、应用层 facade / service、局部配置与测试支持类
- 允许在 ai-agent-workplace 记录任务单

禁止修改范围：
- 不修改 Maven 模块依赖方向
- 不修改 infrastructure-service / application-starter / docs 作为本轮主目标
- 不引入新第三方依赖
- 不恢复旧结构或新增未批准的新架构形态

依赖限制：
- 仅使用仓库现有依赖
- chat-domain 仍只依赖 infrastructure-basic 与 *-api

配置限制：
- 非必要不新增配置
- 若必须新增配置，仅允许最小真实使用配置并保持 `cp` 前缀规范

文档依据：
- docs/架构文档.md
- docs/包结构规范.md
- docs/AI协作开发规范.md
- docs/变更审核清单.md
- docs/注释规范.md
- docs/测试规范.md

任务分解 / 执行计划：
1. 建立任务单并确认实现类质量门禁。
2. 收敛跨 feature 的公共认证上下文与公共运行时契约，减少对 `auth/controller/support`、`message/support`、`server/config` 等内部包的直接依赖。
3. 从 `message` 和 `channel` 中拆出高内聚的只读 / 治理 / 目录能力到独立应用服务，保留兼容 facade，降低超大类复杂度。
4. 调整相关 controller / realtime / config / 测试代码以对齐新的契约边界。
5. 运行受影响模块测试，修复失败后复验。
6. 按变更审核清单记录结果、风险与验证证据，归档任务单。

关键假设与依赖：
- 用户已明确授权本轮架构敏感 refactor，可在任务单边界内连续推进，无需逐项再次确认。
- 当前 chat-domain 未处于可立即拆成独立 Maven 子模块的阶段，因此本轮以“模块内边界治理”为主。
- 工作树存在其它未完成改动，本任务只处理与 chat-domain 优化直接相关的文件，不回退他人修改。

实现要求：
- 优先解决结构性根因，而不是只做命名调整。
- 新增共享契约必须放在稳定且可复用的位置，避免再次落入单个 feature 内部目录。
- 大服务拆分后，职责说明和测试需同步更新。

测试要求：
- 至少覆盖受影响应用服务、配置装配和协议入口的成功/失败路径。
- 运行 chat-domain 相关 Maven 测试；若全量过慢，至少运行与受影响包匹配的模块测试并记录命令。

质量门禁：
- chat-domain 编译通过且无新增诊断错误。
- 受影响测试通过。
- 关键边界问题有代码证据显示已收敛。

复审要求：
- 对认证边界、realtime 链路、消息插件目录和应用服务拆分进行深度自检。

文档要求：
- 若未形成新的长期项目规则，不修改 docs。

验收标准：
- chat-domain 内不再由多个 feature 直接依赖 `auth/controller/support` 内部类。
- server 不再直接依赖 message 的 `support/plugin` 内部类。
- message / channel 至少各拆出一组明确的独立应用服务职责，原有超大类复杂度下降。
- 相关测试通过并有记录。

完成定义：
- 代码、测试、验证、自检与任务单记录全部完成。

实际结果：
- 已完成本轮 chat-domain 边界治理与服务拆分落地。
- 新增共享认证与服务端身份契约：
  - `shared.application.auth.AuthenticatedAccount`
  - `shared.controller.support.RequestAuthenticationContext`
  - `shared.application.server.ServerIdentityProvider`
- 保留旧鉴权类型名兼容壳：
  - `features.auth.controller.support.AuthRequestContext`
  - `features.auth.controller.support.AuthenticatedPrincipal`
- `ServerIdentityProperties` 实现 `ServerIdentityProvider`，跨 feature 不再直接依赖 server 配置实现细节。
- 新增 `ChannelQueryApplicationService`，将频道发现 / 审计等只读职责从 `ChannelApplicationService` 中拆出并由 facade 转发。
- 新增 `MessageQueryApplicationService` 与 `MessagePluginCatalogApplicationService`，将消息查询与插件目录职责从 `MessageApplicationService` 中拆出并由 facade 转发。
- 新增 `MessagePluginCatalogItemResult`，稳定 server 与 message 之间的目录输出边界。
- `ServerApplicationService`、`ServerPluginCatalogController`、`ServerDomainCatalogController` 已改为面向新应用层边界，同时保留兼容构造器，降低外围测试和装配改造成本。
- 修复服务发现 `ws_url` 与文档约定不一致的问题，统一为 `wss://`。
- 修复文件下载测试所需的资源消息转换器缺口，并在 realtime 装配测试中增加仅用于测试的生命周期处理器，避免 context runner 真实绑定端口。

验证记录：
- 2026-06-24 执行定向验证：
  - `mvn -o -Dmaven.repo.local=/tmp/chat-domain-m2/repository -pl chat-domain -am -Dtest=FileControllerTests,ServerControllerTests,ServerApplicationServiceTests,RealtimeServerConfigurationContextTests -Dsurefire.failIfNoSpecifiedTests=false test`
  - 结果：通过
- 2026-06-24 执行全量 chat-domain 离线回归：
  - `mvn -o -Dmaven.repo.local=/tmp/chat-domain-m2/repository -pl chat-domain -am test -DskipTests=false`
  - 结果：通过，`Tests run: 343, Failures: 0, Errors: 0, Skipped: 0`

残留风险：
- 旧鉴权兼容壳仍保留在 `features.auth.controller.support`，后续若 application-starter 等外围模块完成迁移，可考虑在独立任务中删除。
- 本轮仅完成模块内边界治理，`ChannelApplicationService` 与 `MessageApplicationService` 仍保留 facade 角色，后续若继续演进，可进一步拆分命令侧用例服务。
- `server` 与 `message` 之间仍通过兼容构造器保留对旧装配方式的迁移通道，后续可在全仓稳定后收口。

知识沉淀 / 是否回写 docs：
- 若优化过程中形成稳定“跨 feature 只可依赖 application/shared 契约”的长期规则，后续建议回写 docs；本轮先以代码落地为主。

产物清理与保留说明：
- 保留任务单；不在源码目录散落临时文件。

补充说明：
- 本任务为实现类任务，需执行实现、验证、修复、复验闭环。
