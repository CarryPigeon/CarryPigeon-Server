# 系统性测试质量整理任务单

## 任务类型

实现类任务。本任务以测试质量整理为目标，原则上只修改测试代码、测试依赖和 AI 工作目录任务单；不修改生产代码、正式配置、模块结构或项目规范文档。

## 任务目标

对当前测试进行一轮系统性整理：

- 扫描并修正典型弱契约测试。
- 扫描并清理典型模板化测试注释。
- 扫描 controller 协议测试中仅断言状态码、缺少 command/query 映射断言的关键写路径，并补充一批高价值断言。
- 运行与整理范围匹配的 Maven 测试。

## 受影响模块

优先只读扫描全仓库测试；实现优先落在：

- `chat-domain`
- `infrastructure-service/database-api`
- `infrastructure-service/*-api`
- 必要时触达其它测试模块

## 允许修改范围

- 各模块 `src/test/java`。
- 必要时仅为测试编译补充 test scope 测试依赖。
- `ai-agent-workplace/` 当前任务单。

## 禁止边界

- 不修改生产代码行为。
- 不新增业务功能。
- 不调整模块职责、包结构或依赖方向。
- 不引入新的架构规则。
- 不修改 `docs/`。
- 不做无关格式化或大面积机械重写。

## 依据文档

- `docs/测试规范.md`
- `docs/注释规范.md`
- `docs/架构文档.md`
- `docs/包结构规范.md`
- `docs/AI协作开发规范.md`
- `docs/变更审核清单.md`

## 执行计划

1. 全仓库扫描弱契约模式、模板化注释和 controller 映射断言缺口。
2. 按风险和收益选择一批改动点，避免无边界扩大。
3. 修正选定弱契约测试或补足断言。
4. 清理触达文件中的模板化注释。
5. 运行受影响模块测试；若范围覆盖 `chat-domain`，使用 `-am` 带依赖模块。
6. 记录结果、验证证据与残留风险，关闭任务单。

## 质量门禁

- 受影响测试命名仍符合 `methodName_condition_expected()`。
- 受影响测试类保留稳定 `@Tag`。
- 断言直接验证契约，不只验证调用过或非空。
- controller 关键写路径应验证当前账号、路径参数、请求体字段到 command/query 的映射。
- 触达文件不得保留明显模板化注释。
- 运行与改动范围匹配的 Maven 测试并记录结果。

## 验收标准

- 输出系统性扫描结果。
- 修复一批明确可落地的问题。
- 验证命令通过，或明确记录阻塞原因。
- 任务单关闭为 `done`。

## 执行结果

### 扫描结果

- 已扫描全测试目录中的典型弱契约和模板化注释短语：
  - `当前测试类覆盖对象`
  - `条件下满足`
  - `测试契约`
  - `defaultImplementation`
  - `overridingImplementation`
- 整理后扫描结果：无命中。

### 实际变更

- `chat-domain`
  - `AuthControllerTests`
    - 为邮箱验证码、会话创建、注册、登录、刷新、撤销等关键 HTTP 写路径补充 command 入参映射断言。
    - 校验客户端插件列表从请求体到 `ServerEntranceApi.findMissingRequiredPlugins` 的映射。
    - 清理模板化注释，改为描述具体协议契约。
    - 修正泛型 captor 的未检查编译警告。
  - `UserProfileControllerTests`
    - 为当前用户资料、按账号查询、批量用户查询、邮箱更新、资料更新等路径补充账号、查询参数、请求体字段映射断言。
    - 清理模板化注释，改为描述具体失败条件和协议边界。
- `infrastructure-service/database-api`
  - `MentionDatabaseServiceContractTests`
    - 将匿名弱替身改为记录型测试替身。
    - 覆盖写入、列表查询过滤参数、单条已读、批量已读的接口契约。
  - `ChannelPinDatabaseServiceContractTests`
    - 将单点写入测试扩展为查询、写入、删除、分页列表、计数契约测试。
    - 使用记录型测试替身验证接口入参和返回语义。

### 验证结果

- 已执行：`mvn -pl chat-domain -am test -DskipTests=false`
- 结果：通过。
- 覆盖模块：
  - `infrastructure-basic`
  - `infrastructure-service`
  - `database-api`
  - `storage-api`
  - `cache-api`
  - `mail-api`
  - `chat-domain`
- 测试汇总：302 tests, 0 failures, 0 errors, 0 skipped。

### 自检结论

- 未修改生产代码、正式配置、模块结构或 `docs/`。
- 未新增长期规则或架构规则。
- 本轮改动保持在测试代码和 AI 工作目录任务单范围内。
- 触达测试方法命名仍符合 `methodName_condition_expected()`。
- 触达测试类保留 `@Tag("contract")`。
- 关键断言从“只看状态码/非空”提升为验证协议字段映射和 database-api 接口契约。

### 残留风险

- 本轮是高价值点整理，不等同于全仓库所有 controller 和 repository 测试都已达到同一强度。
- 当前 controller 测试仍主要基于 standalone MockMvc，未覆盖完整 Spring 容器装配链路。
- database-api 契约测试验证接口语义，不验证具体 database-impl 的真实持久化行为。
