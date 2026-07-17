任务名称：

domain api 一接口一实现类规范调整

任务目标：

将 `chat-domain` 的 `domain/api` 实现规则调整为：一个 API 接口应对应一个明确命名的实现类，禁止一个实现类同时实现多个 API 接口，禁止在正式代码中使用匿名类作为 API 实现。

任务背景：

当前 `docs/architecture/包结构规范.md` 已规定 `domain/api` 用于向外暴露 domain 能力，但没有明确规定 API 接口与实现类之间的一一对应关系。现有实现中存在一个领域服务实现多个 API 接口、以及组合类实现多个 API 接口的情况，容易使对外能力边界和内部服务组合边界混在一起。

影响模块：

- `docs`
- `ai-agent-workplace`

允许修改范围：

- `docs/architecture/包结构规范.md`
- 当前任务单

禁止修改范围：

- 本任务不修改 Java 生产代码。
- 本任务不修改测试代码。
- 本任务不改变 HTTP/WS 协议、响应字段、错误码和业务行为。
- 本任务不新增依赖。
- 本任务不修改 Maven 模块结构。

拟定规则：

1. `domain/api` 中每个对外 API 接口应有且只有一个明确命名的实现类。
2. API 实现类可以委托一个或多个内部 `domain/service` 协作对象完成业务，但自身只实现一个 API 接口。
3. 一个 `*DomainService` 不应同时 `implements` 多个 `*Api`。
4. 正式代码不得使用匿名类、lambda 或临时组合对象作为 `domain/api` 实现。
5. 命名建议为 `<ApiNameWithoutApi>DomainApi`，例如 `AuthAccountApi` 对应 `AuthAccountDomainApi`。
6. 内部业务服务仍放在 `domain/service`，外部适配层仍优先依赖 `domain/api`。

验收标准：

- `docs/architecture/包结构规范.md` 明确写入一接口一实现类规则。
- 规范表达清楚 API 实现类和内部领域服务的关系。
- 明确本任务只调整长期规范，不直接进行代码落地。

实际结果：

- 已更新 `docs/architecture/包结构规范.md` 的 `domain` 层规则。
- 明确 `domain/api` 接口由一一对应的 API 实现类实现。
- 明确 API 实现类自身只实现一个 API 接口，不允许一个类同时 `implements` 多个 `*Api`。
- 明确 API 实现类可以委托多个内部 `domain/service` 协作对象，但不得把内部协作服务直接作为多个 API 的复合实现暴露。
- 明确正式代码不得使用匿名类、lambda 或临时组合对象作为 `domain/api` 实现。
- 增加 `*DomainApi` 命名建议，建议命名为 `<ApiNameWithoutApi>DomainApi`。

验证记录：

- 已复核 `docs/architecture/包结构规范.md` 中 `domain` 规则段落和类命名建议段落。
- 已通过 `rg` 确认新增规则关键词在规范中可检索。

残留风险：

- 当前任务只调整长期规范，没有同步改造 Java 生产代码。
- 现有代码中仍存在不满足新规范的实现形态，后续需要单独任务按该规范落地。

知识沉淀 / 是否回写 docs：

- 已回写正式文档：`docs/architecture/包结构规范.md`。

产物清理与保留说明：

- 本任务单归档为 `done` 后保留在 `ai-agent-workplace/`。
