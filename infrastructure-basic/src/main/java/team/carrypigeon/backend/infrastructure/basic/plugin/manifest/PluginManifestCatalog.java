package team.carrypigeon.backend.infrastructure.basic.plugin.manifest;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 本次 JVM classpath 中已经通过预检的插件 Manifest 快照。
 * 职责：向启动协调和插件运行时提供稳定的插件描述查询。
 * 边界：只保存描述，不动态加载、卸载或替换插件代码。
 */
public final class PluginManifestCatalog {

    private final List<PluginManifest> manifests;
    private final Map<String, PluginManifest> manifestsById;

    public PluginManifestCatalog(List<PluginManifest> manifests) {
        Map<String, PluginManifest> byId = new LinkedHashMap<>();
        for (PluginManifest manifest : manifests == null ? List.<PluginManifest>of() : manifests) {
            if (byId.putIfAbsent(manifest.pluginId(), manifest) != null) {
                throw new IllegalArgumentException("duplicate plugin id: " + manifest.pluginId());
            }
        }
        this.manifestsById = Map.copyOf(byId);
        this.manifests = List.copyOf(byId.values());
    }

    public static PluginManifestCatalog empty() {
        return new PluginManifestCatalog(List.of());
    }

    public List<PluginManifest> manifests() {
        return manifests;
    }

    public Optional<PluginManifest> find(String pluginId) {
        return Optional.ofNullable(manifestsById.get(pluginId));
    }

    public PluginManifest require(String pluginId) {
        return find(pluginId).orElseThrow(() -> new IllegalArgumentException("plugin manifest not found: " + pluginId));
    }
}
