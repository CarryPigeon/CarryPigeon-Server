任务名称：
database-impl Mapper 扫描范围修正

任务目标：
修复 `DatabaseServiceAutoConfigurationTests.autoConfiguration_enabled_registersDatabaseBeans` 失败，确保 database-impl 的自动配置只扫描真实 Mapper，不误扫 service 内部接口。

任务背景：
当前 `DatabaseMapperScanAutoConfiguration` 使用 `@MapperScan(basePackages = "...impl.mybatis")`，扫描范围过大，会把 `service` 包内部的 `DatabaseOperation` 等接口识别成 Mapper，导致自动配置测试和运行时上下文存在错误装配风险。

影响模块：
- infrastructure-service/database-impl
- ai-agent-workplace

允许修改范围：
- 允许修改 database-impl 自动配置和相关测试
- 允许补充任务单

禁止修改范围：
- 不修改 chat-domain 业务语义
- 不新增第三方依赖
- 不扩大为数据库实现整体重构
- 不修改 docs

文档依据：
- docs/架构文档.md
- docs/包结构规范.md
- docs/AI协作开发规范.md
- docs/变更审核清单.md

执行计划：
1. 定位误扫来源并确认真实 Mapper 包分布。
2. 收紧 `@MapperScan` 范围，只覆盖真实 Mapper。
3. 回归 `DatabaseServiceAutoConfigurationTests`。
4. 记录验证结果并归档。

验收标准：
- `DatabaseServiceAutoConfigurationTests` 通过。
- 自动配置不再尝试为 service 内部接口创建 Mapper Bean。

实际结果：
- 已为 database-impl 的真实 Mapper 显式添加 `@Mapper` 注解。
- 已将 `DatabaseMapperScanAutoConfiguration` 改为按 `annotationClass = Mapper.class` 扫描，避免误扫 service 内部接口。
- 已修复 `DatabaseServiceAutoConfigurationTests.autoConfiguration_enabled_registersDatabaseBeans` 失败。

验证记录：
- 2026-06-25：在 `/tmp/carrypigeon-backend-build-0625b/src` 执行
  `mvn -o -Dmaven.repo.local=/tmp/carrypigeon-m2/repository -pl infrastructure-service/database-impl -Dtest=DatabaseServiceAutoConfigurationTests test`
  通过，结果为 `Tests run: 2, Failures: 0, Errors: 0, Skipped: 0`。
- 2026-06-25：在 `/tmp/carrypigeon-backend-build-0625b/src` 执行
  `mvn -o -Dmaven.repo.local=/tmp/carrypigeon-m2/repository -pl infrastructure-service/database-impl -DskipTests=false test`
  通过，结果为 `Tests run: 91, Failures: 0, Errors: 0, Skipped: 0`。

残留风险：
- 当前无与本次修复直接相关的残留风险；后续新增 Mapper 时需要保持显式 `@Mapper` 标注，否则不会被自动扫描注册。
