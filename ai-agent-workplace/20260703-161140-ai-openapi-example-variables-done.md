# OpenAPI 请求体示例变量化自检任务单

## 任务目标

复查 OpenAPI/Apifox 请求体示例是否接入导入说明中的环境变量，并修正仍写死的本地测试 ID，使 Apifox 导入后更适合按环境变量一键调试。

## affected modules

- `application-starter`
- `ai-agent-workplace`

## 允许修改范围

- `application-starter/src/main/java/team/carrypigeon/backend/starter/config/OpenApiConfiguration.java`
- `application-starter/src/test/java/team/carrypigeon/backend/starter/config/OpenApiConfigurationTests.java`
- 当前任务单文件

## 禁止边界

- 不修改业务 Controller、Service、Repository 行为。
- 不修改鉴权、数据库、Docker 或 `.env`。
- 不新增依赖。
- 不扩展架构规则。

## 依据文档

- `AGENTS.md`
- `docs/AI协作开发规范.md`
- `docs/变更审核清单.md`
- `docs/测试规范.md`

## 验收标准

- 请求体示例中的可变资源 ID 使用 Apifox 变量占位。
- `x-apifox-import-note` 明确列出相关变量名。
- 测试覆盖变量占位存在性。
- 相关 Maven 测试通过。
