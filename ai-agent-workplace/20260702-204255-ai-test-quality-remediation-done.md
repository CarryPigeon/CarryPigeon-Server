# 测试质量优化任务单

## 任务类型

实现类任务。本任务只修改测试代码与 AI 工作目录任务单，不修改生产代码、正式配置、模块结构或项目规范文档。

## 任务目标

根据测试方法质量审查结论，优化当前测试质量：

- 修正无效或弱价值的 `database-api` 契约测试。
- 为 `mail-api` 补充最小 API 契约测试。
- 为代表性 controller 测试补充命令/查询映射断言。
- 清理触达测试文件中的模板化注释，改为说明真实契约。

## 受影响模块

- `chat-domain`
- `infrastructure-service/database-api`
- `infrastructure-service/mail-api`

## 允许修改范围

- 上述模块的 `src/test/java`。
- 必要时仅为测试编译补充对应模块已有基线测试依赖，必须先确认当前 POM。
- `ai-agent-workplace/` 当前任务单。

## 禁止边界

- 不修改正式生产代码。
- 不新增业务行为。
- 不调整模块结构或依赖方向。
- 不引入新的架构规则。
- 不修改 `docs/`。

## 依据文档

- `docs/测试规范.md`
- `docs/注释规范.md`
- `docs/架构文档.md`
- `docs/包结构规范.md`
- `docs/AI协作开发规范.md`
- `docs/变更审核清单.md`

## 执行计划

1. 读取目标测试、接口与模块 POM，确认当前测试依赖和现有模式。
2. 修正 `database-api` 中把测试替身行为误当接口契约的测试。
3. 给 `mail-api` 添加最小模型/接口契约测试。
4. 给代表性 controller 测试补充 command/query 映射断言。
5. 清理触达文件中的模板化注释。
6. 运行受影响模块测试。
7. 记录实际结果、验证证据与残留风险，关闭任务单。

## 质量门禁

- 受影响测试命名仍符合 `methodName_condition_expected()`。
- 测试类仍具备稳定 `@Tag` 分级。
- 失败断言验证失败语义，而不是只验证抛异常。
- controller 协议测试至少验证关键路径参数映射。
- 运行与改动范围匹配的 Maven 测试命令。

## 验收标准

- 无效契约测试被删除、改正或替换为真实契约断言。
- `mail-api` 有最小测试覆盖。
- 代表性 controller 测试可捕获路由/认证/请求体到 command 的映射错误。
- 测试通过或明确记录阻塞原因。

## 实际变更

- 修正 `ChannelReadStateDatabaseServiceContractTests`：
  - 删除把测试替身抛错误判为接口默认契约的断言。
  - 改为验证查询、写入和未读列表的接口输入输出语义。
- 修正 `NotificationPreferenceDatabaseServiceContractTests`：
  - 拆分服务端级查询、频道级列表和 upsert 写入契约。
  - 使用记录型测试替身验证接口参数和记录传递。
- 新增 `mail-api` 测试：
  - `MailSendCommandTests` 覆盖合法命令和空白收件人/标题/正文失败语义。
  - `MailSenderServiceContractTests` 覆盖发送服务抽象接收完整命令。
  - `mail-api/pom.xml` 增加 test scope 的 `spring-boot-starter-test`，与其它 api 模块测试依赖模式一致。
- 优化 `ChannelControllerTests`：
  - 为创建、删除、更新、成员列表、管理员提升/撤销、踢人、封禁、解封、通知偏好更新补充 `ArgumentCaptor` 映射断言。
  - 清理触达方法中的模板化测试注释，改为说明具体协议契约。

## 验证记录

- `mvn -pl infrastructure-service/database-api,infrastructure-service/mail-api,chat-domain test -DskipTests=false`
  - `database-api`: 45 tests passed。
  - `mail-api`: 5 tests passed。
  - `chat-domain`: 未执行到测试，因未带 `-am` 导致本地 reactor 依赖解析失败。
- `mvn -pl chat-domain -am test -DskipTests=false`
  - 构建成功。
  - `infrastructure-basic`: 28 tests passed。
  - `database-api`: 45 tests passed。
  - `storage-api`: 2 tests passed。
  - `cache-api`: 1 test passed。
  - `mail-api`: 5 tests passed。
  - `chat-domain`: 302 tests passed。

## 自检结论

- 本次未修改生产代码、正式配置、模块结构或 `docs/`。
- `mail-api` 仅新增 test scope 测试依赖，不影响生产依赖方向。
- 受影响测试类均保留稳定 `@Tag`。
- 触达文件中未继续保留审查指出的模板化短语。

## 残留风险

- 当前工作树存在大量既有未提交/未跟踪/删除状态，本次只在当前工作区基础上做增量优化，未回退既有重构改动。
- 只优化了审查中最典型的问题点，未系统清理全仓库所有弱契约或模板化注释。
