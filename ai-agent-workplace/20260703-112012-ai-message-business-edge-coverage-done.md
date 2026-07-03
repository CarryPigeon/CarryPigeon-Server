# Message Business 边界场景继续覆盖任务单

## 任务类型

实现类任务。继续补充 message business 链路中除真实数据库外的边界业务场景。

## 任务目标

- 在既有 `MessageBusinessChainTests` 基础上继续补充非真实数据库场景。
- 保持真实 controller、真实领域 API、真实领域模型和内存/确定性替身。
- 不接入真实数据库、真实对象存储或真实实时网络。
- 不修改生产代码行为。

## 受影响模块

- `chat-domain`
- `ai-agent-workplace/` 当前任务单

## 允许修改范围

- `chat-domain/src/test/java`
- `ai-agent-workplace/` 当前任务单

## 禁止边界

- 不修改生产代码。
- 不新增依赖。
- 不改变模块结构、包结构或依赖方向。
- 不接真实 MySQL/MyBatis。
- 不处理与本任务无关的既有脏状态。

## 依据文档

- `docs/测试规范.md`
- `docs/注释规范.md`
- `docs/AI协作开发规范.md`
- `docs/变更审核清单.md`

## 拟补充场景

1. 搜索 sender/domain/before/after 过滤。
2. 历史分页 cursor 与 has_more。
3. 缺失认证上下文返回 unauthorized。
4. mention 标记不存在 ID 返回 404，批量已读 before 边界。
5. 附件上传非法 message_type、空文件校验。
6. 转发到非成员频道失败或转发不存在消息失败。
7. 取消不存在置顶失败。

## 验收标准

- 新增或扩展 business 测试覆盖上述主要边界。
- `mvn -pl chat-domain -am test -DskipTests=false -Dtest.groups=business` 通过。
- `mvn -pl chat-domain -am test -DskipTests=false` 通过。
- 任务单关闭为 `done`。

## 执行结果

- 继续扩展 `chat-domain/src/test/java/team/carrypigeon/backend/chat/domain/features/message/chain/MessageBusinessChainTests.java`。
- 新增 7 个 message business 边界场景：
  - 搜索 sender/domain/before/after 组合过滤。
  - 历史分页 cursor 与 has_more。
  - 缺失认证上下文返回 unauthorized。
  - mention 不存在 ID 标记已读返回 404，批量已读 before 边界。
  - 附件上传非法 message_type 和空文件校验。
  - 转发不存在源消息、转发到非成员目标频道失败。
  - 取消不存在置顶返回 not found。
- 为 fixture 补充未认证 MockMvc 和第二个私有频道，用于权限边界验证。
- 未接入真实数据库，未修改生产代码，未新增依赖。

## 验证记录

- 通过：`mvn -pl chat-domain -am test -DskipTests=false -Dtest=MessageBusinessChainTests -Dsurefire.failIfNoSpecifiedTests=false`
  - 结果：23 tests, 0 failures, 0 errors, 0 skipped。
- 通过：`mvn -pl chat-domain -am test -DskipTests=false -Dtest.groups=business`
  - 结果：38 tests, 0 failures, 0 errors, 0 skipped。
- 通过：`mvn -pl chat-domain -am test -DskipTests=false`
  - 结果：340 tests, 0 failures, 0 errors, 0 skipped。

## 自检结论

- 结构设计：仍保留在 `features/message/chain` 的 business 分包，和 mock/controller/domain/persistence 测试职责分离。
- 覆盖面：message 非 DB business 场景进一步覆盖到搜索组合过滤、分页、认证缺失、mention 已读边界、附件上传校验、转发失败和取消置顶失败。
- 注释和可读性：新增测试方法均说明验证契约，fixture 增量能力仅服务本轮场景。
- 剩余风险：仍不覆盖真实 MySQL/MyBatis、真实 MinIO、真实 WebSocket、真实 access token 贯穿 message controller 的端到端链路。
