package team.carrypigeon.backend.chat.domain.service.catalog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import team.carrypigeon.backend.chat.domain.controller.web.api.config.CpApiProperties;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.Base64;
import java.security.KeyFactory;
import java.security.spec.X509EncodedKeySpec;

/**
 * 插件包扫描器（文件系统 -> 运行时目录索引）。
 * <p>
 * 功能：
 * <ul>
 *   <li>扫描插件 zip 包目录，解析 {@code manifest.json}</li>
 *   <li>生成插件目录（/api/plugins/catalog）与 domain 目录（/api/domains/catalog）所需的索引</li>
 *   <li>提取 contract schema（用于服务端强校验与合约下载端点）</li>
 *   <li>按配置执行 allow/block/sha256/签名等信任策略</li>
 * </ul>
 * <p>
 * 约束：
 * <ul>
 *   <li>不执行插件代码</li>
 *   <li>只接受 zip 根目录的 {@code manifest.json}（snake_case）</li>
 * </ul>
 */
@Slf4j
@Component
public class ApiPluginPackageScanner {

    /**
     * 插件包内 manifest 文件名（位于 zip 根目录）。
     */
    private static final String MANIFEST_JSON = "manifest.json";

    private final ObjectMapper objectMapper;
    private final CpApiProperties properties;
    private final ApiPluginCatalogIndex catalogIndex;

    /**
     * 上一次扫描目录指纹（用于避免重复扫描）。
     */
    private final AtomicReference<String> lastFingerprint = new AtomicReference<>("");
    /**
     * 上一次定时刷新尝试时间（毫秒）。
     * <p>
     * 注意：定时任务触发频率固定较高（用于快速响应配置变更），实际刷新间隔由配置控制。
     */
    private final AtomicLong lastRefreshAttemptMillis = new AtomicLong(0L);

