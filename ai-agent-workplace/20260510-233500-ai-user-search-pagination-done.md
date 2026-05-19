# 用户信息搜索分页任务单

## 任务目标
在现有用户信息 API 基础上补齐搜索与分页能力，保持与消息模块一致的 query/service/repository/database 分层风格。

## 影响模块
- `chat-domain`
- `infrastructure-service`
- `application-starter`
- `docs/`

## 允许修改范围
- 用户信息相关 controller / application service / query / dto / repository / support / test
- 用户资料持久化契约与数据库实现
- 用户信息 API 文档

## 禁止边界
- 不更改整体架构分层
- 不引入新外部依赖
- 不修改无关业务模块
- 不扩展到完整用户中心重构

## 依据文档
- `docs/架构文档.md`
- `docs/包结构规范.md`
- `docs/依赖引入规范.md`
- `docs/API.md`
- `docs/异常与错误码规范.md`
- `docs/测试规范.md`
- `docs/AI协作开发规范.md`
- `docs/变更审核清单.md`

## 验收标准
- 用户模块提供明确的搜索与分页入口
- 接口参数校验与现有 `200/300/404/500` 语义一致
- 数据库契约、仓储适配、应用服务、控制器链路完整
- 相关测试通过
- API 文档同步更新
