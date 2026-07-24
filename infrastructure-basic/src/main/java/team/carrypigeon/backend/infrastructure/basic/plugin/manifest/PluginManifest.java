package team.carrypigeon.backend.infrastructure.basic.plugin.manifest;

import java.util.List;

/**
 * 插件包 Manifest 的只读模型。
 * 职责：表达启动期插件的身份、宿主兼容性、依赖和 Spring 入口。
 * 边界：不承载插件实例、权限执行或业务配置解释。
 */
public record PluginManifest(
        String pluginId,
        String name,
        String version,
        String publisher,
        String trustLevel,
        String loadMode,
        PluginHostRequirement host,
        List<String> autoConfigurationClasses,
        List<String> requiredPluginIds,
        List<String> optionalPluginIds,
        List<String> requiredHostArtifacts,
        boolean databaseMigrations,
        String configurationPrefix,
        String source,
        String sha256
) {

    public PluginManifest {
        pluginId = require(pluginId, "pluginId");
        name = require(name, "name");
        version = require(version, "version");
        publisher = require(publisher, "publisher");
        trustLevel = require(trustLevel, "trustLevel");
        loadMode = require(loadMode, "loadMode");
        host = host == null ? new PluginHostRequirement("", "", "", "") : host;
        autoConfigurationClasses = List.copyOf(autoConfigurationClasses == null ? List.of() : autoConfigurationClasses);
        requiredPluginIds = List.copyOf(requiredPluginIds == null ? List.of() : requiredPluginIds);
        optionalPluginIds = List.copyOf(optionalPluginIds == null ? List.of() : optionalPluginIds);
        requiredHostArtifacts = List.copyOf(requiredHostArtifacts == null ? List.of() : requiredHostArtifacts);
        configurationPrefix = configurationPrefix == null ? "" : configurationPrefix.trim();
        source = source == null ? "classpath" : source;
        sha256 = sha256 == null ? "" : sha256;
    }

    private static String require(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.trim();
    }

    /**
     * Manifest 中的宿主兼容性要求。
     */
    public record PluginHostRequirement(
            String version,
            String buildHash,
            String javaVersion,
            String springBootVersion
    ) {

        public PluginHostRequirement {
            version = version == null ? "" : version.trim();
            buildHash = buildHash == null ? "" : buildHash.trim();
            javaVersion = javaVersion == null ? "" : javaVersion.trim();
            springBootVersion = springBootVersion == null ? "" : springBootVersion.trim();
        }
    }
}
