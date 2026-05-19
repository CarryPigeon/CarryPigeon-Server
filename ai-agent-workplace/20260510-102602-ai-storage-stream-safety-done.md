# 任务单：storage 流安全优化

## 任务目标
- 修复对象存储读取/校验路径中不必要打开真实对象流的问题，降低资源泄漏与 IO 压力风险。

## 影响模块
- `infrastructure-service/storage-api`
- `infrastructure-service/storage-impl`
- `chat-domain/features/message`
- 相关测试模块

## 允许修改范围
- 仅限 storage 读取契约与 MinIO 实现、消息附件消费路径、相关测试与必要注释。

## 禁止边界
- 不调整模块职责与依赖方向
- 不改启动默认启用策略
- 不改健康检查语义
- 不引入新依赖
- 不修改无关业务流程

## Governing Docs
- `docs/架构文档.md`
- `docs/包结构规范.md`
- `docs/依赖引入规范.md`
- `docs/配置规范.md`
- `docs/注释规范.md`
- `docs/测试规范.md`
- `docs/异常与错误码规范.md`
- `docs/变更审核清单.md`
- `AGENTS.md`

## 优化方向
- 让消息附件的“存在性/元信息验证”不再强制打开对象内容流。
- 如果保留读取流能力，则明确流生命周期责任并用测试锁定。

## 验收标准
- 读取/验证路径不再无谓创建远程对象流。
- 相关契约与实现测试覆盖成功与失败路径。
- `lsp_diagnostics` 对改动文件无新增错误。
- 相关测试通过。
- 代码注释符合仓库规范。
