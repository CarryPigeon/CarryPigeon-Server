# CarryPigeon Backend 文档导航

> 本目录用于归档后端的协议、API、架构、模块、运维与插件相关文档。

## 1. 先从哪里读起

- **按读者角色（推荐入口）**
  - 后端开发者：`doc/audience/backend-developer-guide.md`
  - 插件开发者：`doc/audience/plugin-developer-guide.md`
  - 客户端 / 机器人端：`doc/audience/client-developer-guide.md`

- **客户端 / 前端开发**
  - 完整客户端协议与路由说明：`doc/audience/client-developer-guide.md`
  - 推送样例（生产格式示例）：`doc/push.md`
  - 精简英文概览（协议 + 主要路由）：`doc/api.md`

- **后端开发**
  - 总览：`doc/overview.md`
  - 架构：`doc/architecture.md`
  - 模块导航：`doc/modules/README.md`

- **插件开发**
  - 入口：`doc/plugins/README.md`

- **运维 / 安全**
  - 运维：`doc/ops/*`
  - 安全：`doc/ops/security-guidelines.md`

## 2. JSON 字段命名约定（重要）

- **对外 JSON（请求/响应/推送）统一使用 `snake_case`**（由 `common/json/JacksonConfiguration` 配置）。
- 文档中若出现 `camelCase` 的字段名，应以代码实现为准，并优先按本约定修正为 `snake_case`。

## 3. 文档分工（避免重复）

- `doc/audience/client-developer-guide.md`：作为“客户端协议与接口”的主文档（更完整、更偏使用者视角）。
- `doc/api.md`：保留为“精简概览 / 快速对接”，并尽量只包含高层说明与少量示例。
- `doc/api/`：按领域拆分的路由说明（适合前后端协同按功能阅读）。
- `doc/push.md`：只放推送样例与字段说明，避免在其他文档里重复堆大量样例。
