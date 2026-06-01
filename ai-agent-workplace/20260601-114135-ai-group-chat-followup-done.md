任务名称：
群聊治理刷新提示补强

任务目标：
补齐 invite / accept invite 与申请列表刷新提示之间的一致性缺口，使群聊治理界面在 realtime hint 维度上保持完整。

任务背景：
当前 `ChannelInvite` 与 `ChannelApplication` 共用同一持久化记录和管理列表来源，但 invite 创建、invite 接受时只广播了成员列表或频道列表刷新，没有同步广播 `applications` 作用域提示，管理端申请/邀请列表可能无法及时感知变更。

影响模块：
- `chat-domain`
- `docs`（仅当需要补充当前代码事实）

允许修改范围：
- 允许修改 `ChannelApplicationService` 及相关测试
- 允许更新任务单

禁止修改范围：
- 不调整模块依赖
- 不新增第三方依赖
- 不改动无关 feature

文档依据：
- `docs/架构文档.md`
- `docs/API.md`
- `docs/t/12-ws-events-v1.md`
- `docs/测试规范.md`

任务分解 / 执行计划：
1. 校验 invite / application 当前共用读模型与 realtime 提示行为。
2. 为 invite 创建、invite 接受补 `channel.changed(scope=applications)` after-commit 广播。
3. 补充契约测试并跑定向验证。
4. 完成任务单归档。

验收标准：
- invite 创建会触发 `applications` 刷新 hint
- invite 接受会触发 `applications` 与 `members` 刷新 hint
- 定向测试通过

实际结果：
- 已为 `inviteChannelMember` 补齐 `channel.changed(scope=applications)` 的事务后广播。
- 已为 `acceptChannelInvite` 补齐 `channel.changed(scope=applications)` 的事务后广播，保持 invite / application 共用管理列表时的 refresh hint 完整性。
- 已补充对应的 `ChannelApplicationServiceTests` 断言。

验证记录：
- `cd /mnt/d/workspace/items/carrypigeon/backend && mvn -pl chat-domain -am -Dtest=ChannelApplicationServiceTests -Dsurefire.failIfNoSpecifiedTests=false test`
- 结果：`Tests run: 28, Failures: 0, Errors: 0, Skipped: 0`

残留风险：
- 当前 invite / application 管理仍共享同一底层记录模型，后续若协议层要显式区分“邀请列表”和“申请列表”，仍需要单独拆读模型或加状态语义。
