#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path
from typing import Dict, List, Tuple

ROOT = Path(__file__).resolve().parents[1]
CTRL_DIR = ROOT / "chat-domain/src/main/java/team/carrypigeon/backend/chat/domain/controller/web/api"
FLOW_DIR = ROOT / "application-starter/src/main/resources/config"
WS_HANDLER = ROOT / "chat-domain/src/main/java/team/carrypigeon/backend/chat/domain/controller/web/api/ws/ApiWebSocketHandler.java"
WS_PUBLISHER = ROOT / "chat-domain/src/main/java/team/carrypigeon/backend/chat/domain/service/ws/ApiWsEventPublisher.java"
PROBLEM_REASON_ENUM = ROOT / "api/src/main/java/team/carrypigeon/backend/api/chat/domain/error/CPProblemReason.java"
API11_DOC = ROOT / "doc/api/11-HTTP端点清单.md"
API12_DOC = ROOT / "doc/api/12-WebSocket事件清单.md"
API13_DOC = ROOT / "doc/api/13-错误模型与Reason枚举.md"

HTTP_BEGIN_MARKER = "<!-- AUTO-GENERATED:HTTP_ENDPOINTS:BEGIN -->"
HTTP_END_MARKER = "<!-- AUTO-GENERATED:HTTP_ENDPOINTS:END -->"
WS_BEGIN_MARKER = "<!-- AUTO-GENERATED:WS_PROTOCOL:BEGIN -->"
WS_END_MARKER = "<!-- AUTO-GENERATED:WS_PROTOCOL:END -->"
ERROR_BEGIN_MARKER = "<!-- AUTO-GENERATED:ERROR_REASONS:BEGIN -->"
ERROR_END_MARKER = "<!-- AUTO-GENERATED:ERROR_REASONS:END -->"

CHAIN_CONST_PATTERN = re.compile(
    r'private\s+static\s+final\s+String\s+(CHAIN_[A-Z0-9_]+)\s*=\s*"([^"]+)"\s*;'
)
MAPPING_ANNOTATION_PATTERN = re.compile(
    r'@(?P<ann>GetMapping|PostMapping|PutMapping|PatchMapping|DeleteMapping)\((?P<args>[^)]*)\)',
    re.S,
)
METHOD_SIGNATURE_PATTERN = re.compile(
    r'public\s+[^\{;]+?\s+(?P<method>[a-zA-Z_][a-zA-Z0-9_]*)\s*\(',
    re.S,
)
CHAIN_CALL_PATTERN = re.compile(r'flowRunner\.executeOrThrow\(\s*([^,\n]+)\s*,')
CHAIN_XML_PATTERN = re.compile(r'<chain\s+name="([^"]+)"')
WS_EVENT_PATTERN = re.compile(r'publishToUsers\([^,]+,\s*"([^"]+)"')
WS_SWITCH_COMMAND_PATTERN = re.compile(r'case\s+"([^"]+)"\s*->')
WS_SEND_ERR_REASON_PATTERN = re.compile(r'sendErr\([^,]+,\s*"[^"]+"\s*,\s*[^,]+,\s*CPProblemReason\.([A-Z0-9_]+)\s*,')
WS_STATIC_TYPE_PATTERN = re.compile(r'put\(\s*"type"\s*,\s*"([^"]+)"\s*\)')
REASON_ENUM_PATTERN = re.compile(r'^\s*([A-Z][A-Z0-9_]*)\("([a-z0-9_]+)",\s*(\d+)\)\s*[,;]', re.M)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def _extract_path(args: str) -> str:
    m = re.search(r'value\s*=\s*"([^"]+)"', args)
    if m:
        return m.group(1)
    m = re.search(r'"([^"]+)"', args)
    if m:
        return m.group(1)
    return ""


