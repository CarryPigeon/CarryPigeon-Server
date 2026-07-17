# Business 链路继续覆盖任务单

## 任务类型

实现类任务。本任务在既有 `business` 测试分层基础上继续补齐其它核心 feature 的真实业务链路测试。

## 任务目标

- 选择一条认证/用户资料之外的核心业务链路进行 `business` 覆盖。
- 使用真实 controller、真实领域 API、真实领域模型和内存/确定性替身。
- 不接入真实外部服务，不修改生产代码行为。
- 运行 `business` 标签测试和 `chat-domain` 全量测试。

## 受影响模块

- `chat-domain`
- `ai-agent-workplace/` 当前任务单

## 允许修改范围

- `chat-domain/src/test/java`
- `ai-agent-workplace/` 当前任务单

## 禁止边界

- 不修改生产代码行为。
- 不调整模块职责、包结构规范或依赖方向。
- 不新增依赖或测试框架。
- 不让 `chat-domain` 测试依赖 `infrastructure-service/*-impl`。
- 不处理当前工作区中与本任务无关的既有脏状态。

## 依据文档

- `docs/standards/测试规范.md`
- `docs/standards/注释规范.md`
- `docs/standards/AI协作开发规范.md`
- `docs/standards/变更审核清单.md`

## 执行计划

1. 快速阅读频道/消息 controller、领域 API 和现有测试，选择收益最高的业务链路。
2. 新增或扩展 `features/*/chain/*BusinessChainTests`。
3. 覆盖常见成功、失败和边界场景。
4. 运行 `business` 标签测试和 `chat-domain` 全量测试。
5. 记录结果并归档任务单。

## 质量门禁

- 新增测试类命名符合 `<Name>Tests`。
- 新增测试方法命名符合 `methodName_condition_expected()`。
- 新增测试注释说明链路、边界和限制。
- `business` 测试仍不连接真实外部服务。
- `mvn -pl chat-domain -am test -DskipTests=false -Dtest.groups=business` 通过。
- `mvn -pl chat-domain -am test -DskipTests=false` 通过。

## 验收标准

- 至少补齐一条认证/用户资料之外的核心业务链路。
- 验证命令通过，或明确记录不可通过原因。
- 任务单关闭为 `done`。

## 执行结果

- 新增 `chat-domain/src/test/java/team/carrypigeon/backend/chat/domain/features/message/chain/MessageBusinessChainTests.java`。
- 覆盖 message 真实业务链路：发送文本、历史读取、搜索、提及 inbox 与已读、编辑、删除、附件上传后发送文件消息、置顶/取消置顶、非成员访问拒绝、协议校验错误、编辑版本冲突、置顶权限与消息不存在。
- 测试使用真实 controller、真实领域 API、真实频道边界适配器、真实消息插件与响应映射器；MySQL、Redis、MinIO、WebSocket 以确定性内存替身隔离。
- 未修改生产代码、模块结构、依赖方向或长期项目规则。

## 验证记录

- 通过：`mvn -pl chat-domain -am test -DskipTests=false -Dtest.groups=business`
  - 结果：23 tests, 0 failures, 0 errors, 0 skipped。
- 通过：`mvn -pl chat-domain -am test -DskipTests=false`
  - 结果：325 tests, 0 failures, 0 errors, 0 skipped。
- 通过：`mvn -pl chat-domain -am test -DskipTests=false -Dtest=MessageBusinessChainTests -Dsurefire.failIfNoSpecifiedTests=false`
  - 结果：8 tests, 0 failures, 0 errors, 0 skipped。
- 命令选择失误记录：
  - `mvn -pl chat-domain -am test -DskipTests=false -Dtest=MessageBusinessChainTests` 因前置模块无该测试类且未设置 `surefire.failIfNoSpecifiedTests=false` 失败。
  - `mvn -pl chat-domain test -DskipTests=false -Dtest=MessageBusinessChainTests` 因不带 `-am` 且本地 reactor 依赖未安装导致依赖解析失败。

## 自检结论

- 结构设计：新增 `features/message/chain` 分包承载真实业务链路测试，与既有 mock/controller/domain 测试分层区分清楚。
- 覆盖面：本轮补足 message 常见成功、失败和边界场景，覆盖发送到查询、提及、附件、编辑删除、置顶治理的主要链路正确性。
- 注释和可读性：测试类、测试方法和 fixture 均按测试规范说明验证契约、边界和外部替身限制。
- 未完成风险：当前 business 链路仍不连接真实数据库、真实对象存储或真实实时网络通道；这些由 persistence/contract/mock 单测和后续集成测试补位。
