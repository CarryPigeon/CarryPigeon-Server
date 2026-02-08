# 插件文档总览

> 本目录描述服务端插件体系：能力边界、开发流程、生命周期与安全约束。

## 1. 阅读顺序（推荐）

1. 架构概览：`doc/plugins/plugin-architecture.md`
2. 开发实践：`doc/plugins/plugin-dev-guide.md`
3. API 参考：`doc/plugins/plugin-api-reference.md`
4. 生命周期：`doc/plugins/plugin-lifecycle.md`
5. 安全规范：`doc/plugins/plugin-security-and-sandbox.md`

## 2. 插件定位

- 目标：在不破坏核心链路的前提下，增强业务能力
- 边界：优先通过 `api` 暴露的稳定接口扩展
- 原则：可灰度、可回滚、可观测

## 3. 两类插件

- 拓展性插件：新增 LiteFlow 节点或服务能力，增强现有流程
- 侵入性插件：替换 DAO/Service 默认实现（需严格评估风险）

## 4. 必须遵守的规范

- 对外协议与错误模型以 `doc/api/*` 为准
- 日志与敏感信息治理遵循 `doc/ops/*`
- 插件包元数据与扫描策略遵循 `doc/api/15-插件包扫描与Manifest规范.md`