def _extract_body(src: str, open_brace_index: int) -> str:
    depth = 0
    i = open_brace_index
    while i < len(src):
        ch = src[i]
        if ch == '{':
            depth += 1
        elif ch == '}':
            depth -= 1
            if depth == 0:
                return src[open_brace_index + 1:i]
        i += 1
    return ""


def _resolve_chain(raw: str, constants: Dict[str, str]) -> str:
    raw = raw.strip()
    if raw.startswith('"') and raw.endswith('"'):
        return raw.strip('"')
    return constants.get(raw, raw)


def _normalize_block_join(prefix: str, block: str, suffix: str) -> str:
    normalized_prefix = prefix.rstrip("\n")
    normalized_block = block.rstrip("\n")
    normalized_suffix = suffix.lstrip("\n")
    if normalized_suffix:
        return f"{normalized_prefix}\n\n{normalized_block}\n\n{normalized_suffix}"
    return f"{normalized_prefix}\n\n{normalized_block}\n"


def sync_doc_block(
    current: str,
    generated_block: str,
    begin_marker: str,
    end_marker: str,
    insert_anchor: str,
) -> str:
    begin_idx = current.find(begin_marker)
    end_idx = current.find(end_marker)

    if begin_idx >= 0 and end_idx > begin_idx:
        block_end = end_idx + len(end_marker)
        return _normalize_block_join(current[:begin_idx], generated_block, current[block_end:])

    idx = current.find(insert_anchor)
    if idx < 0:
        return _normalize_block_join(current, generated_block, "")
    return _normalize_block_join(current[:idx], generated_block, current[idx:])


def generate_http_endpoints() -> dict:
    items: List[dict] = []
    for java_file in sorted(CTRL_DIR.glob("*.java")):
        if java_file.name.endswith("ExceptionHandler.java"):
            continue
        source = _read(java_file)
        constants = {name: value for name, value in CHAIN_CONST_PATTERN.findall(source)}

        for ann in MAPPING_ANNOTATION_PATTERN.finditer(source):
            http_method = ann.group("ann").replace("Mapping", "").upper()
            path = _extract_path(ann.group("args"))

            method_match = METHOD_SIGNATURE_PATTERN.search(source, ann.end())
            if not method_match:
                continue
            method_name = method_match.group("method")

            signature_start = method_match.start()
            open_brace_index = source.find('{', method_match.end())
            if open_brace_index < 0:
                continue
            signature_text = source[signature_start:open_brace_index]
            body = _extract_body(source, open_brace_index)

            chain = None
            c = CHAIN_CALL_PATTERN.search(body)
            if c:
                chain = _resolve_chain(c.group(1), constants)

            auth = "required" if "ApiAuth.requireUid(" in body else "public"

            request_body_type = None
            rb = re.search(r'@RequestBody\s+(?:@Valid\s+)?([A-Za-z0-9_$.<>]+)', signature_text)
            if rb:
                request_body_type = rb.group(1)
            else:
                rb = re.search(r'@Valid\s+@RequestBody\s+([A-Za-z0-9_$.<>]+)', signature_text)
                if rb:
                    request_body_type = rb.group(1)

            items.append({
                "method": http_method,
                "path": path,
                "controller": java_file.stem,
                "handler": method_name,
                "chain": chain,
                "auth": auth,
                "request_body_type": request_body_type,
            })

    items.sort(key=lambda x: (x["path"], x["method"], x["controller"], x["handler"]))
    return {
        "kind": "http_endpoints",
        "source": "chat-domain/controller + ApiFlowRunner chain calls",
        "count": len(items),
        "items": items,
    }


def generate_flow_chains() -> dict:
    items: List[dict] = []
    for xml_file in sorted(FLOW_DIR.glob("api_*.xml")):
        content = _read(xml_file)
        for chain in CHAIN_XML_PATTERN.findall(content):
            items.append({
                "chain": chain,
                "file": xml_file.name,
            })
    items.sort(key=lambda x: x["chain"])
    return {
        "kind": "api_flow_chains",
        "source": "application-starter config/api_*.xml",
        "count": len(items),
        "items": items,
    }


