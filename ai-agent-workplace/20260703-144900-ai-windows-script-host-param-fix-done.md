任务名称：Windows 启动脚本 Host 参数冲突修复

任务目标：修复 Windows PowerShell 启动脚本因 `Host` 参数名与内置只读变量冲突导致无法启动的问题。

任务背景：用户执行 `./bin/windows/app-start.bat` 时报错 `Cannot overwrite variable Host because it is read-only or constant`，定位到 `Test-TcpPort` 函数使用了 `Host` 作为参数名。

影响模块：启动脚本与分发脚本。

允许修改范围：仅允许修改 Windows PowerShell 脚本中的 TCP 检查函数参数命名及调用点。

禁止修改范围：不修改 Java 业务逻辑，不调整模块依赖，不新增依赖，不修改运行配置语义。

依赖限制：不新增依赖。

配置限制：不新增配置项。

文档依据：`AGENTS.md`、`docs/standards/变更审核清单.md`、`docs/operations/部署手册.md`。

任务分解 / 执行计划：
1. 检索 Windows 脚本中 `Host` 参数使用点。
2. 将 `Host` 参数改为不与 PowerShell 内置变量冲突的名称。
3. 对脚本进行 PowerShell 语法级验证。
4. 记录结果并关闭任务单。

关键假设与依赖：该错误由 PowerShell 参数变量绑定触发，改名不会改变端口检查行为。

实现要求：保持原有启动流程、端口检查和错误提示不变。

测试要求：至少执行 PowerShell 解析级验证。

质量门禁：脚本语法验证通过。

复审要求：检查调用点与函数参数名一致。

文档要求：本次不引入长期规则，不修改 `docs/`。

验收标准：执行 `bin/windows/app-start.bat` 不再因 `Host` 变量只读问题中断。

完成定义：脚本修复完成、验证完成、任务单改名为 `done`。

实际结果：待填写。

验证记录：待填写。

残留风险：待填写。

知识沉淀 / 是否回写 docs：待填写。

产物清理与保留说明：保留任务单作为协作追踪材料。

补充说明：无。

## 执行结果补充

实际结果：已将 `bin/windows/app-start.ps1` 与 `distribution/src/bin/start.ps1` 中 `Test-TcpPort` 的 `Host` 参数改为 `TargetHost`，并同步更新调用点，避免与 PowerShell 内置只读变量 `$Host` 冲突。

验证记录：
- `rg -n 'Test-TcpPort -Host|\[string\]\$Host|\$task = \$client\.ConnectAsync\(\$Host|\$ServiceName is not reachable at \$Host' bin/windows distribution/src/bin -S || true`：无剩余命中。
- `powershell.exe -NoProfile -Command 'foreach ($p in @("bin/windows/app-start.ps1","distribution/src/bin/start.ps1")) { ... ParseFile ... }; "ok"'`：通过，输出 `ok`。

残留风险：未在当前 Linux/WSL 环境直接执行完整 `app-start.bat`，因为完整启动会进入 Maven 构建和 Spring Boot 运行；本次已覆盖导致用户报错的 PowerShell 参数解析问题。

知识沉淀 / 是否回写 docs：不引入长期规则，不回写 `docs/`。

产物清理与保留说明：任务单保留在 `ai-agent-workplace/`，并关闭为 `done`。
