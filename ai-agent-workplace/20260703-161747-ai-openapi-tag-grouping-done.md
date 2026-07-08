# OpenAPI 分组名称自检与修正任务单

## 任务目标

复查 OpenAPI/Apifox 分组名是否稳定，修正缺少类级 `@Tag` 导致导入后显示 `xxx-controller` 的 Controller。

## affected modules

- `chat-domain`
- `application-starter`
- `ai-agent-workplace`

## 允许修改范围

- `chat-domain/src/main/java/**/controller/http/*Controller.java` 中缺失的 OpenAPI `@Tag` 元数据
- `application-starter/src/test/java/team/carrypigeon/backend/starter/config/OpenApiConfigurationTests.java`
- 当前任务单文件

## 禁止边界

- 不修改 Controller 路由、入参、返回值和业务调用。
- 不修改鉴权、数据库、Docker 或运行配置。
- 不新增依赖。
- 不引入新的架构规则。

## 依据文档

- `AGENTS.md`
- `docs/AI协作开发规范.md`
- `docs/变更审核清单.md`
- `docs/测试规范.md`
- `docs/注释规范.md`

## 验收标准

- 当前 HTTP Controller 均有稳定类级 `@Tag`。
- 不再依赖 springdoc 默认 controller tag 作为 Apifox 分组名。
- 相关 Maven 测试通过。