def generate_error_reasons() -> dict:
    source = _read(PROBLEM_REASON_ENUM)
    items: List[dict] = []
    for enum_name, code, status in REASON_ENUM_PATTERN.findall(source):
        items.append({
            "enum": enum_name,
            "code": code,
            "status": int(status),
        })
    if not items:
        raise RuntimeError(f"No reason entries parsed from {PROBLEM_REASON_ENUM}")

    by_status: Dict[int, int] = {}
    for item in items:
        by_status[item["status"]] = by_status.get(item["status"], 0) + 1

    return {
        "kind": "error_reasons",
        "source": "api CPProblemReason enum",
        "count": len(items),
        "status_distribution": by_status,
        "items": items,
    }


def generate_ws_protocol(reason_data: dict) -> dict:
    handler = _read(WS_HANDLER)
    publisher = _read(WS_PUBLISHER)

    code_by_enum = {item["enum"]: item["code"] for item in reason_data["items"]}

    event_types = sorted(set(WS_EVENT_PATTERN.findall(publisher)))
    commands = sorted(set(WS_SWITCH_COMMAND_PATTERN.findall(handler)))

    ws_reason_enums = sorted(set(WS_SEND_ERR_REASON_PATTERN.findall(handler)))
    error_reasons = sorted({code_by_enum.get(enum_name, enum_name.lower()) for enum_name in ws_reason_enums})

    static_types = sorted(set(WS_STATIC_TYPE_PATTERN.findall(handler) + WS_STATIC_TYPE_PATTERN.findall(publisher)))
    static_types = [t for t in static_types if t]

    return {
        "kind": "ws_protocol",
        "source": "ApiWebSocketHandler + ApiWsEventPublisher",
        "commands": commands,
        "error_reasons": error_reasons,
        "event_types": event_types,
        "static_message_types": static_types,
    }


def render_json(data: dict) -> str:
    return json.dumps(data, ensure_ascii=False, indent=2, sort_keys=True) + "\n"


def render_http_endpoints_markdown(http_data: dict) -> str:
    lines = [
        "# HTTP Endpoints (Generated)",
        "",
        "> Auto-generated from controller code. Do not edit manually.",
        "",
        f"Total: {http_data['count']}",
        "",
        "| Method | Path | Auth | Chain | Handler | Request Body |",
        "|---|---|---|---|---|---|",
    ]
    for item in http_data["items"]:
        chain = item["chain"] if item["chain"] else "-"
        req = item["request_body_type"] if item["request_body_type"] else "-"
        handler = f"{item['controller']}#{item['handler']}"
        lines.append(
            f"| `{item['method']}` | `{item['path']}` | `{item['auth']}` | `{chain}` | `{handler}` | `{req}` |"
        )
    lines.append("")
    return "\n".join(lines)


def render_flow_chains_markdown(flow_data: dict) -> str:
    lines = [
        "# API Flow Chains (Generated)",
        "",
        "> Auto-generated from `config/api_*.xml`. Do not edit manually.",
        "",
        f"Total: {flow_data['count']}",
        "",
        "| Chain | XML File |",
        "|---|---|",
    ]
    for item in flow_data["items"]:
        lines.append(f"| `{item['chain']}` | `{item['file']}` |")
    lines.append("")
    return "\n".join(lines)


def render_error_reasons_markdown(reason_data: dict) -> str:
    lines = [
        "# Error Reasons (Generated)",
        "",
        "> Auto-generated from `CPProblemReason` enum. Do not edit manually.",
        "",
        f"Total: {reason_data['count']}",
        "",
        "## Status Distribution",
    ]

    for status in sorted(reason_data["status_distribution"].keys()):
        lines.append(f"- `{status}`: {reason_data['status_distribution'][status]}")

    lines.extend([
        "",
        "## Canonical Reason Table",
        "| Enum | Reason Code | HTTP Status |",
        "|---|---|---|",
    ])
    for item in reason_data["items"]:
        lines.append(f"| `{item['enum']}` | `{item['code']}` | `{item['status']}` |")

    lines.append("")
    return "\n".join(lines)


