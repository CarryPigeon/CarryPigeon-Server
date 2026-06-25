任务名称：
测试质量整改第一轮

任务目标：
在不修改业务语义和模块边界的前提下，补齐 `database-api` 缺失契约测试，增强 `database-impl` 适配器映射断言，并纠正 `storage-impl` 测试规格漂移，形成可执行的第一轮测试质量整改结果。

任务背景：
上一轮只读审查确认当前测试方法质量整体未稳定达到成熟项目标准，主要问题集中在 `database-api` 契约测试覆盖不均衡、`database-impl` 写路径断言过浅、`storage-impl` 存在命名/注释/断言语义漂移，以及 `chat-domain` 存在超大测试类。用户已要求继续推进整改。

任务类型：
实现类任务

影响模块：
- `infrastructure-service/database-api`
- `infrastructure-service/database-impl`
- `infrastructure-service/storage-impl`
- `chat-domain`
- `ai-agent-workplace/`

允许修改范围：
- 允许补充和调整上述模块中的测试代码
- 允许在不改变业务行为前提下调整测试注释、`@DisplayName`、断言方式与测试类拆分
- 允许新增本轮任务单

禁止修改范围：
- 不修改正式业务逻辑
- 不修改模块职责、依赖方向和对外协议
- 不新增第三方依赖
- 不借机重构无关测试

依赖限制：
- 仅使用现有测试依赖与现有模块依赖

配置限制：
- 不新增配置

文档依据：
- `AGENTS.md`
- `docs/AI协作开发规范.md`
- `docs/变更审核清单.md`
- `docs/任务单模板.md`
- `docs/测试规范.md`
- `docs/注释规范.md`

任务分解 / 执行计划：
1. 复核本轮涉及测试与当前工作区状态，避免覆盖已有用户改动。
2. 为 `database-api` 新增 auth/user 契约测试，锁定默认写入/更新/撤销能力的 fail-fast 与可覆盖语义。
3. 为 `database-impl` 写路径测试引入参数捕获，验证 record 到 entity 的字段映射而非仅验证委托发生。
4. 纠正 `storage-impl` 命名与显示名称漂移；评估 `chat-domain` 大测试类是否可在本轮安全拆分。
5. 运行受影响模块相关测试；若环境阻塞，记录替代验证与影响。
6. 按审核清单完成自检，关闭任务单。

关键假设与依赖：
- 当前用户希望按上一轮审查建议直接进入测试整改，而不是先单独审核每一项细化方案。
- `chat-domain` 大测试类拆分若牵涉过大，将在本轮只给出拆分边界建议，不强行重写。
- 若 Maven 仍受外部依赖解析限制，将至少执行能在本地命中的最小测试集合或说明阻塞。

实现要求：
- 新增测试必须遵守测试命名、注释和分级规范。
- `database-api` 契约测试要体现公共边界的默认行为和扩展行为。
- `database-impl` 写路径测试必须验证关键字段映射。
- 仅修正 `storage-impl` 规格漂移，不改变存储实现实际契约。

测试要求：
- 至少覆盖新增 `database-api` 契约测试的成功/失败或默认/覆盖语义。
- 至少增强 `database-impl` 中 auth/channel/user 相关写路径断言。
- 运行受影响测试类或对应模块测试。

质量门禁：
- 改动保持在既定模块边界内
- 新增或调整的测试命名、注释、标签符合规范
- 相关测试命令已执行，或明确记录环境阻塞与替代验证
- 任务单记录实际结果、验证记录和残留风险

复审要求：
- 对跨模块测试整改进行深度自检，重点检查边界、注释与断言质量

文档要求：
- 本轮默认不改 `docs/`，除非发现必须沉淀的长期测试规则

验收标准：
- `database-api` 补齐 auth/user 相关 contract 测试
- `database-impl` 至少关键写路径测试不再只停留在 `any()` 委托断言
- `storage-impl` 规格漂移已纠正
- 给出 `chat-domain` 大测试类的处理结果或明确边界建议

完成定义：
- 用户收到可继续用于后续测试整改排期的第一轮落地结果，且本轮改动已完成验证或形成明确阻塞说明

实际结果：
- 已新增 `database-api` 中 auth/account、auth/session、user/profile 三组 contract 测试
- 已增强 `database-impl` 中 auth account、auth refresh session、user profile、channel 写路径测试的字段映射断言
- 已纠正 `storage-impl` 中 MinIO 读取测试的 `@DisplayName` 与断言语义漂移
- 已评估 `chat-domain` 超大测试类拆分风险，本轮未直接拆分，保留为后续独立整改项

验证记录：
- `mvn -o -pl infrastructure-service/database-api -Dtest=AuthAccountDatabaseServiceContractTests,AuthRefreshSessionDatabaseServiceContractTests,UserProfileDatabaseServiceContractTests test`
  - 结果：`database-api` 完成 `compile` 与 `testCompile`，执行测试阶段失败
  - 原因：本地 Maven 仓库缺少 `maven-surefire-plugin:3.5.4` 相关离线依赖，无法在 offline 模式执行 surefire
- `mvn -o -pl infrastructure-service/database-impl,infrastructure-service/storage-impl -DskipTests test-compile`
  - 结果：失败
  - 原因：离线环境缺少 `database-api`、`infrastructure-basic` 及部分第三方依赖，无法完成依赖解析
- 尝试带网络执行 Maven 测试
  - 结果：未执行成功
  - 原因：执行环境自动审批返回 403 鉴权错误，无法取得联网执行权限
- `git diff --check -- infrastructure-service/database-api/src/test/java infrastructure-service/database-impl/src/test/java infrastructure-service/storage-impl/src/test/java ai-agent-workplace/20260624-152051-ai-test-remediation-current.md`
  - 结果：通过，无新增空白与补丁格式问题

残留风险：
- `chat-domain` 超大测试类拆分仍未处理，后续若进入整改应单独建任务单并先抽取共享测试支撑
- Maven 外部依赖与执行环境鉴权问题仍阻塞完整测试执行
- `database-api` 新增 contract 测试采用最小内存实现来表达接口语义；若后续确定该层需要统一 fail-fast/default 能力边界，应回写文档并同步收紧接口契约

知识沉淀 / 是否回写 docs：
- 暂不回写

产物清理与保留说明：
- 保留本任务单作为本轮测试整改记录

补充说明：
- 本轮优先处理边界清晰且收益最高的测试问题，避免与当前正在进行的结构整理互相干扰。
