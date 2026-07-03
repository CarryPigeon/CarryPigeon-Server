# Message Business 额外场景覆盖任务单

## 任务类型

实现类任务。继续补充 message business 链路中除真实数据库外的常见业务场景。

## 任务目标

- 在既有 `MessageBusinessChainTests` 基础上补充更多真实业务链路场景。
- 继续使用真实 controller、真实领域 API、真实领域模型和内存/确定性替身。
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

1. 转发消息链路。
2. voice 消息链路。
3. 历史 around_mid / before / after 查询。
4. mention unread_only / cid / cursor 过滤。
5. 禁言成员发送失败。
6. 普通成员删除他人消息失败，owner 删除他人消息成功。
7. 附件 object_key 越权和对象不存在失败。
8. 置顶数量上限或重复置顶行为。

## 验收标准

- 新增或扩展 business 测试覆盖上述高价值场景中的主要项。
- `mvn -pl chat-domain -am test -DskipTests=false -Dtest.groups=business` 通过。
- `mvn -pl chat-domain -am test -DskipTests=false` 通过。
- 任务单关闭为 `done`。

## 执行结果

- 扩展 `chat-domain/src/test/java/team/carrypigeon/backend/chat/domain/features/message/chain/MessageBusinessChainTests.java`。
- 新增 8 个 message business 场景：
  - 转发消息生成新消息并保留源消息摘要。
  - voice 附件上传后发送语音消息并返回 share_key、下载路径和语音元数据。
  - 历史消息 `around_mid` + `before` + `after` 邻近窗口。
  - mention inbox 的 unread、频道、游标过滤组合。
  - 禁言成员发送消息失败。
  - 普通成员删除他人消息失败，owner 删除成员消息成功。
  - 附件 object_key 越权和对象不存在失败。
  - 重复置顶覆盖备注，置顶数量上限返回校验错误。
- 为 fixture 补充 voice 插件注册、mention 发送辅助方法、成员更新能力。
- 未接入真实数据库，未修改生产代码，未新增依赖。

## 验证记录

- 通过：`mvn -pl chat-domain -am test -DskipTests=false -Dtest=MessageBusinessChainTests -Dsurefire.failIfNoSpecifiedTests=false`
  - 结果：16 tests, 0 failures, 0 errors, 0 skipped。
- 通过：`mvn -pl chat-domain -am test -DskipTests=false -Dtest.groups=business`
  - 结果：31 tests, 0 failures, 0 errors, 0 skipped。
- 通过：`mvn -pl chat-domain -am test -DskipTests=false`
  - 结果：333 tests, 0 failures, 0 errors, 0 skipped。

## 自检结论

- 结构设计：仍集中在 `features/message/chain` 的真实业务链路测试分包内，不混入 mock/controller/domain 单测。
- 覆盖面：除真实数据库外，message 常见业务场景已覆盖发送、查询、搜索、转发、文本/文件/语音、提及、编辑删除、置顶治理、权限和附件边界。
- 注释和可读性：新增测试方法均有契约说明，测试 fixture 只补本轮必要能力。
- 剩余风险：仍未验证真实 MySQL/MyBatis 落库、真实 MinIO、真实 WebSocket 网络分发和真实 access token 到 message controller 的跨 feature 链路。
