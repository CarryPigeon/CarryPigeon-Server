# CarryPigeon Backend 文档导航

> 本目录采用“单一事实来源（SSOT）”原则：
> 对外协议以 `doc/api/*` 为准，其他文档只做实现说明与角色化指引。

## 1. 推荐阅读路径

- 产品与验收范围：`doc/PRD.md`
- 对外协议主入口：`doc/api/文档索引.md`
- 后端开发入口：`doc/audience/backend-developer-guide.md`
- 客户端开发入口：`doc/audience/client-developer-guide.md`
- 插件开发入口：`doc/audience/plugin-developer-guide.md`

## 2. 目录分工（标准）

- `doc/api/*`：对外协议规范（HTTP `/api` + WebSocket `/api/ws`）
- `doc/generated/*`：由代码自动生成的协议产物（端点/链路/WS 事件）
- `doc/audience/*`：按角色组织的阅读入口与落地建议
- `doc/modules/*`：模块职责与代码结构
- `doc/features/*`：关键业务特性设计说明
- `doc/plugins/*`：插件机制、生命周期与 API
- `doc/ops/*`：部署、安全、性能、可观测性

## 3. 文档统一约定

- 对外接口统一使用术语：`HTTP API 路由`、`WebSocket 事件`
- 不再使用旧对外协议术语（TCP/Netty 路由）描述客户端接入
- 对外 JSON 字段统一 `snake_case`
- 示例中的实体 ID（`uid/cid/mid`）统一按“十进制字符串”表达

## 4. 文档治理策略

- 一次性扫描报告、阶段性验收表、草案计划不作为长期规范文档保留
- 规范类文档优先“短而准”，避免在多处重复同一接口字段
- 新增接口后运行 `python3 scripts/generate_protocol_artifacts.py` 自动同步端点清单与生成产物
- 提交前运行 `bash scripts/check_code_as_doc.sh` 做“代码即文档”契约校验
