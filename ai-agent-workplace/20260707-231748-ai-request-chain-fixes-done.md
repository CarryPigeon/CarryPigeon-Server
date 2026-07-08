# 请求处理链问题修正任务单

## 任务名称

请求处理链问题修正

## 任务目标

根据 `ai-agent-workplace/20260707-224015-ai-request-chain-audit-done.md` 中确认的问题，修复请求链权限绕过、审批断链、入口校验、异常模型、文档不一致和 WS 入站协议测试缺口。

## 任务背景

上一轮逐调用链审查发现消息转发、频道申请审批、消息请求体校验、文件领域异常、撤回接口文档和 WS 入站命令文档存在已确认缺口。用户要求进行修正。

## 影响模块

- `chat-domain`
- `docs/`
- `ai-agent-workplace/`

## 允许修改范围

- 修复 `chat-domain` 中已确认的请求链问题。
- 补充或调整相关测试。
- 同步修正 `docs/API.md` 和 `docs/t/*` 中与本次问题直接相关的 API / WS 文档。
- 更新本任务单。

## 禁止修改范围

- 不调整 Maven 模块结构。
- 不新增第三方依赖。
- 不改变既有依赖方向。
- 不重写 feature 包结构。
- 不引入新的架构模式。
- 不修改数据库模型或 SQL。
- 不处理审查任务单外的新需求。

## 依赖限制

只使用现有 Spring Boot、JUnit 5、MockMvc、Netty 测试能力和仓库已有工具类。

## 配置限制

不新增运行时配置。

## 文档依据

- `AGENTS.md`
- `docs/AI协作开发规范.md`
- `docs/变更审核清单.md`
- `docs/任务单模板.md`
- `docs/架构文档.md`
- `docs/包结构规范.md`
- `docs/异常与错误码规范.md`
- `docs/测试规范.md`
- `ai-agent-workplace/20260707-224015-ai-request-chain-audit-done.md`

## 任务分解 / 执行计划

1. 修复消息转发目标频道发送治理校验，并补禁言转发失败测试。
2. 修复入群申请审批 approve 前封禁复核，并补申请后封禁再审批失败测试。
3. 修复消息编辑/转发请求体入口校验，并补 MockMvc 校验失败测试。
4. 处理撤回 HTTP 接口文档不一致：优先补齐 HTTP 入口并走现有生命周期领域 API。
5. 修复文件领域 `accountId` 正数校验异常类型。
6. 补充 WS 入站 `send_channel_message` 文档，修正 `docs/api` 死链引用。
7. 运行相关模块测试和依赖边界搜索。
8. 按变更审核清单自检并归档任务单。

## 关键假设与依赖

- 审查任务单中的问题均已由用户要求进入修复阶段。
- 撤回能力已有领域 API，因此补 HTTP 入口属于暴露既有能力，不新增业务规则。
- 本次不解决用户批量资料查询性能问题的数据库 API 扩展，避免扩大为数据库契约变更；先保留任务单风险。

## 实现要求

- 继续保持 `chat-domain` 按 feature first 组织。
- Controller 只做协议解析、鉴权主体读取和领域命令映射。
- 领域服务继续通过 repository port 和边界 port 完成业务规则。
- 业务校验使用 `ProblemException`，避免协议层 500。

## 测试要求

- 补充权限绕过失败测试。
- 补充审批封禁复核失败测试。
- 补充请求体入口校验测试。
- 补充撤回 HTTP 入口测试。
- 如 WS 文档涉及入站命令，补或调整现有 WS handler 测试。

## 质量门禁

- 相关 `chat-domain` 测试通过。
- 跨模块依赖边界搜索无新增越界。
- 文档与实际入口保持一致。
- 任务单记录实际结果和验证记录。

## 复审要求

重点复审权限校验、异常映射、对外 API 行为和模块边界。

## 文档要求

只修正本次问题涉及的 API / WS 文档，不新增长期架构规则。

## 验收标准

