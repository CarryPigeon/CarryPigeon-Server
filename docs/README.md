# CarryPigeon 后端文档

本文档集是 CarryPigeon 后端（Java 21 / Spring Boot / Maven 多模块）的唯一规范入口，供三类读者使用：

- **后端开发者**：按规范新增或修改 feature，不需要先读完整套代码。
- **AI 协作代理**：先读本文与 `standards/`，再动手改代码。
- **客户端与运维**：只看 `api/`、`sql/`、`operations/` 三部分。

## 1. 文档与代码的关系

- **代码是唯一事实来源**。文档只回答"为什么这样切"和"必须遵守什么"，不复制实现细节。
- 文档与代码冲突时，以代码为准，并当场修改文档；不允许"文档说 A、代码做 B"长期共存。
- 文档不粘贴大段代码。必要示例只保留签名、包路径或一句话结论。
- 任何一份文档都不允许成为"第二份实现"：如果一段描述只能靠读代码才能验证，就不该写进文档。

## 2. 以 `channel` 为规范化基准

所有规范都以 `features/channel` 的真实代码作为**正例**。原因：它是当前唯一完整走完下面所有边界的 feature。

| 要看的规范点 | channel 中的参考位置 |
|---|---|
| feature 内分层 | `chat-domain/.../features/channel/{domain,adaptor,controller}` |
| 对外契约 | `domain/api/<capability>/<Capability>Api.java` |
| 契约输入与输出 | `domain/api/<capability>/{input,result}` |
| 契约实现 | `domain/service/<capability>/<Capability>DomainApi.java` |
| 领域模型 | `domain/model/*` |
| 领域错误码 | `domain/problem/ChannelProblem.java` |
| 仓储抽象 | `domain/repository/*Repository.java` |
| 出站实现 | `adaptor/{persistence,realtime,user}` |
| 域内能力抽象 | `domain/capability/{ChannelEventNotifier,MemberProfileQuery}.java` |
| HTTP 入口 | `controller/http/**`（含 `dto`） |

写新文档时，凡是能指向 channel 具体位置的地方，都指向它，而不是写抽象要求。

## 3. 目录地图

```text
docs
├── README.md                      本文：文档地图、阅读顺序、维护规则
├── standards/                     怎么写代码（规范）
│   ├── 包结构与依赖.md             模块划分、feature 内分层、允许/禁止依赖、channel 基准树
│   ├── 异常与错误码.md             ProblemMapping/ProblemException、每 feature 码表、HTTP 与 WS 映射
│   ├── 配置约定.md                 cp.* 前缀、配置绑定方式、本地开发开关
│   ├── 注释与命名.md               类/方法注释边界、命名后缀、禁止的模糊命名
│   ├── 持久化与SQL.md              表结构、仓储与基础设施服务的分工、SQL 脚本约定
│   └── 测试规范.md                 测试分层、命名、必须覆盖的契约（本轮暂不重写）
├── architecture/                  系统怎么组成（架构）
│   ├── 整体架构.md                 四模块职责、端口与适配器、事务与事件推送、运行时装配
│   ├── 实时通道.md                 契约在基建、实现在 config/ws 的方向分区、协议不变量
│   ├── 插件系统.md                 插件边界、加载流程、宿主约束
│   └── 基建清单.md                 infrastructure-basic 已落地的能力与使用约束
├── features/                      按业务域说明（channel 为模板）
│   ├── _模板.md                    feature 文档模板
│   ├── channel.md
│   ├── message.md
│   ├── auth.md
│   ├── user.md
│   ├── file.md
│   ├── plugin.md
│   ├── verification.md
│   └── server.md
├── api/                           对外协议
│   ├── HTTP接口.md                 REST 端点、鉴权、错误响应、示例
│   └── 实时通道协议.md              WS 握手、信封、命令、事件、续传
├── sql/                           数据库定义（唯一事实来源）
│   ├── README.md                   执行顺序、约定、已取消脚本的说明
│   ├── 01-auth.sql
│   ├── 02-user.sql
│   ├── 03-channel.sql              含唯一 system 频道种子
│   ├── 04-message.sql
│   └── 99-reset.sql                破坏性清理
└── operations/                    部署与运维
    ├── 本地开发.md                 起外部服务、启动应用、常用命令
    ├── 部署手册.md                 构建产物、配置覆盖、上线与回滚
    ├── Docker与外部服务.md         docker-compose 服务、端口、数据卷、Nginx 边缘入口
    ├── 数据库部署.md               建库、字符集、脚本执行顺序、验收与备份恢复
    └── 启动脚本索引.md             bin/ 下各脚本的用途与参数
```

