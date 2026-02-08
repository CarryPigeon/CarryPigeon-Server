# 插件 API 参考（稳定边界）

> 本文仅列出建议依赖的稳定扩展边界，避免插件绑定宿主内部实现细节。

## 1. 推荐依赖范围

- 领域模型：`team.carrypigeon.backend.api.bo.domain.*`
- 会话与通知模型：`team.carrypigeon.backend.api.bo.connection.*`、`team.carrypigeon.backend.api.connection.notification.*`
- 数据访问抽象：`team.carrypigeon.backend.api.dao.database.*`
- 流程上下文：`team.carrypigeon.backend.api.chat.domain.flow.*`
- 节点基类：`team.carrypigeon.backend.api.chat.domain.node.*`

## 2. 扩展点一览

### 2.1 LiteFlow 节点扩展

适用：拓展性插件

- 新增节点参与 `api_*` chain
- 通过上下文 key 读取请求与会话信息
- 输出结果遵循统一错误模型

### 2.2 DAO/Service 实现替换

适用：侵入性插件

- 实现 `api` 中定义的 DAO/Service 抽象
- 使用 Spring 装配策略替换默认实现
- 保持与宿主一致的输入输出语义

### 2.3 事件与通知扩展

- 可增强事件构建逻辑（payload 加工、附加字段）
- 不可破坏 `doc/api/12-WebSocket事件清单.md` 的既定字段语义

## 3. 兼容性要求

- 对外接口语义必须遵循 `doc/api/*`
- 错误结构必须遵循 `doc/api/13-错误模型与Reason枚举.md`
- JSON 字段命名必须 `snake_case`

## 4. 不推荐依赖

- 宿主内部实现类（`chat-domain` / `dao` 的具体实现）
- 临时/实验性包路径
- 与旧协议耦合的历史对象（非当前协议必需）

## 5. 版本建议

- 插件发布时声明最小宿主版本
- 每次升级验证：登录、消息发送、消息拉取、WS 事件、错误分支
