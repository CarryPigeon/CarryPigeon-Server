# Generated Protocol Artifacts

这些文件由代码自动生成，不允许手工编辑。

生成命令：

```bash
python3 scripts/generate_protocol_artifacts.py
```

一致性检查：

```bash
python3 scripts/generate_protocol_artifacts.py --check
```

输出文件：

- `doc/generated/http-endpoints.json`：HTTP 端点清单（来源：Controller）
- `doc/generated/flow-chains.json`：LiteFlow chain 清单（来源：`config/api_*.xml`）
- `doc/generated/error-reasons.json`：错误 reason 枚举清单（来源：`CPProblemReason`）
- `doc/generated/ws-protocol.json`：WS 命令/事件/错误 reason 清单（来源：WS 处理与发布代码）
- `doc/generated/http-endpoints.md`：HTTP 端点总表（可读 Markdown 视图）
- `doc/generated/flow-chains.md`：Flow 责任链总表（可读 Markdown 视图）
- `doc/generated/error-reasons.md`：错误 reason 总表（可读 Markdown 视图）
- `doc/generated/ws-protocol.md`：WS 协议要点总表（可读 Markdown 视图）
- `doc/api/11-HTTP端点清单.md`：在标记区自动同步 HTTP 端点总表（其余内容可手写）
- `doc/api/12-WebSocket事件清单.md`：在标记区自动同步 WS 命令/事件要点（其余内容可手写）
- `doc/api/13-错误模型与Reason枚举.md`：在标记区自动同步 reason 枚举总表（其余内容可手写）

代码即文档校验（一键）：

```bash
bash scripts/check_code_as_doc.sh
```
