任务名称：ChannelController 构造器装配修复

任务目标：修复 Spring 启动时 `channelController` Bean 因多构造器无法明确选择而实例化失败的问题。

任务背景：用户本地启动报错 `No default constructor found`，源码中 `ChannelController` 存在五参生产构造器和四参测试兼容构造器，Spring 无法在多个未标注构造器之间确定注入入口。

影响模块：`chat-domain`

允许修改范围：仅允许修改 `ChannelController` 的构造器装配声明；允许按需运行相关测试。

禁止修改范围：不调整模块依赖方向，不新增依赖，不修改业务逻辑、HTTP 路由、DTO、配置或 SQL。

依赖限制：只能使用项目既有 Spring Boot 依赖。

配置限制：不新增配置项。

文档依据：`AGENTS.md`、`docs/architecture/架构文档.md`、`docs/architecture/包结构规范.md`、`docs/standards/注释规范.md`、`docs/standards/测试规范.md`、`docs/standards/变更审核清单.md`。

任务分解 / 执行计划：
1. 核对 `ChannelController` 构造器和现有测试调用。
2. 将生产装配构造器显式标记为 Spring 注入入口。
3. 运行 `chat-domain` 相关控制器测试或模块测试。
4. 按审核清单记录结果并关闭任务单。

关键假设与依赖：现有四参构造器仍被部分测试使用，保留它以降低改动面。

实现要求：保持控制器业务行为不变，仅修复 Bean 构造器选择。

测试要求：至少运行覆盖 `ChannelController` 的相关测试。

质量门禁：相关 Maven 测试通过，且不引入新的架构、依赖或配置变化。

复审要求：自检构造器选择、测试覆盖和边界规则。

文档要求：本次为局部启动缺陷修复，不新增长期项目规则，不修改 `docs/`。

验收标准：应用启动时 Spring 能通过五参构造器装配 `ChannelController`。

完成定义：代码修复已完成、测试已运行、任务单改名为 `done`。

实际结果：待填写。

验证记录：待填写。

残留风险：待填写。

知识沉淀 / 是否回写 docs：待填写。

产物清理与保留说明：保留任务单作为协作追踪材料。

补充说明：无。
