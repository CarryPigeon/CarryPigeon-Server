# 测试系统三层分层落地任务单

## 任务类型

实现类任务。根据已确认方案落地测试系统三层分层：单方法功能测试、调用链测试、真实环境测试。

## 任务目标

- 更新长期测试规范，明确三层测试模型、目录边界、标签与命令。
- 在现有测试体系上做最小必要标签整理。
- 为真实环境测试设计统一数据注入/清理基类或约定骨架，避免因数据缺失导致测试失败。
- 不改变生产代码行为。

## 受影响模块

- `docs/`
- `chat-domain/src/test/java`
- `infrastructure-service/*/src/test/java`
- `application-starter/src/test/java`（如需）
- `ai-agent-workplace/` 当前任务单

## 允许修改范围

- 测试规范文档与测试命令说明。
- 测试代码标签、测试辅助类、测试资源。
- 真实环境测试数据注入/清理辅助骨架。

## 禁止边界

- 不修改生产业务逻辑。
- 不调整模块职责或依赖方向。
- 不让 `chat-domain` 依赖 `infrastructure-service/*-impl`。
- 不接入新的测试框架或新依赖，除非发现 Maven 现有能力无法满足且再单独确认。
- 不处理与本任务无关的既有脏状态。

## 依据文档

- `docs/standards/测试规范.md`
- `docs/standards/AI协作开发规范.md`
- `docs/standards/变更审核清单.md`
- 当前用户确认的三层测试方案

## 执行计划

1. 梳理现有测试标签、目录与命令配置。
2. 更新 `docs/standards/测试规范.md`，固化三层模型：
   - 单方法功能测试
   - 调用链测试
   - 真实环境测试
3. 补充真实环境测试数据注入/清理规则和命令。
4. 对现有关键测试补齐主标签，优先覆盖 `business`、`mock`、`contract`、`unit`、`env-*`。
5. 如适合，新增真实环境测试 fixture 骨架，不默认启用真实外部服务测试。
6. 运行分层验证命令和全量测试。
7. 按审核清单记录结果并归档任务单。

## 验收标准

- 文档明确三层测试职责、目录、标签和命令。
- 现有 business 调用链测试仍可按标签运行。
- 单方法功能测试可排除 `business/env` 运行。
- 真实环境测试具备数据注入/清理规范或辅助骨架。
- `mvn -pl chat-domain -am test -DskipTests=false -Dtest.groups=business` 通过。
- `mvn -pl chat-domain -am test -DskipTests=false` 通过。

## 实际变更

- 更新 `docs/standards/测试规范.md`：
  - 固化三层测试模型：单方法功能测试、调用链测试、真实环境测试。
  - 新增 `env` / `env-db` / `env-cache` / `env-storage` / `env-mail` / `env-app` 标签族定义。
  - 补充真实环境测试数据注入、唯一命名空间、反向清理和环境准备规则。
  - 补充三层模型与真实环境测试的 Maven 执行命令。
- 新增 `infrastructure-service/database-impl/src/test/java/.../testsupport/EnvTestDataScope.java`：
  - 为真实数据库环境测试提供测试侧数据命名空间和清理栈。
  - 不连接真实数据库，不引入生产依赖，只提供 env 测试生命周期骨架。
- 新增 `EnvTestDataScopeTests`：
  - 覆盖命名空间生成、key 生成、空名称校验、反向清理、清理失败暴露、关闭后拒绝注册。

## 验证记录

- 通过：`mvn -pl infrastructure-service/database-impl -am test -DskipTests=false -Dtest=EnvTestDataScopeTests -Dsurefire.failIfNoSpecifiedTests=false`
  - 结果：5 tests, 0 failures, 0 errors, 0 skipped。
- 通过：`mvn -pl chat-domain -am test -DskipTests=false -Dtest.groups=business`
  - 结果：38 tests, 0 failures, 0 errors, 0 skipped。
- 通过：`mvn -pl chat-domain -am test -DskipTests=false -Dtest.excludedGroups=business,env,env-db,env-cache,env-storage,env-mail,env-app`
  - 结果：302 tests, 0 failures, 0 errors, 0 skipped。
- 通过：`mvn -pl chat-domain -am test -DskipTests=false`
  - 结果：340 tests, 0 failures, 0 errors, 0 skipped。

## 已知情况与风险

- 本轮没有新增真实 MySQL/Redis/MinIO/SMTP 测试本体，因为当前没有确认的真实测试环境、schema 初始化入口和可稳定执行的环境变量契约。
- 真实环境测试规则和数据生命周期 helper 已落地，后续新增 `env-db` 等测试时必须由测试用例主动注入数据并注册清理动作。
- 曾尝试执行 `mvn -pl application-starter -am test -DskipTests=false -Dtest=EnvTestDataScopeTests -Dsurefire.failIfNoSpecifiedTests=false`，被 `application-starter` 既有测试代码中的旧 `application.*` 包引用阻塞；该问题不是本轮新增代码导致，未在本任务内修复。

## 变更审核清单自检

- 架构边界：通过。未让 `chat-domain` 依赖 `infrastructure-service/*-impl`，未改变生产模块职责。
- 依赖边界：通过。未新增 Maven 依赖。
- 配置边界：通过。未新增运行配置或占位配置。
- 注释规范：通过。新增测试 helper 和测试方法均有职责/契约说明。
- 测试规范：通过。新增测试类命名、方法命名、标签和断言均符合当前规范。
- 文档同步：通过。新增长期测试分层规则已写入 `docs/standards/测试规范.md`。
- 未完成项：真实环境测试本体需要在真实测试环境契约确认后按 `env-*` 标签继续补充。