## 4. 阅读顺序

| 场景 | 建议顺序 |
|---|---|
| 第一次接触本项目 / AI 首轮 | 本文 → `standards/包结构与依赖.md` → `architecture/整体架构.md` → `features/channel.md` |
| 新增一个 feature | `features/_模板.md` → `standards/包结构与依赖.md` → 参考 `features/channel.md` |
| 修改现有 feature | 对应 `features/<name>.md` → 涉及依赖时看 `standards/包结构与依赖.md` |
| 改基建或模块边界 | `architecture/整体架构.md` → `architecture/基建清单.md` → `standards/包结构与依赖.md` |
| 改对外接口 | `api/HTTP接口.md` 或 `api/实时通道协议.md`，同时核对 `standards/异常与错误码.md` |
| 改表结构 | `sql/README.md` → 对应域脚本 → `standards/持久化与SQL.md` |
| 发版部署 | `operations/部署手册.md` → `operations/Docker与外部服务.md` |

## 5. 文档状态与维护规则

每份文档开头标注状态：

- `状态：待写`：只有占位，不可作为依据。
- `状态：草稿`：内容已成形，仍在评审。
- `状态：定稿`：可被引用，改动需同步下方映射。

**改代码时必须同步的文档**

| 改动内容 | 必须同步 |
|---|---|
| 模块划分、包结构、依赖方向 | `standards/包结构与依赖.md`、`architecture/整体架构.md` |
| 新增或调整错误码 | `standards/异常与错误码.md`、对应 `features/<name>.md` |
| 新增或调整配置项 | `standards/配置约定.md`、`operations/部署手册.md` |
| 新增或调整 HTTP/WS 协议 | `api/` 对应文档 |
| 表结构、索引或字符集变化 | `sql/` 对应脚本、`standards/持久化与SQL.md`、`operations/数据库部署.md` |
| 数据库连接参数或账号权限变化 | `operations/数据库部署.md` §4、`operations/部署手册.md` |
| 外部服务（MySQL/Redis/MinIO）端口或变量变化 | `operations/Docker与外部服务.md` §3–§4 |
| 基建能力变化 | `architecture/基建清单.md` |
| 启动脚本或部署方式变化 | `operations/启动脚本索引.md`、`operations/部署手册.md` |

**禁止事项**

- 用文档替代代码注释：能写在类注释里的局部约束，不写进文档。
- 在文档里维护两份同一事实（例如既保留 all-in-one 建库脚本，又保留分域脚本）。
- 文档先行描述"计划中"的能力，除非明确标注为待办。

## 6. 当前进度

| 文档 | 状态 |
|---|---|
| `README.md` | 草稿（待评审） |
| `standards/包结构与依赖.md` | 待写 |
| `standards/异常与错误码.md` | 草稿（待评审） |
| `standards/配置约定.md` | 草稿（待评审） |
| `standards/注释与命名.md` | 草稿（待评审） |
| `standards/持久化与SQL.md` | 草稿（待评审） |
| `standards/测试规范.md` | 待写（本轮暂不重写） |
| `architecture/整体架构.md` | 草稿（待评审） |
| `architecture/实时通道.md` | 草稿（待评审） |
| `architecture/插件系统.md` | 待写 |
| `architecture/基建清单.md` | 待写 |
| `features/*` | 待写 |
| `api/HTTP接口.md` | 草稿（待评审） |
| `api/实时通道协议.md` | 草稿（待评审） |
| `sql/*` | 草稿（待评审） |
| `operations/Docker与外部服务.md` | 草稿（待评审） |
| `operations/数据库部署.md` | 草稿（待评审） |
| `operations/{本地开发,部署手册,启动脚本索引}.md` | 待写 |

## 7. 历史文档

旧文档集已在提交 `8a12f43` 之后整体删除并按本文结构重建。需要查阅旧内容时：

```bash
git show 8a12f43:docs/architecture/架构文档.md
git checkout 8a12f43 -- docs   # 全量取回
```

旧文档仅作参考，不再具有约束力；与新规范冲突时以新规范为准。

## 8. 暂不重建的内容

- `docs/product/`（产品需求文档、覆盖矩阵）：属于产品侧内容，是否重建待定。
- 根 `change-record.md`：个人变更流水，不属于项目文档。
- 根 `AGENTS.md`：仓库协作规则，待全部规范定稿后统一重写，避免中途丢失协作约束。
