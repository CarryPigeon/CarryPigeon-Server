任务名称：
正式部署手册编写

任务目标：
将当前项目的 thin-jar 分发体系与 Docker 外部依赖体系整理为正式部署手册写入 `docs/`，为仓库使用者提供稳定、统一、可执行的部署与启动说明。

任务背景：
当前仓库已经完成 thin-jar 分发改造，并补齐了项目级 `bin/` 命令层与 Docker 外部依赖脚本。用户明确要求把这套体系整理成正式部署手册，并同时介绍当前 Docker 体系。

影响模块：
- `docs`
- `readme.md`
- `ai-agent-workplace`

允许修改范围：
- 新增或更新 `docs/` 中的正式部署手册
- 更新 `readme.md` / `docs/Docker配置.md` 中的文档入口引用
- 在 `ai-agent-workplace/` 中记录任务单

禁止修改范围：
- 不修改业务代码
- 不修改分发与 Docker 脚本行为
- 不扩大为容器化应用改造

依赖限制：
- 不引入任何新依赖
- 仅基于现有 distribution / Docker / bin 脚本与验证结果整理文档

配置限制：
- 不新增运行时配置
- 文档只描述当前真实存在的配置项和命令

文档依据：
- `AGENTS.md`
- `docs/Docker配置.md`
- `distribution/README.md`
- `readme.md`
- `ai-agent-workplace/20260425-231330-ai-remediation-implementation-done.md`
- `ai-agent-workplace/20260426-104048-ai-thin-jar-distribution-task-done.md`

任务分解 / 执行计划：
1. 确认现有 Docker / distribution / bin 脚本与文档边界。
2. 新建正式部署手册，覆盖：
   - 当前 Docker 体系说明
   - thin-jar 分发产物结构
   - 分发打包命令
   - 前台启动、后台启动、停止命令
   - Docker 外部依赖启动/停止/重置/日志命令
   - 适用边界与注意事项
3. 为现有 README / Docker 文档补充正式手册入口。
4. 做文档级自检并进行 Oracle 复审。

关键假设与依赖：
- 当前 thin-jar 与 Docker 脚本体系已经稳定，可作为正式文档基线。
- 手册应描述“当前体系”，而不是设计未来容器化方案。

实现要求：
- 文档必须足够让新使用者按顺序完成打包、启动依赖、启动应用、停止应用。
- 明确区分“外部依赖由 Docker 提供”和“应用本身不在当前阶段容器化”。
- 命令必须与现有脚本一致。

测试要求：
- 本次为文档任务，不新增测试
- 以已通过的命令验证记录作为文档真实性依据

质量门禁：
- 文档内容与实际脚本/产物/命令一致
- 不出现未来规划冒充当前能力
- 通过 Oracle 复审

复审要求：
- 需进行 Oracle 文档复审，检查是否清晰、准确、无关键遗漏

文档要求：
- 正式手册进入 `docs/`
- 任务级追溯留在 `ai-agent-workplace/`

验收标准：
- `docs/` 中存在一份正式部署手册
- 文档能清楚解释当前 Docker 体系与 thin-jar 分发模式
- 用户可直接按文档执行关键命令

完成定义：
- 手册编写完成
- 引用入口补齐
- 任务单改为 `done`

实际结果：
- 已在 `docs/` 下新增正式部署手册 `docs/部署手册.md`。
- 手册已覆盖当前 Docker 体系说明、thin-jar 分发结构、分发打包命令、前台启动、后台启动、停止命令、Docker 外部依赖命令以及边界与注意事项。
- 已更新 `readme.md` 与 `docs/Docker配置.md`，补充对部署手册的入口引用。
- 已根据 Oracle 复审修正文档中的路径上下文问题，使 repo 根目录包装命令与 distribution 目录下的 PID/stdout 路径描述保持一致。

验证记录：
- 文档内容以当前已存在且已验证通过的脚本、分发目录与 Maven 命令为依据编写。
- Oracle 文档复审已完成，并确认此前唯一阻塞问题（repo-root wrapper 与 distribution-root 路径混用）已修复。
- Oracle 跟进复审通过：当前范围内无新的阻塞问题，仅保留路径上下文在后续维护时继续保持一致的非阻塞提醒。

残留风险：
- `distribution/README.md` 仍同时描述“仓库根目录包装命令”与“distribution 包内脚本命令”两种上下文，后续维护时需继续保持区分。
- 仍未在当前环境中执行真实 Docker CLI 运行验证，因此 Docker 相关命令文档继续以已有脚本和静态验证为基础。

知识沉淀 / 是否回写 docs：
- 本任务即为回写 `docs/`

产物清理与保留说明：
- 任务单已改为 `done`
- 暂保留在 `ai-agent-workplace/` 根目录，便于当前阶段追溯；后续可按规则归档
