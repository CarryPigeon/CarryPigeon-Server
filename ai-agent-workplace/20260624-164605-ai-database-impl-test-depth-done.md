任务名称：
database-impl 测试断言加深

任务目标：
增强 `database-impl` 适配器测试中剩余的浅断言点，使关键写路径测试从“只验证委托发生”提升为“验证字段映射与稳定失败语义”。

任务背景：
前几轮已修复 `auth account`、`auth refresh session`、`user profile`、`channel` 等部分适配器的浅断言问题，但 `channel read state`、`notification preference`、`channel pin`、`channel ban`、`channel member`、`channel audit log`、`channel invite` 等测试仍存在 `verify(any())` 式浅断言。

任务类型：
实现类任务

影响模块：
- `infrastructure-service/database-impl`
- `ai-agent-workplace/`

允许修改范围：
- 允许增强 `database-impl` 相关测试断言
- 允许补充必要测试用例
- 允许新增本轮任务单

禁止修改范围：
- 不修改正式业务逻辑
- 不修改模块依赖方向
- 不新增第三方依赖

依赖限制：
- 仅使用现有测试依赖

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
1. 盘点 `database-impl` 中剩余浅断言的测试点。
2. 优先增强关键写路径测试，使用参数捕获校验实体字段映射。
3. 必要时补充缺失的 upsert/update 测试。
4. 执行本地可行验证并记录阻塞。

关键假设与依赖：
- 现有适配器实现字段映射已稳定，新增断言不会触发大面积业务调整。
- 若 Maven 真实测试执行仍受离线依赖阻塞，则至少完成 `test-compile` 或 `git diff --check` 级别验证。

实现要求：
- 断言直接对应适配器 record -> entity 映射
- 不为了补断言而引入测试噪音
- 仅对高信号写路径加强，不机械重写所有文件

测试要求：
- 至少覆盖 `ChannelReadState`、`NotificationPreference`、`ChannelPin`、`ChannelBan`、`ChannelMember`、`ChannelAuditLog`、`ChannelInvite` 中当前的浅断言点

质量门禁：
- 关键浅断言点已被参数捕获或等价高信号断言替换
- 无新增格式问题
- 验证命令或阻塞原因记录完整

复审要求：
- 自检字段映射断言是否准确、是否误把实现细节写死

文档要求：
- 默认不改 `docs/`

验收标准：
- 本轮选定的浅断言点已完成增强
- 用户可继续用现有测试识别字段映射回归

完成定义：
- `database-impl` 关键适配器测试进一步接近成熟项目标准

实际结果：
- 已增强以下测试中的浅断言点：
  - `MybatisPlusChannelReadStateDatabaseServiceTests`
  - `MybatisPlusNotificationPreferenceDatabaseServiceTests`
  - `MybatisPlusChannelPinDatabaseServiceTests`
  - `MybatisPlusChannelBanDatabaseServiceTests`
  - `MybatisPlusChannelMemberDatabaseServiceTests`
  - `MybatisPlusChannelAuditLogDatabaseServiceTests`
  - `MybatisPlusChannelInviteDatabaseServiceTests`
- 关键写路径测试已由 `verify(any())` 提升为参数捕获并校验字段映射
- 额外补充了 `NotificationPreference` 的频道级 upsert 映射测试

验证记录：
- `git diff --check -- infrastructure-service/database-impl/src/test/java/team/carrypigeon/backend/infrastructure/service/database/impl/mybatis/service ai-agent-workplace/20260624-164605-ai-database-impl-test-depth-current.md`
  - 结果：通过，无新增空白与补丁格式问题
- `mvn -o -pl infrastructure-service/database-impl -DskipTests test-compile`
  - 结果：失败
  - 原因：离线环境缺少 `database-api`、`infrastructure-basic` 及第三方依赖，无法完成依赖解析

残留风险：
- Maven 离线依赖问题仍阻塞 `database-impl` 的真实编译与测试执行
- 还有少量 adapter 测试未逐一深挖，但本轮已覆盖当前最显著的写路径浅断言点

知识沉淀 / 是否回写 docs：
- 暂不回写

产物清理与保留说明：
- 保留本任务单作为本轮整改记录

补充说明：
- 本轮只处理高信号浅断言，不做无边界的大面积重写。
