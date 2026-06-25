任务名称：
database-impl 测试缺口补齐与 auth 应用服务测试拆分

任务目标：
补齐 `database-impl` 中 `Mention` 适配器测试的高信号缺口，并将 `chat-domain` 中过大的 `AuthApplicationServiceTests` 拆分为更清晰的主题测试类与共享测试支撑，提升失败定位效率和可维护性。

任务背景：
前几轮已完成 `database-api` 合同测试补齐、部分 `database-impl` 浅断言加深，以及 `ChannelApplicationServiceTests` 的结构拆分。当前仍有两个延续任务需要并行推进：一是 `Mention` 适配器测试仍缺少失败路径与写入映射断言；二是 `AuthApplicationServiceTests` 仍是单文件混合注册、登录、刷新、注销与全部替身结构的大类，未达到成熟项目测试结构标准。

任务类型：
实现类任务

影响模块：
- `chat-domain`
- `infrastructure-service/database-impl`
- `ai-agent-workplace/`

允许修改范围：
- 允许增强 `MybatisPlusMentionDatabaseServiceTests`
- 允许拆分 `AuthApplicationServiceTests`
- 允许新增包内测试支撑类与拆分后的测试类
- 允许删除被拆分替代的旧测试类
- 允许新增本轮任务单

禁止修改范围：
- 不修改正式业务逻辑
- 不修改模块依赖方向
- 不新增第三方依赖
- 不顺带重构无关测试或正式源码

依赖限制：
- 仅使用现有测试依赖

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
1. 为本轮双任务建立任务单并锁定边界。
2. 增强 `MybatisPlusMentionDatabaseServiceTests`，补齐插入映射断言与失败路径包装语义。
3. 抽取 `AuthApplicationService` 共享测试支撑，按注册、认证、令牌流拆分测试类。
4. 删除旧的超大测试类并执行可行验证。

关键假设与依赖：
- `AuthApplicationServiceTests` 当前无其他并行改动阻塞拆分替换。
- `Mention` 适配器实现的异常包装语义已稳定，测试补齐不会触发业务代码改动。
- Maven 真实执行仍可能受离线依赖阻塞，若无法完成模块测试，则至少完成 `git diff --check` 与结构级核验。

实现要求：
- 拆分后的 auth 测试类保留 `@Tag("contract")`
- 共享测试支撑保持包内可见，仅供当前测试包使用
- `Mention` 测试新增断言应直接对应 record/entity 映射与异常包装消息
- 不扩大业务测试语义范围

测试要求：
- `Mention` 至少覆盖写入映射、查询失败、单条已读失败、批量已读失败
- auth 测试拆分保持原有成功与失败场景，不额外引入新业务分支

质量门禁：
- `AuthApplicationServiceTests` 不再以单个超大类存在
- `Mention` 关键缺口已被高信号断言覆盖
- 无新增格式问题
- 验证命令或阻塞原因记录完整

复审要求：
- 自检共享支撑是否过度暴露
- 自检异常断言是否绑定实现无关细节过深

文档要求：
- 默认不改 `docs/`

验收标准：
- `Mention` 测试能识别字段映射与异常包装回归
- `AuthApplicationService` 测试已按稳定主题拆分
- 任务单记录实际结果与验证证据

完成定义：
- 两个延续测试治理任务都在本轮得到实质推进，且结构与验证信息清晰可复审

实际结果：
- 已增强 `MybatisPlusMentionDatabaseServiceTests`
  - 新增 `insert` 字段映射断言
  - 新增 `listByAccountId` 失败包装测试
  - 新增 `markAsRead` 失败包装测试
  - 新增 `markAllAsRead` 失败包装测试
- 已删除原 `AuthApplicationServiceTests`
- 已新增包级测试支撑 `AuthApplicationServiceTestSupport`
- 已按主题拆分为三个测试类：
  - `AuthApplicationServiceRegistrationTests`
  - `AuthApplicationServiceAuthenticationTests`
  - `AuthApplicationServiceTokenFlowTests`
- 原有注册、登录、刷新、注销场景已等价迁移，未扩大业务测试语义

验证记录：
- `git diff --check -- ai-agent-workplace/20260624-165909-ai-test-gap-and-auth-split-current.md chat-domain/src/test/java/team/carrypigeon/backend/chat/domain/features/auth/application/service infrastructure-service/database-impl/src/test/java/team/carrypigeon/backend/infrastructure/service/database/impl/mybatis/service/MybatisPlusMentionDatabaseServiceTests.java`
  - 结果：通过，无新增空白与补丁格式问题
- `rg -n "class AuthApplicationServiceTests" chat-domain/src/test/java/team/carrypigeon/backend/chat/domain/features/auth/application/service`
  - 结果：无命中，确认原超大测试类已移除
- `wc -l chat-domain/src/test/java/team/carrypigeon/backend/chat/domain/features/auth/application/service/AuthApplicationService*Tests.java chat-domain/src/test/java/team/carrypigeon/backend/chat/domain/features/auth/application/service/AuthApplicationServiceTestSupport.java`
  - 结果：
    - `AuthApplicationServiceAuthenticationTests.java`: 75 行
    - `AuthApplicationServiceRegistrationTests.java`: 128 行
    - `AuthApplicationServiceTokenFlowTests.java`: 117 行
    - `AuthApplicationServiceTestSupport.java`: 389 行
- `mvn -o -pl chat-domain -am -DskipTests test-compile`
  - 结果：失败
  - 原因：离线环境缺少 `spring-boot-starter-json:3.5.3`、`spring-boot-starter-log4j2:3.5.3`、`lombok:1.18.38`、`hutool-core:5.8.36`
- `mvn -o -pl infrastructure-service/database-impl -DskipTests test-compile`
  - 结果：失败
  - 原因：离线环境缺少 `database-api:1.0.0`、`infrastructure-basic:1.0.0`、`spring-boot-starter-jdbc:3.5.3`、`mybatis-plus-spring-boot3-starter:3.5.9`、`mysql-connector-j:9.2.0`、`lombok:1.18.38`

残留风险：
- Maven 离线依赖缺失仍阻塞 `chat-domain` 与 `database-impl` 的真实编译验证
- `AuthApplicationServiceTestSupport` 仍承载全部 auth 内存替身；若后续 auth 测试继续扩展，可再按 repository / token / transaction 支撑拆细

知识沉淀 / 是否回写 docs：
- 暂不回写

产物清理与保留说明：
- 保留本任务单作为本轮双任务整改记录

补充说明：
- 本轮只做测试质量治理，不触碰正式业务行为。
