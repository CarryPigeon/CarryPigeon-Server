package team.carrypigeon.backend.infrastructure.basic.plugin.manifest;

import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;
import org.yaml.snakeyaml.Yaml;

/**
 * 启动期插件 Manifest 加载器。
 * 职责：枚举 classpath 中的 Manifest，解析并校验插件身份、宿主兼容性和依赖关系。
 * 边界：不创建 Spring Bean，不执行插件业务，也不负责运行期生命周期。
 */
public final class PluginManifestLoader {

    public static final String MANIFEST_RESOURCE = "META-INF/carrypigeon/plugin.yaml";
    private static final String PLUGIN_ID_PATTERN = "[a-z0-9]+(?:-[a-z0-9]+)*";
    private static final String SEMANTIC_VERSION_PATTERN =
            "(?:0|[1-9][0-9]*)\\.(?:0|[1-9][0-9]*)\\.(?:0|[1-9][0-9]*)"
                    + "(?:-[0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*)?"
                    + "(?:\\+[0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*)?";

    private PluginManifestLoader() {
    }

    /**
     * 从默认应用 ClassLoader 读取并校验所有插件 Manifest。
     *
     * @param classLoader JVM 启动时的应用 ClassLoader
     * @param hostIdentity 当前宿主身份
     * @return 已校验的 Manifest 快照
     */
    public static PluginManifestCatalog load(ClassLoader classLoader, PluginHostIdentity hostIdentity) {
        List<PluginManifest> manifests = new ArrayList<>();
        try {
            Enumeration<URL> resources = classLoader.getResources(MANIFEST_RESOURCE);
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                manifests.add(read(resource, classLoader));
            }
        } catch (IOException exception) {
            throw new PluginManifestException("failed to enumerate plugin manifests", exception);
        }
        validateAll(manifests, hostIdentity, classLoader);
        return new PluginManifestCatalog(manifests);
    }

    private static PluginManifest read(URL resource, ClassLoader classLoader) {
        try (InputStream input = resource.openStream()) {
            Object loaded = new Yaml().load(input);
            Map<String, Object> root = map(loaded, "manifest root");
            Map<String, Object> host = map(root.get("host"), "host");
            Map<String, Object> entrypoint = map(root.get("entrypoint"), "entrypoint");
            Map<String, Object> dependencies = map(root.get("dependencies"), "dependencies");
            Map<String, Object> extensions = map(root.get("extensions"), "extensions");
            Map<String, Object> configuration = map(root.get("configuration"), "configuration");
            return new PluginManifest(
                    string(root, "plugin_id"),
                    string(root, "name"),
                    string(root, "version"),
                    string(root, "publisher"),
                    string(root, "trust_level"),
                    string(root, "load_mode"),
                    new PluginManifest.PluginHostRequirement(
                            string(host, "version"),
                            string(host, "build_hash"),
                            string(host, "java_version"),
                            string(host, "spring_boot_version")
                    ),
                    stringList(entrypoint.get("auto_configuration")),
                    stringList(dependencies.get("required_plugins")),
                    stringList(dependencies.get("optional_plugins")),
                    stringList(dependencies.get("required_host_artifacts")),
                    booleanValue(extensions, "database_migrations"),
                    string(configuration, "prefix"),
                    resource.toExternalForm(),
                    sha256(resource)
            );
        } catch (PluginManifestException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new PluginManifestException("failed to parse plugin manifest: " + resource, exception);
        }
    }

    private static void validateAll(
            List<PluginManifest> manifests,
            PluginHostIdentity host,
            ClassLoader classLoader
    ) {
        Map<String, PluginManifest> byId = new HashMap<>();
        for (PluginManifest manifest : manifests) {
            if (!manifest.pluginId().matches(PLUGIN_ID_PATTERN)) {
                throw new PluginManifestException("invalid plugin_id: " + manifest.pluginId());
            }
            if (!manifest.version().matches(SEMANTIC_VERSION_PATTERN)) {
                throw new PluginManifestException("plugin version must use semantic versioning: "
                        + manifest.pluginId() + ": " + manifest.version());
            }
            if (!"SYSTEM".equals(manifest.trustLevel())) {
                throw new PluginManifestException("plugin must use SYSTEM trust_level: " + manifest.pluginId());
            }
            if (!"STARTUP_CLASSPATH".equals(manifest.loadMode())) {
                throw new PluginManifestException("plugin must use STARTUP_CLASSPATH load_mode: " + manifest.pluginId());
            }
            if (byId.putIfAbsent(manifest.pluginId(), manifest) != null) {
                throw new PluginManifestException("duplicate plugin_id: " + manifest.pluginId());
            }
            validateManifestCollections(manifest);
            validateHost(manifest, host);
            validateEntrypoints(manifest, classLoader);
            String expectedPrefix = "cp.plugin.configs." + manifest.pluginId();
            if (!expectedPrefix.equals(manifest.configurationPrefix())) {
                throw new PluginManifestException("configuration.prefix must be " + expectedPrefix
                        + ": " + manifest.pluginId());
            }
        }
        for (PluginManifest manifest : manifests) {
            for (String dependency : manifest.requiredPluginIds()) {
                if (!byId.containsKey(dependency)) {
                    throw new PluginManifestException("missing required plugin " + dependency
                            + " for " + manifest.pluginId());
                }
            }
        }
        detectCycles(manifests, byId);
        PluginClasspathInspector.validate(classLoader, manifests);
    }

    private static void validateHost(PluginManifest manifest, PluginHostIdentity host) {
        PluginManifest.PluginHostRequirement requirement = manifest.host();
        requireMatch(manifest, "host.version", requirement.version(), host.version());
        requireMatch(manifest, "host.build_hash", requirement.buildHash(), host.buildHash());
        requireMatch(manifest, "host.java_version", requirement.javaVersion(), host.javaVersion());
        requireMatch(manifest, "host.spring_boot_version", requirement.springBootVersion(), host.springBootVersion());
    }

    private static void requireMatch(PluginManifest manifest, String field, String expected, String actual) {
        if (expected == null || expected.isBlank()) {
            throw new PluginManifestException(field + " must not be blank: " + manifest.pluginId());
        }
        if (!expected.equals(actual)) {
            throw new PluginManifestException(field + " mismatch for " + manifest.pluginId()
                    + ": expected=" + expected + ", actual=" + actual);
        }
    }

    private static void validateEntrypoints(PluginManifest manifest, ClassLoader classLoader) {
        if (manifest.autoConfigurationClasses().isEmpty()) {
            throw new PluginManifestException("entrypoint.auto_configuration must not be empty: " + manifest.pluginId());
        }
        for (String className : manifest.autoConfigurationClasses()) {
            try {
                Class.forName(className, false, classLoader);
            } catch (ClassNotFoundException | LinkageError exception) {
                throw new PluginManifestException("plugin AutoConfiguration class not found: " + className, exception);
            }
        }
    }

    private static void validateManifestCollections(PluginManifest manifest) {
        requireUnique(manifest.autoConfigurationClasses(), "entrypoint.auto_configuration", manifest.pluginId());
        requireUnique(manifest.requiredPluginIds(), "dependencies.required_plugins", manifest.pluginId());
        requireUnique(manifest.optionalPluginIds(), "dependencies.optional_plugins", manifest.pluginId());
        requireUnique(manifest.requiredHostArtifacts(), "dependencies.required_host_artifacts", manifest.pluginId());
        for (String dependency : manifest.requiredPluginIds()) {
            validateDependencyId(manifest, dependency, "required_plugins");
        }
        for (String dependency : manifest.optionalPluginIds()) {
            validateDependencyId(manifest, dependency, "optional_plugins");
        }
        Set<String> overlap = new HashSet<>(manifest.requiredPluginIds());
        overlap.retainAll(manifest.optionalPluginIds());
        if (!overlap.isEmpty()) {
            throw new PluginManifestException("plugin dependency cannot be both required and optional: "
                    + manifest.pluginId() + ": " + overlap);
        }
    }

    private static void validateDependencyId(PluginManifest manifest, String dependency, String field) {
        if (!dependency.matches(PLUGIN_ID_PATTERN) || dependency.equals(manifest.pluginId())) {
            throw new PluginManifestException("invalid plugin dependency in " + field + ": "
                    + manifest.pluginId() + ": " + dependency);
        }
    }

    private static void requireUnique(List<String> values, String field, String pluginId) {
        if (new HashSet<>(values).size() != values.size()) {
            throw new PluginManifestException(field + " must not contain duplicates: " + pluginId);
        }
    }

    private static void detectCycles(List<PluginManifest> manifests, Map<String, PluginManifest> byId) {
        Map<String, VisitState> states = new HashMap<>();
        for (PluginManifest manifest : manifests) {
            visit(manifest.pluginId(), byId, states, new HashSet<>());
        }
    }

    private static void visit(
            String pluginId,
            Map<String, PluginManifest> byId,
            Map<String, VisitState> states,
            Set<String> path
    ) {
        VisitState state = states.get(pluginId);
        if (state == VisitState.VISITED) {
            return;
        }
        if (state == VisitState.VISITING || !path.add(pluginId)) {
            throw new PluginManifestException("plugin dependency cycle detected at: " + pluginId);
        }
        states.put(pluginId, VisitState.VISITING);
        for (String dependency : byId.get(pluginId).requiredPluginIds()) {
            visit(dependency, byId, states, path);
        }
        path.remove(pluginId);
        states.put(pluginId, VisitState.VISITED);
    }

    private static String sha256(URL resource) {
        try {
            Path jarPath = jarPath(resource);
            if (jarPath == null || !Files.isRegularFile(jarPath)) {
                return "";
            }
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = Files.newInputStream(jarPath)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) >= 0) {
                    digest.update(buffer, 0, read);
                }
            }
            StringBuilder result = new StringBuilder();
            for (byte value : digest.digest()) {
                result.append(String.format("%02x", value));
            }
            return result.toString();
        } catch (IOException | NoSuchAlgorithmException exception) {
            throw new PluginManifestException("failed to calculate plugin jar checksum: " + resource, exception);
        }
    }

    private static Path jarPath(URL resource) {
        try {
            if ("jar".equals(resource.getProtocol())) {
                return Path.of(((JarURLConnection) resource.openConnection()).getJarFileURL().toURI());
            }
            if ("file".equals(resource.getProtocol())) {
                return Path.of(URI.create(resource.toExternalForm()));
            }
            return null;
        } catch (Exception exception) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object value, String name) {
        if (value == null) {
            return Map.of();
        }
        if (!(value instanceof Map<?, ?> source)) {
            throw new PluginManifestException(name + " must be an object");
        }
        Map<String, Object> result = new HashMap<>();
        source.forEach((key, item) -> result.put(String.valueOf(key), item));
        return result;
    }

    private static String string(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return "";
        }
        return String.valueOf(value).trim();
    }

    private static boolean booleanValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null && Boolean.parseBoolean(String.valueOf(value));
    }

    private static List<String> stringList(Object value) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).map(String::trim).filter(item -> !item.isBlank()).toList();
        }
        return List.of(String.valueOf(value).trim());
    }

    private enum VisitState {
        VISITING,
        VISITED
    }
}
