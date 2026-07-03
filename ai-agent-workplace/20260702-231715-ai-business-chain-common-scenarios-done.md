# Business 链路常见场景补测任务单

## 任务类型

实现类任务。本任务在既有 `business` 测试分层基础上补齐认证与用户资料链路的常见成功、失败和边界场景。

## 任务目标

- 扩展 `AuthUserBusinessChainTests` 的常见场景覆盖。
- 保持测试仍为 `chat-domain` 内真实业务链路测试，不接入真实外部服务。
- 验证补充后的 `business` 标签命令和 `chat-domain` 全量测试。

## 受影响模块

- `chat-domain`
- `ai-agent-workplace/` 当前任务单

## 允许修改范围

- `chat-domain/src/test/java/team/carrypigeon/backend/chat/domain/features/auth/chain/AuthUserBusinessChainTests.java`
- `ai-agent-workplace/` 当前任务单

## 禁止边界

- 不修改生产代码行为。
- 不修改正式模块职责、依赖方向或对外协议。
- 不新增依赖或测试框架。
- 不让 `chat-domain` 测试依赖 `infrastructure-service/*-impl`。
- 不处理当前工作区中与本任务无关的既有脏状态。

## 依据文档

- `docs/测试规范.md`
- `docs/注释规范.md`
- `docs/AI协作开发规范.md`
- `docs/变更审核清单.md`

## 执行计划

1. 在现有 `business` 链路测试夹具上补充常见认证失败场景。
2. 补充 refresh/revoke 相关失败与状态场景。
3. 补充用户资料读取、批量读取、邮箱更新、背景上传等常见用户链路。
4. 运行 `business` 标签测试和 `chat-domain` 全量测试。
5. 记录结果并归档任务单。

## 质量门禁

- 新增测试方法命名符合 `methodName_condition_expected()`。
- 新增测试注释说明场景输入和期望。
- `business` 测试仍不连接真实外部服务。
- `mvn -pl chat-domain -am test -DskipTests=false -Dtest.groups=business` 通过。
- `mvn -pl chat-domain -am test -DskipTests=false` 通过。

## 验收标准

- 常见业务链路成功、失败和边界场景覆盖明显扩展。
- 验证命令通过，或明确记录不可通过原因。
- 任务单关闭为 `done`。

## 执行结果

### 实际变更

- 扩展 `AuthUserBusinessChainTests`。
- `business` 测试数量从 2 个增加到 15 个。
- 未修改生产代码、正式模块职责、依赖方向或对外协议。
- 未新增依赖或测试框架。
- 未让 `chat-domain` 测试依赖 `infrastructure-service/*-impl`。

### 覆盖场景

- 邮箱验证码会话成功创建账号、资料和默认频道成员关系。
- 邮箱验证码会话拒绝缺失 required plugin。
- 邮箱验证码错误返回参数校验失败，且不创建账户。
- 非 `email_code` 授权类型返回参数校验失败。
- 用户名密码注册、登录、refresh 轮换、revoke 撤销。
- 重复注册返回参数校验失败。
- 密码错误和账号不存在返回认证失败语义。
- 无效 refresh token 返回未认证。
- 已撤销 refresh session 不能再次刷新。
- 受保护用户资料接口拒绝缺失或非法 access token。
- 按账号读取和批量读取公开资料贯穿真实用户资料领域 API。
- 当前账号资料缺失返回 404。
- 更新邮箱成功时校验验证码并归一化邮箱。
- 更新邮箱时错误验证码和重复邮箱返回参数校验失败。
- 背景图上传经过认证并调用文件传输端口。

### 验证结果

- 已执行：`mvn -pl chat-domain -am test -DskipTests=false -Dtest.groups=business`
  - 结果：通过。
  - 汇总：15 tests, 0 failures, 0 errors, 0 skipped。
- 已执行：`mvn -pl chat-domain -am test -DskipTests=false`
  - 结果：通过。
  - 汇总：317 tests, 0 failures, 0 errors, 0 skipped。

### 自检结论

- 新增测试方法命名保持 `methodName_condition_expected()` 风格。
- 新增测试注释说明了验证的链路、输入条件和期望。
- 测试仍使用真实 controller、真实领域 API、真实认证上下文和内存/确定性替身。
- 未连接 MySQL、Redis、MinIO、SMTP 等真实外部服务。
- 当前工作区存在与本任务无关的既有 `ai-agent-workplace` 脏状态，本任务未处理这些无关变更。

### 残留风险

- 本轮覆盖的是认证与用户资料常见业务链路，不覆盖消息、频道、文件下载等其它 feature 的 `business` 链路。
- `business` 测试仍不验证真实数据库事务、Redis 缓存、对象存储或 SMTP 适配行为。
- Spring 容器完整装配链路仍依赖 `application-starter` 既有 smoke/config 测试，本轮没有新增 `@SpringBootTest`。