def render_ws_protocol_markdown(ws_data: dict) -> str:
    lines = [
        "# WebSocket Protocol (Generated)",
        "",
        "> Auto-generated from WS handler/publisher code. Do not edit manually.",
        "",
        "## Commands",
    ]
    if ws_data["commands"]:
        lines.extend([f"- `{cmd}`" for cmd in ws_data["commands"]])
    else:
        lines.append("- (none)")

    lines.extend(["", "## Error Reasons"])
    if ws_data["error_reasons"]:
        lines.extend([f"- `{reason}`" for reason in ws_data["error_reasons"]])
    else:
        lines.append("- (none)")

    lines.extend(["", "## Event Types"])
    if ws_data["event_types"]:
        lines.extend([f"- `{event_type}`" for event_type in ws_data["event_types"]])
    else:
        lines.append("- (none)")

    lines.extend(["", "## Static Message Types"])
    if ws_data["static_message_types"]:
        lines.extend([f"- `{msg_type}`" for msg_type in ws_data["static_message_types"]])
    else:
        lines.append("- (none)")

    lines.append("")
    return "\n".join(lines)


def build_api11_generated_block(http_data: dict) -> str:
    lines = [
        HTTP_BEGIN_MARKER,
        "### 1.1 自动生成端点总表（代码权威）",
        "",
        "> 本表由 `scripts/generate_protocol_artifacts.py` 从 Controller + Flow 代码自动生成。",
        "> 变更接口后请重新生成产物，不要手改下表。",
        "",
        "| Method | Path | Auth | Chain | Handler | Request Body |",
        "|---|---|---|---|---|---|",
    ]
    for item in http_data["items"]:
        chain = item["chain"] if item["chain"] else "-"
        req = item["request_body_type"] if item["request_body_type"] else "-"
        handler = f"{item['controller']}#{item['handler']}"
        lines.append(
            f"| `{item['method']}` | `{item['path']}` | `{item['auth']}` | `{chain}` | `{handler}` | `{req}` |"
        )
    lines += ["", HTTP_END_MARKER, ""]
    return "\n".join(lines)


def build_api12_generated_block(ws_data: dict) -> str:
    lines = [
        WS_BEGIN_MARKER,
        "## 1.1 自动生成 WS 协议总表（代码权威）",
        "",
        "> 本节由 `scripts/generate_protocol_artifacts.py` 从 WS 处理代码自动生成。",
        "> 变更命令/事件后请重新生成产物，不要手改下表。",
        "",
        "### 命令（客户端 -> 服务端）",
    ]

    if ws_data["commands"]:
        lines.extend([f"- `{cmd}`" for cmd in ws_data["commands"]])
    else:
        lines.append("- (none)")

    lines.extend([
        "",
        "### 命令响应类型（服务端 -> 客户端）",
        "- 成功：`<command>.ok`",
        "- 失败：`<command>.err`",
        "",
        "### 错误 reason（来自处理器）",
    ])
    if ws_data["error_reasons"]:
        lines.extend([f"- `{reason}`" for reason in ws_data["error_reasons"]])
    else:
        lines.append("- (none)")

    lines.extend(["", "### 事件类型（event.data.event_type）"])
    if ws_data["event_types"]:
        lines.extend([f"- `{event_type}`" for event_type in ws_data["event_types"]])
    else:
        lines.append("- (none)")

    lines.extend(["", "### 静态消息 type（代码字面量）"])
    if ws_data["static_message_types"]:
        lines.extend([f"- `{msg_type}`" for msg_type in ws_data["static_message_types"]])
    else:
        lines.append("- (none)")

    lines += ["", WS_END_MARKER, ""]
    return "\n".join(lines)


