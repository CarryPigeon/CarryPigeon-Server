package team.carrypigeon.backend.chat.domain.service.catalog;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.chat.domain.controller.web.api.config.CpApiProperties;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 插件/Domain 运行时目录索引（由插件包扫描器生成）。
 *
 * <p>索引用途：
 * <ul>
 *   <li>{@code GET /api/plugins/catalog}</li>
 *   <li>{@code GET /api/domains/catalog}</li>
 *   <li>{@code /api/plugins/download/**} 与 {@code /api/contracts/**}</li>
 *   <li>服务端对非 Core domain 的 schema/constraints 校验</li>
 * </ul>
 *
 * <p>线程安全：通过“不可变快照 + 原子替换”实现读写隔离；读侧无锁。
 */
@Service
public class ApiPluginCatalogIndex {

    /** 当前索引快照（扫描器每次刷新后整体替换）。 */
    private final AtomicReference<Snapshot> snapshot = new AtomicReference<>(Snapshot.empty());

    /**
     * 获取当前索引快照。
     *
     * @return 不为 null 的快照
     */
    public Snapshot snapshot() {
        return snapshot.get();
    }

    /**
     * 替换当前索引快照。
     *
     * <p>注意：传入 null 将替换为 {@link Snapshot#empty()}，避免读侧空指针。
     *
     * @param snapshot 新快照
     */
    public void replace(Snapshot snapshot) {
        this.snapshot.set(snapshot == null ? Snapshot.empty() : snapshot);
    }

    /**
     * 一次扫描生成的目录快照（不可变视图）。
     */
    @Getter
    public static final class Snapshot {
        private final List<CpApiProperties.PluginItem> plugins;
        private final List<DomainItem> domains;
        private final Map<PluginVersionKey, PluginPackageFile> pluginFiles;
        private final Map<ContractKey, ContractFile> contractFiles;

        public Snapshot(List<CpApiProperties.PluginItem> plugins,
                        List<DomainItem> domains,
                        Map<PluginVersionKey, PluginPackageFile> pluginFiles,
                        Map<ContractKey, ContractFile> contractFiles) {
            this.plugins = plugins == null ? List.of() : plugins;
            this.domains = domains == null ? List.of() : domains;
            this.pluginFiles = pluginFiles == null ? Map.of() : pluginFiles;
            this.contractFiles = contractFiles == null ? Map.of() : contractFiles;
        }

        /**
         * 创建空快照对象。
         *
         * @return 不包含任何插件与领域数据的空快照
         */
        public static Snapshot empty() {
            return new Snapshot(List.of(), List.of(), Map.of(), Map.of());
        }

        /**
         * 查询插件包索引文件。
         *
         * @param pluginId 插件标识
         * @param version 插件版本号
         * @return 对应插件版本的包文件信息；不存在时返回 { null}
         */
        public PluginPackageFile pluginFile(String pluginId, String version) {
            return pluginFiles.get(new PluginVersionKey(pluginId, version));
        }

        /**
         * 查询领域契约索引文件。
         *
         * @param pluginId 插件标识
         * @param domain 领域标识
         * @param domainVersion 领域版本号
         * @return 对应领域契约文件；不存在时返回 { null}
         */
        public ContractFile contractFile(String pluginId, String domain, String domainVersion) {
            return contractFiles.get(new ContractKey(pluginId, domain, domainVersion));
        }

        /**
         * 插件目录不可变视图。
         */
        public List<CpApiProperties.PluginItem> pluginsView() {
            return Collections.unmodifiableList(plugins);
        }

        /**
         * Domain 目录不可变视图。
         */
        public List<DomainItem> domainsView() {
            return Collections.unmodifiableList(domains);
        }
    }

    /**
     * 插件包定位键（{@code plugin_id + version}）。
     */
    public record PluginVersionKey(String pluginId, String version) {
    }

    /**
     * 插件包文件信息（用于下载端点）。
     */
    public record PluginPackageFile(Path path, String sha256, long sizeBytes) {
    }

    /**
     * 合约文件定位键（{@code plugin_id + domain + domain_version}）。
     */
    public record ContractKey(String pluginId, String domain, String domainVersion) {
    }

    /**
     * 合约文件信息（schema + sha256 + 约束）。
     */
    public record ContractFile(JsonNode schema, String sha256, Constraints constraints) {
    }

    /**
     * 服务端强约束（与 API 文档字段对齐）。
     */
    public record Constraints(Integer maxPayloadBytes, Integer maxDepth) {
    }

    /**
     * Domain 目录条目。
     */
    public record DomainItem(String domain,
                             List<String> supportedVersions,
                             String recommendedVersion,
                             Constraints constraints,
                             List<Provider> providers,
                             Contract contract) {
    }

    /**
     * Domain 提供方。
     *
     * <p>type 取值：
     * <ul>
     *   <li>{@code core}：服务端内置</li>
     *   <li>{@code plugin}：由插件包提供</li>
     * </ul>
     */
    public record Provider(String type, String pluginId, String minPluginVersion) {
    }

    /**
     * 合约指针（schema_url + sha256）。
     *
     * <p>注意：schemaUrl 为相对路径（不带 host）。
     */
    public record Contract(String schemaUrl, String sha256) {
    }
}
