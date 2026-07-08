# OpenAPI API 覆盖面自检与补齐任务单

## 任务目标

核对当前 HTTP Controller 暴露的 API 与 OpenAPI/Apifox 示例覆盖面，补齐缺失的关键请求示例与覆盖验证，使 Apifox 导入后尽量减少手工整理。

##  affected modules

- `application-starter`
- `ai-agent-workplace`

## 允许修改范围

- `application-starter/src/main/java/team/carrypigeon/backend/starter/config/OpenApiConfiguration.java`
- `application-starter/src/test/java/team/carrypigeon/backend/starter/config/OpenApiConfigurationTests.java`
- 当前任务单文件

## 禁止边界

- 不修改业务 Controller、Service、Repository 行为。
- 不修改鉴权拦截规则。
- 不修改数据库 SQL、Docker、`.env`。
- 不新增依赖。
- 不引入新的架构约定。

## 依据文档

- `AGENTS.md`
- `docs/AI协作开发规范.md`
- `docs/变更审核清单.md`
- `docs/测试规范.md`
- `docs/注释规范.md`

## 验收标准

- 梳理当前 Controller API 数量与 OpenAPI 示例覆盖缺口。
- 对需要请求体的 JSON/multipart 写接口补充代表性示例。
- 增加或调整测试，验证示例覆盖面不低于当前维护清单。
- 执行相关 Maven 测试并通过。
