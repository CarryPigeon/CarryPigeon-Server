任务名称：

CarryPigeon PRD 收敛与正式入库

任务目标：

将当前分散在 `ai-agent-workplace/` 中的 CarryPigeon PRD 草案收敛为一份正式、稳定、可长期维护的产品需求文档，并在完成收敛后写入 `docs/`。

任务背景：

当前已经存在产品总 PRD、MVP 范围 PRD、聊天内核模型 PRD、插件系统 PRD、多服务端协议边界 PRD等多份草案。这些草案已覆盖大部分方向，但仍存在重复内容、旧表述、待确认项未同步、以及尚未进入 `docs/` 的问题。用户已明确要求继续收敛 PRD，直到文档完善后正式拉入 `docs/`，而不是直接进入 `auth` 实现。

影响模块：

- `ai-agent-workplace`
- `docs`

允许修改范围：

- 新增或更新 `ai-agent-workplace/` 下的任务单与收敛记录
- 新增或更新 `docs/` 下的正式 PRD 文档
- 可以对已确认产品决策做统一收敛与去重

禁止修改范围：

- 不修改正式 Java 源码
- 不修改模块依赖
- 不新增第三方依赖
- 不在本轮进入具体功能实现
- 不把未确认的新长期规则混入项目规范文档

依赖限制：

- 本轮仅做文档收敛，不引入任何技术依赖

配置限制：

- 本轮只写已经明确需要的产品与边界要求
- 不新增未来占位配置

文档依据：

- `docs/AI协作开发规范.md`
- `docs/变更审核清单.md`
- `docs/任务单模板.md`
- `ai-agent-workplace/20260420-164627-ai-carrypigeon-prd-current.md`
- `ai-agent-workplace/20260420-170413-ai-mvp-scope-prd-current.md`
- `ai-agent-workplace/20260420-170413-ai-chat-core-model-prd-current.md`
- `ai-agent-workplace/20260420-170413-ai-plugin-system-prd-current.md`
- `ai-agent-workplace/20260420-170413-ai-client-multi-server-prd-current.md`
- 用户已在对话中确认的补充决策

实现要求：

- 统一服务端范围与非范围
- 统一 V0、V1 的能力与非能力
- 统一认证、消息模型、插件边界、多服务端协议边界
- 删除重复和冲突表述
- 把用户已拍板事项写成正式结论，不再保留为待确认问题

测试要求：

- 本轮不新增代码测试
- 文档需自带清晰验收边界，便于后续实现任务拆解

文档要求：

- AI 中间过程放在 `ai-agent-workplace/`
- 正式 PRD 写入 `docs/`
- 若本轮任务完成，任务单从 `current` 改为 `done`

验收标准：

- `docs/` 中存在正式 CarryPigeon PRD 文档
- 文档内容覆盖项目定位、目标用户、典型场景、范围边界、版本规划、认证与消息边界、插件边界、多服务端协议边界
- 文档与用户已确认决策一致
- 原草案中的主要冲突项被消除

补充说明：

- 用户已确认：token 采用 JWT；refresh 周期由 AI 给出专业建议；密码方案由 AI 选择专业方案；V1 文件存储可直接采用 MinIO；插件功能后置，本阶段只留接口；匿名公开源信息接受限流。

## 实际结果

- 已将分散在 `ai-agent-workplace/` 的产品总 PRD、MVP、聊天内核、插件系统、多服务端协议边界草案收敛为单份正式 PRD。
- 已在 `docs/产品需求文档.md` 中写入正式版本。
- 已同步用户最新拍板结论：JWT、`access token + refresh token`、建议 refresh 周期 14 天、文件消息 V1 采用 MinIO、匿名公开源信息限流、插件能力当前仅保留接口和边界。
- 已清理主 PRD 中的旧范围表达，如私聊、客户端能力混入当前服务端交付目标等问题。

## 实际影响文件

- `docs/产品需求文档.md`
- `ai-agent-workplace/20260420-192959-ai-prd-consolidation-done.md`

## 自检结论

- 已明确本次变更目标、影响范围和禁止边界。
- 已遵守 AI 协作开发规范：先在 `ai-agent-workplace/` 建任务单，再将正式长期文档写入 `docs/`。
- 本轮未修改任何正式源码、模块依赖或配置。
- 正式 PRD 已覆盖项目定位、目标用户、场景、MVP 范围、版本规划、聊天模型、JWT 鉴权、插件边界、多服务端协议边界和文件存储策略。
- 文档中的插件范围已经收敛为“当前只留接口，不把具体插件功能纳入近期必做范围”。

## 残留风险与未完成项

- `ai-agent-workplace/` 中仍保留多份历史 PRD 草案，当前作为溯源材料可保留，但它们不应再被视为正式依据。
- 若后续要以正式文档为唯一依据，建议在下一轮把历史草案加上“已被正式 PRD 替代”的说明，避免误用。