    /**
     * 构造插件包扫描器。
     *
     * @param objectMapper JSON 读写组件
     * @param properties   API 配置
     * @param catalogIndex 扫描索引存储
     */
    public ApiPluginPackageScanner(ObjectMapper objectMapper, CpApiProperties properties, ApiPluginCatalogIndex catalogIndex) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.catalogIndex = catalogIndex;
    }

    /**
     * 在应用启动阶段执行目录扫描。
     */
    @PostConstruct
    public void scanOnStartup() {
        CpApiProperties.PluginPackageScan cfg = properties.getApi().getPluginPackageScan();
        if (cfg == null || !cfg.isEnabled()) {
            log.info("插件包扫描：已禁用");
            return;
        }
        scanOnce();
    }

    /**
     * 定时刷新入口。
     * <p>
     * 固定 5 秒触发一次，但会按 {@code refreshIntervalSeconds} 做“节流”，避免频繁 IO 扫描。
     */
    @Scheduled(fixedDelay = 5000)
    public void scheduledRefresh() {
        CpApiProperties.PluginPackageScan cfg = properties.getApi().getPluginPackageScan();
        if (cfg == null || !cfg.isEnabled()) {
            return;
        }
        int intervalSeconds = cfg.getRefreshIntervalSeconds();
        if (intervalSeconds <= 0) return;

        long now = System.currentTimeMillis();
        long lastAttempt = lastRefreshAttemptMillis.get();
        if (lastAttempt > 0 && (now - lastAttempt) < intervalSeconds * 1000L) {
            return;
        }
        lastRefreshAttemptMillis.set(now);
        scanOnce();
    }

    /**
     * 执行一次扫描。
     * <p>
     * 该方法会先计算目录指纹；若目录无变化则直接返回。
     */
    public void scanOnce() {
        CpApiProperties.PluginPackageScan cfg = properties.getApi().getPluginPackageScan();
        Path dir = resolveDir(cfg.getDir());
        if (!Files.exists(dir) || !Files.isDirectory(dir)) {
            log.info("插件包扫描：目录不存在：dir={}", dir.toAbsolutePath());
            catalogIndex.replace(ApiPluginCatalogIndex.Snapshot.empty());
            return;
        }

        String fingerprint = fingerprint(dir);
        String last = lastFingerprint.get();
        if (fingerprint != null && !fingerprint.isBlank() && fingerprint.equals(last)) {
            return;
        }

        Map<ApiPluginCatalogIndex.PluginVersionKey, ApiPluginCatalogIndex.PluginPackageFile> pluginFiles = new HashMap<>();
        Map<ApiPluginCatalogIndex.ContractKey, ApiPluginCatalogIndex.ContractFile> contractFiles = new HashMap<>();

        /**
         * plugin_id -> version -> item。
         */
        Map<String, TreeMap<Semver, CpApiProperties.PluginItem>> allPlugins = new HashMap<>();
        /**
         * domain -> versions -> contract meta。
         */
        Map<String, Map<String, DomainContractAccumulator>> domainAcc = new HashMap<>();

        List<Path> zips;
        try (var s = Files.walk(dir)) {
            zips = s.filter(p -> Files.isRegularFile(p) && p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".zip"))
                    .sorted()
                    .toList();
        } catch (Exception e) {
            log.error("插件包扫描：遍历目录失败：dir={}", dir, e);
            catalogIndex.replace(ApiPluginCatalogIndex.Snapshot.empty());
            return;
        }

        for (Path zipPath : zips) {
            try {
                ScanResult r = scanZip(zipPath, cfg);
                if (r == null) {
                    continue;
                }
                CpApiProperties.PluginItem item = r.pluginItem();
                Semver v = Semver.parse(item.getVersion());
                if (v == null) {
                    log.warn("插件包扫描：跳过非法版本：path={}, version={}", zipPath, item.getVersion());
                    continue;
                }

                allPlugins.computeIfAbsent(item.getPluginId(), k -> new TreeMap<>()).put(v, item);
                pluginFiles.put(new ApiPluginCatalogIndex.PluginVersionKey(item.getPluginId(), item.getVersion()),
                        new ApiPluginCatalogIndex.PluginPackageFile(zipPath, r.zipSha256(), r.zipSizeBytes()));

                for (ResolvedContract c : r.contracts()) {
                    contractFiles.put(new ApiPluginCatalogIndex.ContractKey(item.getPluginId(), c.domain(), c.domainVersion()),
                            new ApiPluginCatalogIndex.ContractFile(c.schema(), c.sha256(), c.constraints()));
                    domainAcc.computeIfAbsent(c.domain(), k -> new HashMap<>())
                            .put(c.domainVersion(), new DomainContractAccumulator(item.getPluginId(), item.getVersion(), c));
                }
            } catch (Exception e) {
                log.warn("插件包扫描：扫描 zip 失败：path={}", zipPath, e);
            }
        }

        List<CpApiProperties.PluginItem> pluginsForCatalog = materializePlugins(cfg.isLatestOnly(), allPlugins, properties.getApi().getRequiredPlugins());
        List<ApiPluginCatalogIndex.DomainItem> domainsForCatalog = materializeDomains(cfg, domainAcc);

        ApiPluginCatalogIndex.Snapshot snapshot = new ApiPluginCatalogIndex.Snapshot(
                pluginsForCatalog,
                domainsForCatalog,
                pluginFiles,
                contractFiles
        );
        catalogIndex.replace(snapshot);
        lastFingerprint.set(fingerprint == null ? "" : fingerprint);
        log.info("插件包扫描：完成：plugins={}, domains={}, dir={}",
                pluginsForCatalog.size(), domainsForCatalog.size(), dir.toAbsolutePath());
    }

    /**
     * 计算目录指纹（对 zip 文件路径/大小/修改时间做归一后 hash）。
     */
    private String fingerprint(Path dir) {
        try (var s = Files.walk(dir)) {
            StringBuilder sb = new StringBuilder();
            s.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".zip"))
                    .sorted()
                    .forEach(p -> {
                        try {
                            sb.append(p.toAbsolutePath()).append('|')
                                    .append(Files.size(p)).append('|')
                                    .append(Files.getLastModifiedTime(p).toMillis()).append('\n');
                        } catch (Exception ignored) {
                            sb.append(p.toAbsolutePath()).append('|').append("err").append('\n');
                        }
                    });
            return sha256Hex(sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.debug("插件包扫描：计算目录指纹失败：dir={}", dir, e);
            return "";
        }
    }

    /**
     * 将扫描到的插件（多版本）物化为对外目录的 plugins[]。
     */
    private List<CpApiProperties.PluginItem> materializePlugins(boolean latestOnly,
                                                                Map<String, TreeMap<Semver, CpApiProperties.PluginItem>> all,
                                                                List<String> requiredPlugins) {
        List<CpApiProperties.PluginItem> out = new ArrayList<>();
        for (var e : all.entrySet()) {
            TreeMap<Semver, CpApiProperties.PluginItem> versions = e.getValue();
            if (versions == null || versions.isEmpty()) {
                continue;
            }
            if (latestOnly) {
                CpApiProperties.PluginItem latest = versions.lastEntry().getValue();
                if (latest != null) {
                    latest.setRequired(requiredPlugins != null && requiredPlugins.contains(latest.getPluginId()));
                    out.add(latest);
                }
            } else {
                for (CpApiProperties.PluginItem item : versions.values()) {
                    if (item != null) {
                        item.setRequired(requiredPlugins != null && requiredPlugins.contains(item.getPluginId()));
                        out.add(item);
                    }
                }
            }
        }
        out.sort(Comparator.comparing(CpApiProperties.PluginItem::getPluginId, Comparator.nullsLast(String::compareTo)));
        return out;
    }

    /**
     * 将扫描到的 contract 物化为对外目录的 domains[]。
     */
    private List<ApiPluginCatalogIndex.DomainItem> materializeDomains(CpApiProperties.PluginPackageScan cfg,
                                                                     Map<String, Map<String, DomainContractAccumulator>> domainAcc) {
        List<ApiPluginCatalogIndex.DomainItem> out = new ArrayList<>();
        for (var e : domainAcc.entrySet()) {
            String domain = e.getKey();
            Map<String, DomainContractAccumulator> byVer = e.getValue();
            if (domain == null || domain.isBlank() || byVer == null || byVer.isEmpty()) {
                continue;
            }
            List<String> supported = byVer.keySet().stream().sorted(ApiPluginPackageScanner::compareVersionString).toList();
            String recommended = supported.isEmpty() ? "1.0.0" : supported.getLast();
            DomainContractAccumulator rec = byVer.get(recommended);
            ApiPluginCatalogIndex.Constraints constraints = rec == null ? new ApiPluginCatalogIndex.Constraints(null, null) : rec.contract.constraints();

            List<ApiPluginCatalogIndex.Provider> providers = byVer.values().stream()
                    .filter(Objects::nonNull)
                    .map(v -> new ApiPluginCatalogIndex.Provider("plugin", v.pluginId, v.minPluginVersion))
                    .distinct()
                    .toList();

            String schemaUrl = null;
            String sha = null;
            if (rec != null) {
                schemaUrl = cfg.getContractBasePath().replaceAll("/+$", "") + "/"
                        + rec.pluginId + "/" + urlEncode(domain) + "/" + recommended;
                sha = rec.contract.sha256();
            }
            ApiPluginCatalogIndex.Contract contract = schemaUrl == null ? null : new ApiPluginCatalogIndex.Contract(schemaUrl, sha);

            out.add(new ApiPluginCatalogIndex.DomainItem(domain, supported, recommended, constraints, providers, contract));
        }
        out.sort(Comparator.comparing(ApiPluginCatalogIndex.DomainItem::domain, Comparator.nullsLast(String::compareTo)));
        return out;
    }

    /**
     * 扫描单个 zip 文件并解析出插件条目与 contracts。
     */
    private ScanResult scanZip(Path zipPath, CpApiProperties.PluginPackageScan cfg) throws Exception {
        String zipSha256 = sha256Hex(zipPath);
        long zipSize = Files.size(zipPath);
        try (ZipFile zip = new ZipFile(zipPath.toFile())) {
            ZipEntry manifestEntry = zip.getEntry(MANIFEST_JSON);
            if (manifestEntry == null) {
                log.warn("插件包扫描：跳过缺少 manifest.json 的 zip：path={}", zipPath);
                return null;
            }
            JsonNode manifest = readJson(zip, manifestEntry);
            if (manifest == null || !manifest.isObject()) {
                log.warn("插件包扫描：跳过非法 manifest.json：path={}", zipPath);
                return null;
            }

            String pluginId = text(manifest.get("plugin_id"));
            String name = text(manifest.get("name"));
            String version = text(manifest.get("version"));
            String minHostVersion = text(manifest.get("min_host_version"));
            if (pluginId == null || pluginId.isBlank() || version == null || version.isBlank()) {
                log.warn("插件包扫描：跳过缺少 plugin_id/version 的 zip：path={}", zipPath);
                return null;
            }

            if (!trusted(cfg, manifest, pluginId, zipSha256)) {
                log.warn("插件包扫描：跳过不可信插件包：path={}, pluginId={}, version={}", zipPath, pluginId, version);
                return null;
            }

            CpApiProperties.PluginItem item = new CpApiProperties.PluginItem();
            item.setPluginId(pluginId);
            item.setName(Optional.ofNullable(name).orElse(pluginId));
            item.setVersion(version);
            item.setMinHostVersion(Optional.ofNullable(minHostVersion).orElse("0.1.0"));
            item.setPermissions(readStringList(manifest.get("permissions")));

            item.setProvidesDomains(readProvidesDomains(manifest.get("provides_domains")));
            CpApiProperties.Download dl = new CpApiProperties.Download();
            dl.setUrl(cfg.getDownloadBasePath().replaceAll("/+$", "") + "/" + pluginId + "/" + version);
            dl.setSha256(zipSha256);
            item.setDownload(dl);

            List<ResolvedContract> contracts = readContracts(zip, manifest.get("contracts"));
            if (item.getProvidesDomains() != null && !item.getProvidesDomains().isEmpty()) {
                Map<String, Boolean> hasContract = new HashMap<>();
                for (ResolvedContract c : contracts) {
                    if (c != null) {
                        hasContract.put(c.domain() + "@" + c.domainVersion(), true);
                    }
                }
                List<CpApiProperties.ProvidesDomainItem> filtered = item.getProvidesDomains().stream()
                        .filter(Objects::nonNull)
                        .filter(pd -> pd.getDomain() != null && pd.getDomainVersion() != null)
                        .filter(pd -> Boolean.TRUE.equals(hasContract.get(pd.getDomain() + "@" + pd.getDomainVersion())))
                        .toList();
                int dropped = item.getProvidesDomains().size() - filtered.size();
                if (dropped > 0) {
                    log.warn("插件包扫描：已移除缺少契约的 provides_domains：pluginId={}, version={}, dropped={}", pluginId, version, dropped);
                }
                item.setProvidesDomains(filtered);
            }
            return new ScanResult(item, zipSha256, zipSize, contracts);
        }
    }

    /**
     * 解析 manifest.contracts[] 并读取 schema。
     */
    private List<ResolvedContract> readContracts(ZipFile zip, JsonNode contractsNode) {
        List<ResolvedContract> out = new ArrayList<>();

        if (contractsNode != null && contractsNode.isArray()) {
            for (JsonNode c : contractsNode) {
                if (c == null || !c.isObject()) {
                    continue;
                }
                String domain = text(c.get("domain"));
                String domainVersion = text(c.get("domain_version"));
                if (domain == null || domain.isBlank() || domainVersion == null || domainVersion.isBlank()) {
                    continue;
                }

                JsonNode schema = null;
                if (c.has("payload_schema")) {
                    schema = c.get("payload_schema");
                } else {
                    String schemaPath = text(c.get("schema_path"));
                    if (schemaPath == null || schemaPath.isBlank()) {
                        schemaPath = "contracts/" + normalizeDomain(domain) + "-" + domainVersion + ".schema.json";
                    }
                    ZipEntry entry = zip.getEntry(schemaPath);
                    if (entry != null) {
                        schema = readJson(zip, entry);
                    }
                }
                if (schema == null) {
                    log.warn("插件包扫描：缺少 contract schema：domain={}, version={}", domain, domainVersion);
                    continue;
                }

                ApiPluginCatalogIndex.Constraints constraints = readConstraints(c.get("constraints"));
                if (constraints.maxPayloadBytes() == null || constraints.maxDepth() == null) {
                    Integer mb = intOrNull(c.get("max_payload_bytes"));
                    Integer md = intOrNull(c.get("max_depth"));
                    constraints = new ApiPluginCatalogIndex.Constraints(
                            constraints.maxPayloadBytes() == null ? mb : constraints.maxPayloadBytes(),
                            constraints.maxDepth() == null ? md : constraints.maxDepth()
                    );
                }

                String sha256 = sha256Hex(schema.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
                out.add(new ResolvedContract(domain, domainVersion, schema, sha256, constraints));
            }
        }

        return out;
    }

    /**
     * 读取 constraints（max_payload_bytes/max_depth）。
     */
    private ApiPluginCatalogIndex.Constraints readConstraints(JsonNode node) {
        if (node == null || !node.isObject()) {
            return new ApiPluginCatalogIndex.Constraints(null, null);
        }
        Integer maxPayloadBytes = intOrNull(node.get("max_payload_bytes"));
        Integer maxDepth = intOrNull(node.get("max_depth"));
        return new ApiPluginCatalogIndex.Constraints(maxPayloadBytes, maxDepth);
    }

    /**
     * 读取 provides_domains[]。
     */
    private List<CpApiProperties.ProvidesDomainItem> readProvidesDomains(JsonNode node) {
        if (node == null || !node.isArray()) {
            return List.of();
        }
        List<CpApiProperties.ProvidesDomainItem> out = new ArrayList<>();
        for (JsonNode it : node) {
            if (it == null || !it.isObject()) {
                continue;
            }
            String domain = text(it.get("domain"));
            String domainVersion = text(it.get("domain_version"));
            if (domain == null || domain.isBlank() || domainVersion == null || domainVersion.isBlank()) {
                continue;
            }
            CpApiProperties.ProvidesDomainItem pd = new CpApiProperties.ProvidesDomainItem();
            pd.setDomain(domain);
            pd.setDomainVersion(domainVersion);
            out.add(pd);
        }
        return out;
    }

    /**
     * 读取字符串数组（permissions 等）。
     */
    private List<String> readStringList(JsonNode node) {
        if (node == null || !node.isArray()) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (JsonNode it : node) {
            String s = text(it);
            if (s != null && !s.isBlank()) {
                out.add(s);
            }
        }
        return out;
    }

    /**
     * 从 zip entry 中读取 JSON。
     */
    private JsonNode readJson(ZipFile zip, ZipEntry entry) {
        try (InputStream in = zip.getInputStream(entry)) {
            return objectMapper.readTree(in);
        } catch (Exception e) {
            log.warn("插件包扫描：读取 zip 内 JSON 失败：entry={}", entry.getName(), e);
            return null;
        }
    }

    /**
     * 判断插件包是否可信（allow/block/sha256/签名）。
     */
    private boolean trusted(CpApiProperties.PluginPackageScan cfg,
                            JsonNode manifest,
                            String pluginId,
                            String zipSha256) {
        CpApiProperties.PluginTrust trust = cfg == null ? null : cfg.getTrust();
        if (trust == null || !trust.isEnabled()) {
            return true;
        }
        if (trust.getBlockedPluginIds() != null && trust.getBlockedPluginIds().contains(pluginId)) {
            return false;
        }
        if (trust.getAllowedPluginIds() != null && !trust.getAllowedPluginIds().isEmpty()
                && !trust.getAllowedPluginIds().contains(pluginId)) {
            return false;
        }
        if (trust.getAllowedZipSha256() != null && !trust.getAllowedZipSha256().isEmpty()
                && !trust.getAllowedZipSha256().contains(zipSha256)) {
            return false;
        }
        if (trust.isRequireEd25519Signature()) {
            return verifyEd25519Signature(trust, manifest);
        }
        return true;
    }

    /**
     * 校验 manifest 的 Ed25519 签名。
     * <p>
     * 验签消息口径：移除 {@code signature} 字段后，对 manifest 对象按 key 字典序稳定序列化，再对 UTF-8 字节进行验签。
     */
    private boolean verifyEd25519Signature(CpApiProperties.PluginTrust trust,
                                           JsonNode manifest) {
        if (trust == null) {
            return false;
        }
        String keyId = text(manifest == null ? null : manifest.get("signing_key_id"));
        String sigB64 = text(manifest == null ? null : manifest.get("signature"));
        if (keyId == null || keyId.isBlank() || sigB64 == null || sigB64.isBlank()) {
            return false;
        }
        CpApiProperties.Ed25519PublicKey pk = null;
        if (trust.getEd25519PublicKeys() != null) {
            for (CpApiProperties.Ed25519PublicKey it : trust.getEd25519PublicKeys()) {
                if (it != null && keyId.equals(it.getKeyId())) {
                    pk = it;
                    break;
                }
            }
        }
        if (pk == null || pk.getPublicKeyBase64() == null || pk.getPublicKeyBase64().isBlank()) {
            return false;
        }
        try {
            PublicKey pub = parseEd25519PublicKey(pk.getPublicKeyBase64());
            byte[] sig = Base64.getDecoder().decode(sigB64);
            byte[] msg = canonicalManifestBytesForSignature(manifest);
            if (msg == null) {
                return false;
            }
            Signature verifier = Signature.getInstance("Ed25519");
            verifier.initVerify(pub);
            verifier.update(msg);
            return verifier.verify(sig);
        } catch (Exception e) {
            log.debug("插件包扫描：验签失败：keyId={}", keyId, e);
            return false;
        }
    }

    /**
     * 解析 Ed25519 公钥（X.509 SubjectPublicKeyInfo -> PublicKey）。
     */
    private PublicKey parseEd25519PublicKey(String base64X509) throws Exception {
        byte[] bytes = Base64.getDecoder().decode(base64X509);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(bytes);
        KeyFactory kf = KeyFactory.getInstance("Ed25519");
        return kf.generatePublic(spec);
    }

    /**
     * 生成“用于验签”的 canonical manifest 字节。
     */
    private byte[] canonicalManifestBytesForSignature(JsonNode manifest) {
        if (manifest == null || !manifest.isObject()) {
            return null;
        }
        ObjectNode copy = ((ObjectNode) manifest).deepCopy();
        copy.remove("signature");
        try {
            return objectMapper.writer()
                    .with(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
                    .writeValueAsBytes(copy);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 解析扫描目录配置：相对路径以工作目录解析。
     */
    private Path resolveDir(String dir) {
        if (dir == null || dir.isBlank()) {
            return Paths.get("plugins/packages");
        }
        Path p = Paths.get(dir);
        if (p.isAbsolute()) {
            return p;
        }
        return Paths.get(System.getProperty("user.dir")).resolve(p).normalize();
    }

    /**
     * 计算文件 sha256（hex）。
     */
    private String sha256Hex(Path path) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        try (InputStream in = Files.newInputStream(path); DigestInputStream dis = new DigestInputStream(in, md)) {
            dis.transferTo(java.io.OutputStream.nullOutputStream());
        }
        return hex(md.digest());
    }

    /**
     * 计算字节数组 sha256（hex）。
     */
    private String sha256Hex(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(bytes);
            return hex(md.digest());
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 二进制 digest -> hex 字符串。
     */
    private static String hex(byte[] digest) {
        StringBuilder sb = new StringBuilder(digest.length * 2);
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * 将 JsonNode 转为文本；若非文本节点则返回其 JSON 字符串。
     */
    private static String text(JsonNode n) {
        if (n == null || n.isNull()) {
            return null;
        }
        if (n.isTextual()) {
            return n.asText();
        }
        return n.toString();
    }

    /**
     * 将 JsonNode 转为 Integer；失败返回 null。
     */
    private static Integer intOrNull(JsonNode n) {
        if (n == null || n.isNull()) {
            return null;
        }
        if (n.canConvertToInt()) {
            return n.asInt();
        }
        if (n.isTextual()) {
            try {
                return Integer.parseInt(n.asText());
            } catch (Exception ignored) {
                return null;
            }
        }
        return null;
    }

    /**
     * 将 domain 转为文件名安全片段（用于约定 schema_path）。
     */
    private static String normalizeDomain(String domain) {
        return domain.replace(':', '-').replace('/', '-');
    }

    /**
     * 对 domain 做 URL 编码（当前仅处理 ':'）。
     */
    private static String urlEncode(String domain) {
        return domain.replace(":", "%3A");
    }

    /**
     * 比较两个版本号字符串（按 x.y.z）。
     */
    private static int compareVersionString(String a, String b) {
        Semver sa = Semver.parse(a);
        Semver sb = Semver.parse(b);
        if (sa == null && sb == null) {
            return String.valueOf(a).compareTo(String.valueOf(b));
        }
        if (sa == null) {
            return -1;
        }
        if (sb == null) {
            return 1;
        }
        return sa.compareTo(sb);
    }

    /**
     * 扫描过程中的中间聚合结构：用于生成 DomainCatalog providers/contract。
     */
    private static final class DomainContractAccumulator {
        private final String pluginId;
        private final String minPluginVersion;
        private final ResolvedContract contract;

        /**
         * 构造领域契约聚合项。
         *
         * @param pluginId          插件标识
         * @param minPluginVersion  最小插件版本
         * @param contract          解析后的契约对象
         */
        private DomainContractAccumulator(String pluginId, String minPluginVersion, ResolvedContract contract) {
            this.pluginId = pluginId;
            this.minPluginVersion = minPluginVersion;
            this.contract = contract;
        }
    }

    /**
     * 单个 zip 的扫描结果。
     */
    private record ScanResult(CpApiProperties.PluginItem pluginItem, String zipSha256, long zipSizeBytes, List<ResolvedContract> contracts) {
    }

    /**
     * 单个 domain contract（解析自 manifest.contracts[]）。
     */
    private record ResolvedContract(String domain, String domainVersion, JsonNode schema, String sha256, ApiPluginCatalogIndex.Constraints constraints) {
    }

    /**
     * 最小 SemVer 解析/比较器（仅支持 x.y.z；忽略预发布/构建号）。
     */
    private record Semver(int major, int minor, int patch, String raw) implements Comparable<Semver> {
        static Semver parse(String v) {
            if (v == null || v.isBlank()) {
                return null;
            }
            String s = v.trim();
            int dash = s.indexOf('-');
            if (dash > 0) {
                s = s.substring(0, dash);
            }
            String[] parts = s.split("\\.");
            if (parts.length < 3) {
                return null;
            }
            try {
                return new Semver(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), v);
            } catch (Exception ignored) {
                return null;
            }
        }

        /**
         * 比较语义化版本顺序。
         *
         * @param o 待比较的语义化版本对象
         * @return 小于 0 表示当前版本更小，等于 0 表示相同，大于 0 表示更大
         */
        @Override
        public int compareTo(Semver o) {
            int c = Integer.compare(major, o.major);
            if (c != 0) return c;
            c = Integer.compare(minor, o.minor);
            if (c != 0) return c;
            return Integer.compare(patch, o.patch);
        }
    }
}
