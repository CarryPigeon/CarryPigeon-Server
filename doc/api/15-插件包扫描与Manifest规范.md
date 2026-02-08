# 15｜插件包扫描与 Manifest 规范（服务端实现约定）

版本：1.0（draft）  
日期：2026-02-04  

本文描述后端在 **不执行插件代码** 的前提下，如何通过“扫描插件包”提供：
- `GET /api/plugins/catalog` 的目录发现
- `GET /api/domains/catalog` 的 domain 发现 + 合约下载指针
- 服务端对非 `Core:*` domain 的 schema 校验

## 1. 扫描目录

默认扫描目录由配置决定：

```yaml
cp:
  api:
    plugin_package_scan:
      enabled: true
      dir: plugins/packages
      latest_only: true
      refresh_interval_seconds: 30
      download_base_path: api/plugins/download
      contract_base_path: api/contracts
      trust:
        enabled: false
        allowed_plugin_ids: []
        blocked_plugin_ids: []
        allowed_zip_sha256: []
        require_ed25519_signature: false
        ed25519_public_keys: []
```

说明：
- `dir` 为本地文件系统目录，内部放置 `*.zip` 插件包。
- `download_base_path` / `contract_base_path` 为对外返回的 **相对路径**（不带 host）。
- `refresh_interval_seconds` 为服务端定时刷新扫描间隔（秒）；设为 0 可关闭定时刷新（仍会启动扫描一次）。
- `trust.*` 为“服务端白名单/签名校验”策略（可选），用于控制哪些插件包会出现在 catalog / download / contracts 中。

## 2. 插件包格式（最小约定）

插件包为 zip，根目录必须包含：
- `manifest.json`

可选包含：
- `contracts/*.schema.json`（JSON Schema）

## 3. manifest.json（建议字段）

字段使用 `snake_case`：

```json
{
  "plugin_id": "math-formula",
  "name": "Math Formula",
  "version": "1.2.0",
  "min_host_version": "0.1.0",
  "permissions": ["network"],
  "signing_key_id": "publisher-key-01",
  "signature": "BASE64_SIGNATURE",
  "provides_domains": [
    { "domain": "Math:Formula", "domain_version": "1.0.0" }
  ],
  "contracts": [
    {
      "domain": "Math:Formula",
      "domain_version": "1.0.0",
      "schema_path": "contracts/Math-Formula-1.0.0.schema.json",
      "constraints": { "max_payload_bytes": 8192, "max_depth": 20 }
    }
  ],
  "entry": "index.js"
}
```

合约字段说明：
- `contracts[].payload_schema`：也可直接内联 JSON Schema 对象（与 `schema_path` 二选一）
- 未提供 `schema_path` 时，服务端会尝试按约定文件名兜底：`contracts/{domain}-{domain_version}.schema.json`（domain 中 `:` 会替换为 `-`）

兼容 PRD（可选扩展）：
- `contracts[].schema_url` + `contracts[].sha256`：
  - 语义：插件包不内置 schema，而是提供一个“可下载的 schema 指针”与内容哈希
  - 当前服务端扫描实现：**不拉取远程 schema**；仅将其视为“元数据”，并不会把该 domain 计入 `/api/domains/catalog` 的可校验集合
  - 建议：如需服务端参与校验，请使用 `schema_path` 或 `payload_schema` 将 schema 放入插件包内

目录一致性约束（服务端实现）：
- `provides_domains[]` 中声明的每一项都必须能在 `contracts[]` 中找到对应契约
- 若发现 `provides_domains` 与 `contracts` 不一致：服务端会从 `provides_domains` 中移除缺少契约的项，避免误导客户端

## 4. 服务端对外下载端点（实现提供）

> 这些端点用于让 `download.url` / `contract.schema_url` 有一个“可用默认实现”。

- 插件包下载：`GET /api/plugins/download/{plugin_id}/{version}`
- 合约下载：`GET /api/contracts/{plugin_id}/{domain}/{domain_version}`

## 5. Schema 校验支持范围（当前实现）

当前服务端实现支持一组“够用的” JSON Schema 关键字（用于 P0 的结构/字段/未知字段拒绝）：
- `type` / `required` / `properties` / `additionalProperties`
- `items` / `enum`
- `const`
- `minLength` / `maxLength` / `pattern`
- `minimum` / `maximum`
- `minItems` / `maxItems`
- `allOf` / `anyOf` / `oneOf`（子集实现）

暂不支持（后续可扩展）：
- 远程 `$ref` 与更复杂的组合/引用特性（如 `if/then/else`、`dependentSchemas` 等）

已支持（局部）：
- 本地 `$ref`：仅支持 `#` / `#/...`（同一 schema 内 JSON Pointer）

## 6. 插件包信任策略（可选）

当 `cp.api.plugin_package_scan.trust.enabled=true` 时，服务端会在扫描阶段应用如下规则：
- `blocked_plugin_ids`：命中直接跳过
- `allowed_plugin_ids`：非空时，仅允许出现在列表中的 `plugin_id`
- `allowed_zip_sha256`：非空时，仅允许 zip 的 sha256 命中列表
- `require_ed25519_signature=true` 时：要求 `manifest.json` 中包含：
  - `signing_key_id`：用于从 `ed25519_public_keys` 里匹配公钥
  - `signature`：Base64 字符串（Ed25519）

签名消息（服务端验签口径）：
- 以 `manifest.json` 解析后的对象为准，**移除 `signature` 字段** 后进行序列化
- 序列化要求：对象 key 按字典序排序（保证稳定）；数组顺序保持不变
- 最终对序列化后的 UTF-8 字节做 Ed25519 验签

公钥格式：
- `ed25519_public_keys[].public_key_base64` 需要提供 **X.509 SubjectPublicKeyInfo** 编码后的 base64 字符串（JDK 可直接解析）。