- 审查任务单中的高/中优先级问题已修复或明确降级说明。
- 相关测试覆盖成功/失败路径。
- 质量门禁运行并记录结果。
- 任务单归档为 `done`。

## 完成定义

代码、测试、文档修改完成，验证通过或记录明确阻塞，任务单归档。

## 实际结果

- 修复消息转发目标频道发送治理：转发目标频道改为 `requireSendableChannel(...)`，禁言成员无法通过转发绕过发送限制。
- 修复入群申请审批封禁复核：approve 分支在新增成员前重新检查当前 active ban。
- 修复消息编辑/转发请求体入口校验：为请求体增加 `@Valid` 与 `@NotNull`，缺字段返回统一校验错误。
- 补齐撤回 HTTP 入口：新增 `POST /api/channels/{channelId}/messages/{messageId}/recall`，复用现有 `ChannelMessageLifecycleApi.recallChannelMessage(...)`。
- 修复文件领域服务非法 accountId 异常类型：由 `IllegalArgumentException` 改为 `ProblemException.validationFailed(...)`。
- 同步 WS 入站命令测试替身与文档：`send_channel_message` 成功语义按 v1 `event/message.created` 表达。
- 修正文档 `docs/api` 死链为当前 `docs/t` 路径，并补充撤回接口与 `message.recalled` 事件说明。
- 补充相关领域、业务链路、Controller、文件服务、WS handler 测试。

## 验证记录

- 初次执行 `mvn -pl chat-domain -Dtest=ChannelDomainApiApplicationFlowTests,MessageBusinessChainTests,ChannelMessageQueryControllerTests,FileTransferDomainApiTests,RealtimeChannelHandlerMessageDispatchTests test` 失败：单模块无法解析 reactor 内依赖。
- 改用 `mvn -pl chat-domain -am -Dtest=ChannelDomainApiApplicationFlowTests,MessageBusinessChainTests,ChannelMessageQueryControllerTests,FileTransferDomainApiTests,RealtimeChannelHandlerMessageDispatchTests -Dsurefire.failIfNoSpecifiedTests=false test` 初次失败：新增封禁测试断言文案与实际错误消息不一致，已修正。
- 复跑 `mvn -pl chat-domain -am -Dtest=ChannelDomainApiApplicationFlowTests,MessageBusinessChainTests,ChannelMessageQueryControllerTests,FileTransferDomainApiTests,RealtimeChannelHandlerMessageDispatchTests -Dsurefire.failIfNoSpecifiedTests=false test` 通过：63 tests, 0 failures, 0 errors, 0 skipped。
- 执行 `mvn -pl application-starter -am -DskipTests compile` 通过，确认新增 `ChannelMessageController` 构造参数未破坏启动装配编译。
- 执行依赖边界搜索 `rg "infrastructure\\.service\\..*\\.impl|backend\\.starter|application\\.starter|team\\.carrypigeon\\.backend\\.starter" chat-domain/src/main/java chat-domain/src/test/java infrastructure-basic/src/main/java infrastructure-service/*-api/src/main/java -n`，无命中。
- 执行文档死链搜索 `rg "docs/api/" docs/t -n`，无命中。

## 残留风险

- 未运行全量 `mvn test`；本次运行了与修复范围匹配的 `chat-domain` 定向测试和 `application-starter` 编译门禁。
- 未启动真实 MySQL / Redis / MinIO / Netty 服务做端到端手工验证。
- 审查任务单中的低优先级“用户公开资料批量查询全表读取”本次未修复，因为需要扩展数据库 API 批量查询契约，超出本次最小修复范围。
- 当前工作区已有大量历史/用户未提交改动；本任务只在必要文件上增量修改，未回滚无关改动。

## 知识沉淀 / 是否回写 docs

- 本次未新增长期架构规则。
- 对外 API / WS 协议变化已回写到 `docs/API.md`、`docs/t/11-http-endpoints-v1.md`、`docs/t/12-ws-events-v1.md` 和相关 `docs/t` 链接。

## 产物清理与保留说明

本任务单作为追踪产物保留在 `ai-agent-workplace/`。