def build_api13_generated_block(reason_data: dict) -> str:
    lines = [
        ERROR_BEGIN_MARKER,
        "## 3.1 自动生成 Reason 总表（代码权威）",
        "",
        "> 本节由 `scripts/generate_protocol_artifacts.py` 从 `CPProblemReason` 自动生成。",
        "> 变更 reason 枚举后请重新生成产物，不要手改下表。",
        "",
        "### 状态码分布",
    ]

    for status in sorted(reason_data["status_distribution"].keys()):
        lines.append(f"- `{status}`: {reason_data['status_distribution'][status]}")

    lines.extend([
        "",
        "### Canonical Reason Table",
        "| Enum | Reason Code | HTTP Status |",
        "|---|---|---|",
    ])
    for item in reason_data["items"]:
        lines.append(f"| `{item['enum']}` | `{item['code']}` | `{item['status']}` |")

    lines += ["", ERROR_END_MARKER, ""]
    return "\n".join(lines)


def build_artifacts() -> Dict[str, str]:
    http_data = generate_http_endpoints()
    flow_data = generate_flow_chains()
    reason_data = generate_error_reasons()
    ws_data = generate_ws_protocol(reason_data)

    current_api11 = _read(API11_DOC)
    current_api12 = _read(API12_DOC)
    current_api13 = _read(API13_DOC)

    next_api11 = sync_doc_block(
        current_api11,
        build_api11_generated_block(http_data),
        HTTP_BEGIN_MARKER,
        HTTP_END_MARKER,
        "## 2. Server（公开）",
    )
    next_api12 = sync_doc_block(
        current_api12,
        build_api12_generated_block(ws_data),
        WS_BEGIN_MARKER,
        WS_END_MARKER,
        "## 2. 事件 envelope（服务端 -> 客户端）",
    )
    next_api13 = sync_doc_block(
        current_api13,
        build_api13_generated_block(reason_data),
        ERROR_BEGIN_MARKER,
        ERROR_END_MARKER,
        "## 4. Reason 枚举（P0 必须稳定）",
    )

    return {
        "doc/generated/http-endpoints.json": render_json(http_data),
        "doc/generated/flow-chains.json": render_json(flow_data),
        "doc/generated/error-reasons.json": render_json(reason_data),
        "doc/generated/ws-protocol.json": render_json(ws_data),
        "doc/generated/http-endpoints.md": render_http_endpoints_markdown(http_data),
        "doc/generated/flow-chains.md": render_flow_chains_markdown(flow_data),
        "doc/generated/error-reasons.md": render_error_reasons_markdown(reason_data),
        "doc/generated/ws-protocol.md": render_ws_protocol_markdown(ws_data),
        "doc/api/11-HTTP端点清单.md": next_api11,
        "doc/api/12-WebSocket事件清单.md": next_api12,
        "doc/api/13-错误模型与Reason枚举.md": next_api13,
    }


def write_artifacts() -> None:
    artifacts = build_artifacts()
    for relative, content in artifacts.items():
        path = ROOT / relative
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(content, encoding="utf-8")


def check_artifacts() -> Tuple[bool, List[str]]:
    artifacts = build_artifacts()
    errors: List[str] = []
    for relative, expected in artifacts.items():
        path = ROOT / relative
        if not path.exists():
            errors.append(f"missing artifact: {path}")
            continue
        actual = path.read_text(encoding="utf-8")
        if actual != expected:
            errors.append(f"artifact out-of-date: {path}")
    return (len(errors) == 0, errors)


def main() -> int:
    parser = argparse.ArgumentParser(description="Generate protocol artifacts from code.")
    parser.add_argument("--check", action="store_true", help="Check mode: fail if generated files are outdated.")
    args = parser.parse_args()

    if args.check:
        ok, errors = check_artifacts()
        if not ok:
            for err in errors:
                print(err, file=sys.stderr)
            return 1
        return 0

    write_artifacts()
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
