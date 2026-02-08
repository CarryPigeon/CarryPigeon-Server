# 插件开发者文档（如何扩展后端）

> 目标读者：在尽量不修改核心代码的前提下扩展 CarryPigeon Backend 的开发者。

## 1. 快速导航

- 插件文档入口：`doc/plugins/README.md`
- 插件架构：`doc/plugins/plugin-architecture.md`
- 插件开发教程：`doc/plugins/plugin-dev-guide.md`
- 插件 API 列表：`doc/plugins/plugin-api-reference.md`
- 生命周期与安全：`doc/plugins/plugin-lifecycle.md`、`doc/plugins/plugin-security-and-sandbox.md`

## 2. 两种插件模式

### 2.1 拓展性插件（推荐）

通过 Spring + LiteFlow 增强现有链路，不替换底层实现：

- 新增 `@LiteflowComponent` 节点
- 在 `application-starter/src/main/resources/config/api_*.xml` 中插入节点
- 适用于审计、风控、通知扩展与业务增强

### 2.2 侵入性插件

替换宿主默认实现（DAO/外部服务等），保持 `api` 抽象契约不变：

- 实现 `api` 模块中的 DAO/Service 接口
- 通过 `@Primary` 或 `@ConditionalOnProperty` 控制启用

## 3. 最小实践路径

1. 优先依赖 `api` 模块，不依赖实现细节
2. 实现 LiteFlow 节点并使用统一 key 常量
3. 插入链路并验证错误路径
4. 通过配置开关实现灰度启停

## 4. 契约与规范

- 错误模型遵循 `doc/api/13-错误模型与Reason枚举.md`
- 对外 JSON 统一 `snake_case`
- 插件节点异常需可控降级，不得破坏主链路稳定性

## 5. 上线前检查

- 安全边界：`doc/plugins/plugin-security-and-sandbox.md`
- 生命周期资源释放：`doc/plugins/plugin-lifecycle.md`
- 压测与灰度开关完整
