# 打包与启动（distribution 分发包）

本文描述当前后端项目通过 `distribution` 模块生成分发包（zip），以及解压后用 `-cp` 方式启动的标准流程。

## 目标结构

分发包解压后目录必须严格包含以下结构：

- `application-starter-<version>.jar`：启动 jar（thin jar，置于根目录）
- `module/`：本项目核心模块 jar
- `libs/`：第三方依赖 jar
- `config/`：可修改的外置配置文件目录（含 `application.yaml`、Log4j2、LiteFlow 规则等）

对应打包配置见：

- `distribution/src/assembly/dist.xml`

## 打包

在仓库根目录执行（建议跳过测试以加快打包）：

```bash
mvn -pl distribution -am package -DskipTests=true
```

产物输出：

- `distribution/target/full-distribution.zip`

如果你的环境需要指定 Maven settings 或本地仓库目录，可按需添加：

```bash
mvn -s maven-settings.xml -Dmaven.repo.local=.m2/repository -pl distribution -am package -DskipTests=true
```

## 解压

将 `distribution/target/full-distribution.zip` 解压到本地任意目录，进入解压后的 `full-distribution/` 目录执行启动命令。

## 启动（classpath 方式）

说明：

- `config/` 加入 classpath：用于让 `application.yaml` 与 `classpath:log4j2_config.xml` 可被加载
- `.` 加入 classpath：用于让 `config/*.xml`（LiteFlow 规则）可从当前目录下的 `config/` 目录被解析到
- `module/*`、`libs/*`：将业务模块与第三方依赖全部加入 classpath

### Linux/macOS

在解压后的 `full-distribution/` 目录执行：

```bash
java -cp "config:.:application-starter-*.jar:module/*:libs/*" team.carrypigeon.backend.starter.ApplicationStarter
```

### Windows（PowerShell/CMD）

在解压后的 `full-distribution\\` 目录执行：

```bat
java -cp "config;.;application-starter-*.jar;module\\*;libs\\*" team.carrypigeon.backend.starter.ApplicationStarter
```

## 配置修改

解压后可直接编辑：

- `config/application.yaml`
- `config/log4j2_config.xml`
- `config/*.xml`（LiteFlow 规则文件）

启动时将自动优先使用外置配置目录中的内容（分发包将配置文件打包到 `config/` 下，便于本地解压后修改再启动）。
