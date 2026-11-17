# 分发包

执行 `mvn -pl distribution -am package`，得到
`distribution/target/full-distribution.zip` 和目录
`distribution/target/full-distribution/full-distribution/`。

当前包内脚本支持 Linux，需要 Bash、Java 25 或更新版本，以及 util-linux 的 `flock`。
Windows 可自行使用 Java 类路径启动；当前不提供包内 Windows 生命周期脚本。

## 目录与配置

- `app/`：应用 thin JAR。
- `lib/`：内部模块和运行依赖。
- `config/`：外部 `application.yaml` 和 `log4j2-spring.xml`。
- `bin/`：离线检查、前后台启动与停止脚本。
- `service/systemd/carrypigeon.service`：Linux 服务单元模板。

部署前填写 `config/application.yaml` 中的 `cp.chat.auth.jwt.secret`、
`cp.chat.file.share-key.secret`，并核对 `cp.server.meta` 和全部 `service.*` 连接配置。
两个密钥分别使用至少 32 字符的随机值。启用实时通道时必须填写
`cp.chat.server.realtime.public-url`，例如经 TLS 代理部署使用
`wss://chat.example.com/api/ws`，本机直连使用 `ws://127.0.0.1:18080/api/ws`。
`host`、`port` 只控制本机监听，不能用来推断公开地址。
配置文件中未覆盖的值沿用 JAR 内默认配置。

脚本不加载 `.env`。Spring 支持环境变量、profile 和 `--property=value` 参数；
例如启动时传入 `--server.port=8081`。JVM 参数通过标准 `JAVA_TOOL_OPTIONS` 设置。
插件实现若需要配置，使用其实际读取的 YAML 命名空间。

## 检查与启动

在解压目录中执行：

```bash
bin/verify.sh
bin/verify.sh --strict-config
bin/start.sh
```

普通检查验证交付文件和 YAML 加载；严格检查额外复用应用自身关键配置的绑定约束。
这两种检查均不创建 Spring 容器、不连接外部服务，也不代替真实启动或插件兼容性检查。
启动入口会先执行严格检查。

如需加载插件，自行建立 `plugins/`，仅其顶层 JAR 加入类路径。
`bin/start.sh --safe-mode` 或 `bin/start-background.sh --safe-mode` 会从启动类路径移除插件。
插件代码与应用具有同等进程权限；当前没有插件预检或隔离机制。

后台启动与停止：

```bash
bin/start-background.sh
bin/stop.sh
```

脚本固定从分发目录启动，前后台入口使用同一进程锁。
PID 保存在 `run/application.pid`，后台 stdout 写入
`service-logs/application-stdout.log`；`CP_LOG_HOME` 可覆盖日志目录。

后台启动等待主入口在 `SpringApplication.run` 返回后写出的
`Application is running ...` 标记，并确认进程仍属于当前分发目录。
该标记出现在初始化检查及监听启动之后，要求保留主入口 INFO 日志。
当前没有 `/internal/readiness` HTTP 探针，也不持续监控外部依赖。
启动失败或超过 `CP_START_TIMEOUT_SECONDS`（默认 60 秒）时，脚本清理本次子进程和 PID。
停止入口在发送 SIGTERM 前核对主类和分发目录标记；超过
`CP_STOP_TIMEOUT_SECONDS`（默认 30 秒）仍未退出时保留 PID 并报告失败。
不要在同一安装目录并行启动多个实例；多实例使用独立目录及配置。

## systemd

将单元模板中的安装目录、用户和组改为实际值，保证用户可读取配置并写入日志和 `run/`。
将模板复制到 `/etc/systemd/system/carrypigeon.service` 后执行
`systemctl daemon-reload` 和 `systemctl enable --now carrypigeon`。
systemd 直接托管前台 JVM，停止服务使用 `systemctl stop carrypigeon`。
`Type=simple` 表示进程已创建，完整启动结果仍需查看应用日志。

## 发布产物

仓库根目录执行 `bash bin/linux/dist-release-bundle.sh`，会先离线检查分发目录，
再生成 `distribution/target/release/full-distribution.sha256` 和版本清单。
校验和用于产物完整性检查，不能代替发布者签名。

日志采集路径：普通日志位于 `service-logs/application/`，审计日志位于
`service-logs/audit/`；归档默认分别保留 30 天和 180 天。进程标准输出仍写入
`service-logs/application-stdout.log`。历史归档保留原位，不由启动脚本搬移或删除。
