任务名称：
补充 swagger 交付说明

任务目标：
为团队补充 Swagger / OpenAPI 的访问方式、推荐联调流程和注意事项，让前端、测试与外部协作方更容易上手当前 API 门户。

任务背景：
当前 Swagger 已具备较完整的门户化能力，但仓库入口文档还缺少面向协作方的“怎么用”说明。用户明确要求继续，因此本任务补一段轻量但高价值的交付说明。

影响模块：
- `README.md`
- `docs/API.md`

允许修改范围：
- 允许在根 `README.md` 中新增 Swagger 入口说明
- 允许在 `docs/API.md` 中补充联调流程和使用注意事项

禁止修改范围：
- 不允许改变接口协议或启动方式
- 不允许新增无关文档文件

依赖限制：
- 不新增依赖

配置限制：
- 不新增配置

文档依据：
- `docs/API.md`
- `docs/部署手册.md`
- `AGENTS.md`

任务分解 / 执行计划：
1. 选择最适合团队查看的文档入口位置。
2. 写入 Swagger 访问方式、联调顺序和注意事项。
3. 运行最小验证，确保文档改动不引起回归。

关键假设与依赖：
- 当前 Swagger 入口与运行方式已稳定。

实现要求：
- 文案简短、可执行、面向协作方
- 不重复 API 细节表格，只补入口与流程

测试要求：
- 至少执行受影响 reactor 完整测试作为文档收尾验证

质量门禁：
- `mvn -pl "application-starter" -am -DskipTests=false test` 通过

复审要求：
- 最终输出应清楚说明这是一层交付说明增强，而非协议变更

文档要求：
- 文档内容应与 `docs/API.md` 和当前 OpenAPI 事实一致

验收标准：
- 团队能快速知道 Swagger 入口、如何认证、如何判断业务成功/失败

完成定义：
- 文档补充完成并验证通过
- 任务单改为 `done`

实际结果：
- 在根 `README.md` 中新增 Swagger / OpenAPI 访问入口与推荐联调流程
- 在 `docs/API.md` 中补充首次使用建议与推荐联调顺序
- 文案与当前 Swagger 门户事实保持一致，未新增协议层信息偏差

验证记录：
- `mvn -pl "application-starter" -am -DskipTests=false test`：通过
- 文档改动后再次执行 `mvn -pl "application-starter" -am -DskipTests=false test`：通过

残留风险：
- 当前 README 与 API 文档已给出入口和基本流程，但尚未扩展为面向外部团队的完整独立交付手册

知识沉淀 / 是否回写 docs：
- 已回写 `README.md` 与 `docs/API.md`

产物清理与保留说明：
- 当前任务单保留在 `ai-agent-workplace/`，状态已关闭为 `done`
