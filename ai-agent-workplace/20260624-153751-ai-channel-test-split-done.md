任务名称：
chat-domain 频道应用服务测试拆分

任务目标：
将 `ChannelApplicationServiceTests` 按稳定业务主题拆分为多份更小的契约测试类，并抽取共享测试支撑，降低失败定位成本和维护复杂度，同时不改变测试覆盖语义。

任务背景：
上一轮测试质量审查已确认 `chat-domain` 中 [ChannelApplicationServiceTests.java](/mnt/d/workspace/items/CarryPigeon/Backend/chat-domain/src/test/java/team/carrypigeon/backend/chat/domain/features/channel/application/service/ChannelApplicationServiceTests.java:78) 规模过大，已偏离成熟项目测试可维护性标准。本轮继续推进整改，优先处理该大测试类拆分。

任务类型：
实现类任务

影响模块：
- `chat-domain`
- `ai-agent-workplace/`

允许修改范围：
- 允许拆分 `chat-domain` 中频道应用服务测试类
- 允许新增包内测试支撑类
- 允许调整测试注释、类名、文件组织与静态测试支撑结构

禁止修改范围：
- 不修改正式业务逻辑
- 不修改模块依赖方向
- 不新增第三方依赖
- 不顺带重构无关测试

依赖限制：
- 仅使用现有测试依赖和现有测试目录结构

配置限制：
- 不新增配置

文档依据：
- `AGENTS.md`
- `docs/AI协作开发规范.md`
- `docs/变更审核清单.md`
- `docs/任务单模板.md`
- `docs/测试规范.md`
- `docs/注释规范.md`

任务分解 / 执行计划：
1. 审查 `ChannelApplicationServiceTests` 的场景分布与共享替身结构。
2. 抽取 `TestContext`、内存仓储替身、固定时间/ID 辅助方法为包级测试支撑。
3. 按稳定主题拆分为多份测试类：
   - 访问 / 读取
   - 生命周期
   - 申请 / 邀请流
   - 治理操作
4. 删除原超大测试类或将其替换为拆分后的等价结构。
5. 执行本地可行验证并记录阻塞。

关键假设与依赖：
- 当前工作区没有其他并行修改 `ChannelApplicationServiceTests`，可安全替换该文件结构。
- 若 Maven 测试仍受依赖环境阻塞，本轮至少完成 `git diff --check` 和源码级自检。

实现要求：
- 拆分后的测试类必须保持 `@Tag("contract")`
- 每个测试类职责明确，避免重新堆成“第二个大类”
- 共享测试支撑应保持包内可见，不污染正式源码
- 测试命名、注释和断言语义保持稳定

测试要求：
- 本轮不新增业务场景，只做等价拆分与支撑整理
- 应尽可能运行受影响测试或至少完成可行的编译/格式自检

质量门禁：
- `ChannelApplicationServiceTests` 不再作为 1200+ 行混合类存在
- 拆分后测试文件职责边界清晰
- 无新增格式问题
- 验证命令或阻塞原因记录完整

复审要求：
- 重点自检共享测试支撑是否过度暴露、是否带来新的隐式耦合

文档要求：
- 默认不改 `docs/`

验收标准：
- 原大测试类已拆分
- 共享测试支撑已抽取
- 各拆分测试类可直接对应业务主题
- 任务单记录实际结果与验证证据

完成定义：
- 用户获得一组更易维护的频道应用服务测试结构，且本轮验证或阻塞信息清晰

实际结果：
- 已删除原 `ChannelApplicationServiceTests`
- 已新增包级测试支撑 `ChannelApplicationServiceTestSupport`
- 已按主题拆分为四个测试类：
  - `ChannelApplicationServiceAccessTests`
  - `ChannelApplicationServiceLifecycleTests`
  - `ChannelApplicationServiceApplicationFlowTests`
  - `ChannelApplicationServiceGovernanceTests`
- 各测试类已按原有场景等价迁移，未扩大业务测试语义

验证记录：
- `git diff --check -- chat-domain/src/test/java/team/carrypigeon/backend/chat/domain/features/channel/application/service ai-agent-workplace/20260624-153751-ai-channel-test-split-current.md`
  - 结果：通过，无新增空白与补丁格式问题
- `mvn -o -pl chat-domain -am -DskipTests test-compile`
  - 结果：失败
  - 原因：离线环境缺少 `infrastructure-basic` 所需依赖，如 `spring-boot-starter-json:3.5.3`、`spring-boot-starter-log4j2:3.5.3`、`lombok:1.18.38`、`hutool-core:5.8.36`
- `rg -n "class ChannelApplicationServiceTests" chat-domain/src/test/java/team/carrypigeon/backend/chat/domain/features/channel/application/service`
  - 结果：无命中，确认原超大测试类已移除
- `wc -l .../ChannelApplicationService*Tests.java .../ChannelApplicationServiceTestSupport.java`
  - 结果：
    - `ChannelApplicationServiceAccessTests.java`: 166 行
    - `ChannelApplicationServiceLifecycleTests.java`: 196 行
    - `ChannelApplicationServiceApplicationFlowTests.java`: 309 行
    - `ChannelApplicationServiceGovernanceTests.java`: 232 行
    - `ChannelApplicationServiceTestSupport.java`: 474 行

残留风险：
- 受 Maven 离线依赖缺失限制，未能完成 `chat-domain` 的真实编译验证
- 共享测试支撑仍较大，但已从业务场景类中抽离；若后续继续扩展，可再按 repository / publisher / fixture 细分

知识沉淀 / 是否回写 docs：
- 暂不回写

产物清理与保留说明：
- 保留本任务单作为本轮测试拆分记录

补充说明：
- 本轮优先以“降低维护复杂度”为目标，不扩大测试语义范围。
