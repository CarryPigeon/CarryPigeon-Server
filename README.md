# CarryPigeon Backend

CarryPigeon Backend 是一个基于 **Java 21 + Spring Boot** 的开源聊天后端脚手架。

## 当前状态

- 主要目标：稳定聊天内核、HTTP API、标准错误模型、基础鉴权与可替换基础设施边界
- 当前交付方式：`thin jar + libs` 分发
- 当前外部依赖：MySQL、Redis、MinIO

## 主要特性

- 可拔插的外部服务
- 高度可定制的消息类型
- 面向拓展到插件系统

## 模块结构

- `application-starter`：启动与运行时装配模块
- `chat-domain`：核心业务域模块
- `infrastructure-basic`：固定全局基础设施模块
- `infrastructure-service`：可拔插外部服务基础设施父模块
  - 各级子模块承当具体的外部服务功能
- `distribution`：当前 thin jar + libs 打包与分发模块

## 当前边界说明

- 当前项目仍处于迭代期，不应默认视为生产级稳定发行版
- Docker 当前只承接外部依赖，不承接应用容器化（由于插件化性质决定）
- 当前部署手册不覆盖 Kubernetes、多节点高可用或生产级进程托管方案由部署方自行优化实现
