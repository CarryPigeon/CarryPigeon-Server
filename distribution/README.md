# 分发包

执行 `mvn -pl distribution -am package`，得到
`distribution/target/full-distribution.zip` 和目录
`distribution/target/full-distribution/full-distribution/`。
`application-starter` 只提供启动入口；具体基础设施实现由分发模块装配，
不应单独使用 starter JAR 启动完整应用。本地开发启动脚本同样使用分发模块的运行类路径。

当前包内脚本支持 Linux，需要 Bash、curl、Java 25 或更新版本，以及 util-linux 的 `flock`。
Windows 可自行使用 Java 类路径启动；当前不提供包内 Windows 生命周期脚本。

## 目录与配置

- `app/`：应用 thin JAR。
- `lib/`：内部模块和运行依赖。
- `config/`：外部 `application.yaml` 和 `log4j2-spring.xml`。
- `bin/`：离线检查、前后台启动与停止脚本。
- `service/systemd/carrypigeon.service`：Linux 服务单元模板。

部署前填写 `config/application.yaml` 中的 `cp.chat.auth.jwt.secret`、
`cp.chat.file.share-key.secret`，并核对 `cp.server.meta` 和全部 `service.*` 连接配置。
两个密钥必须不同，分别使用至少 32 字符的随机值，也可通过 `CP_JWT_SECRET` 和 `CP_FILE_SHARE_KEY_SECRET` 提供。
默认环境为 `prod`，部署时填写 MySQL、Redis、MinIO 和 SMTP 连接参数（包括 SMTP 主机和发件地址）。四项服务均参与启动检查。
本地启动脚本显式选择 `dev` 并使用前三项服务。验证码邮件写入工作目录下的 `target/dev-mailbox/`，可通过 `service.mail.local.directory` 修改收件目录。
自动化测试在 `application-starter` 的测试类路径激活 `test`，四项服务均使用内存实现。每次启动选择一个运行环境，原四项服务的 `enabled` 开关由环境选择取代。
更换 JWT 密钥会使既有令牌失效；更换文件签名密钥会使尚未过期的上传凭证失效，已上传对象及下载地址不依赖该密钥。
启用实时通道时必须填写
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
后台入口有界轮询 `GET /internal/readiness`，仅返回 204 时报告启动完成；进程提前退出或超时均失败。
默认探测 `http://127.0.0.1:8080/internal/readiness`，修改端口或 HTTP 部署方式时通过 `CP_READINESS_URL` 同步设置。
探针在启动未完成及开始关闭后返回 503。默认仅允许本机，可用 `cp.http.readiness.trusted-networks` 添加管理网段；不采信转发头。
这是启动就绪检查，未实现持续外部依赖监控。

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
