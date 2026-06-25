任务名称：
chat-domain 命令侧应用服务拆分优化

任务目标：
在上一轮 query/shared 边界治理基础上，继续提升 chat-domain 的可读性与可拓展性，重点拆分 `ChannelApplicationService` 与 `MessageApplicationService` 的命令侧职责，降低超大 facade 的复杂度。

任务背景：
上一轮优化已完成共享认证契约抽离、message plugin catalog 边界收敛，以及 channel/message 查询职责拆分。当前剩余主要结构压力集中在两个超大应用服务的命令侧编排，已经成为后续扩展的主要阻力。

影响模块：
- chat-domain
- ai-agent-workplace

允许修改范围：
- 允许修改 chat-domain 主代码与测试代码
- 允许新增命令侧应用服务、测试支持类与兼容 facade 转发
- 允许在 ai-agent-workplace 记录任务单

禁止修改范围：
- 不修改 Maven 模块依赖方向
- 不修改 docs 作为本轮主目标
- 不引入新第三方依赖
- 不扩大到 application-starter / infrastructure-* 主体重构

文档依据：
- docs/架构文档.md
- docs/包结构规范.md
- docs/AI协作开发规范.md
- docs/变更审核清单.md
- docs/测试规范.md

任务分解 / 执行计划：
1. 识别 Channel / Message 大服务中仍然混杂的命令职责簇。
2. 按业务能力拆出独立命令侧应用服务，优先选择测试已按能力分组的区域。
3. 保留原 facade 作为兼容入口，仅做委派，避免控制器和测试装配面爆炸。
4. 调整测试支持代码与受影响用例。
5. 运行受影响测试并执行 chat-domain 离线回归。
6. 更新任务单结果并归档。

实现要求：
- 拆分后的服务名称必须直接表达业务能力，而不是抽象的 helper / manager。
- facade 应只负责转发，不继续堆积业务编排。
- 优先减少“新需求继续堆进大类”的诱因。

测试要求：
- 至少运行受影响的 channel/message 应用服务测试。
- 最终运行 chat-domain 离线 Maven 回归并记录结果。

验收标准：
- `ChannelApplicationService` 与 `MessageApplicationService` 的命令侧职责进一步下降。
- 新服务具备清晰业务命名和稳定边界。
- 受影响测试通过。

实际结果：
- 已完成 `MessageApplicationService` 命令侧拆分收尾，新增：
  - `AbstractMessageApplicationSupport`
  - `MessageDeliveryApplicationService`
  - `MessageModerationApplicationService`
  - `MessageApplicationService` 兼容 facade
- 已完成 `ChannelApplicationService` 命令侧拆分，新增：
  - `AbstractChannelApplicationSupport`
  - `ChannelAccessApplicationService`
  - `ChannelLifecycleApplicationService`
  - `ChannelApplicationFlowApplicationService`
  - `ChannelGovernanceApplicationService`
  - `ChannelApplicationService` 兼容 facade
- `ChannelApplicationService` 从超大单类编排收敛为稳定入口，生命周期、访问、申请流、治理职责下沉到独立能力服务。
- 保留了原有手工构造入口，避免 controller、realtime 和测试装配面爆炸。

验证记录：
- `mvn -o -Dmaven.repo.local=/tmp/chat-domain-m2/repository -pl chat-domain -am -DskipTests compile`
  - 结果：`BUILD SUCCESS`
- `mvn -o -Dmaven.repo.local=/tmp/chat-domain-m2/repository -pl chat-domain -am -Dtest=MessageApplicationServiceSendTests,MessageApplicationServiceForwardTests,MessageApplicationServicePinsTests,MessageApplicationServiceQueryTests,RealtimeChannelHandlerLifecycleTests,RealtimeChannelHandlerMessageDispatchTests -Dsurefire.failIfNoSpecifiedTests=false test`
  - 结果：`BUILD SUCCESS`
  - 受影响用例：41 通过，0 失败
- `mvn -o -Dmaven.repo.local=/tmp/chat-domain-m2/repository -pl chat-domain -am test -DskipTests=false`
  - 结果：`BUILD SUCCESS`
  - `chat-domain`：343 通过，0 失败

残留风险：
- `ChannelApplicationService` 虽然已经收敛为 facade，但由于兼容手工构造与显式委派，仍有约 300 行体量；后续若统一装配入口，可继续压缩。
- `ChannelQueryApplicationService` 与 `MessageQueryApplicationService` 仍各自持有一部分共享投影/校验模式，若后续继续优化，可评估是否抽出更稳定的读侧共享支撑，但当前不建议过早抽象。
