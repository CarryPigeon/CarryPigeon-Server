package team.carrypigeon.backend.infrastructure.basic.plugin.manifest;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * 插件启动 classpath 冲突检查器。
 * 职责：验证插件入口归属、Boot 自动配置元数据、宿主构件约束、重复类和依赖版本冲突。
 * 边界：只读取本次 JVM 已有 classpath 和插件 JAR，不创建 ClassLoader、不修改 classpath，也不加载 Spring Context。
 */
final class PluginClasspathInspector {

    static final String AUTO_CONFIGURATION_IMPORTS =
            "META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports";
    private static final List<String> FORBIDDEN_CLASS_PREFIXES = List.of(
            "team/carrypigeon/backend/",
            "org/springframework/",
            "com/fasterxml/jackson/",
            "org/apache/logging/log4j/",
            "org/slf4j/",
            "com/mysql/cj/"
    );
    private static final Set<String> FORBIDDEN_ROOT_RESOURCES = Set.of(
            "application.yaml",
            "application.yml",
            "application.properties",
            "bootstrap.yaml",
            "bootstrap.yml",
            "bootstrap.properties",
            "log4j2.xml",
            "log4j2-spring.xml",
            "logback.xml",
            "logback-spring.xml"
    );

    private PluginClasspathInspector() {
    }

    /**
     * 校验本次启动发现的全部插件及其共享 classpath。
     *
     * @param classLoader JVM 默认应用 ClassLoader
     * @param manifests 已完成基础字段解析的插件 Manifest
     */
    static void validate(ClassLoader classLoader, List<PluginManifest> manifests) {
        if (manifests.isEmpty()) {
            return;
        }
        Map<String, PluginJar> pluginJars = resolvePluginJars(manifests);
        for (PluginManifest manifest : manifests) {
            validatePluginJar(classLoader, manifest, pluginJars.get(manifest.pluginId()));
        }
        validateMavenCoordinates(classLoader, manifests, pluginJars.values());
    }

    private static Map<String, PluginJar> resolvePluginJars(List<PluginManifest> manifests) {
        Map<String, PluginJar> pluginJars = new LinkedHashMap<>();
        for (PluginManifest manifest : manifests) {
            Path path = pluginJarPath(manifest);
            if (!Files.isRegularFile(path) || !path.getFileName().toString().endsWith(".jar")) {
                throw new PluginManifestException("plugin manifest must be packaged in a jar: "
                        + manifest.pluginId() + ": " + manifest.source());
            }
            String actualChecksum = sha256(path);
            if (manifest.sha256().isBlank() || !manifest.sha256().equals(actualChecksum)) {
                throw new PluginManifestException("plugin jar changed during preflight: "
                        + manifest.pluginId() + ": " + path);
            }
            pluginJars.put(manifest.pluginId(), new PluginJar(path, pathIdentity(path)));
        }
        return pluginJars;
    }

    private static Path pluginJarPath(PluginManifest manifest) {
        try {
            URL source = URI.create(manifest.source()).toURL();
            if (!"jar".equals(source.getProtocol())) {
                throw new PluginManifestException("plugin manifest must use jar protocol: "
                        + manifest.pluginId() + ": " + manifest.source());
            }
            JarURLConnection connection = (JarURLConnection) source.openConnection();
            connection.setUseCaches(false);
            return Path.of(connection.getJarFileURL().toURI()).toAbsolutePath().normalize();
        } catch (PluginManifestException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new PluginManifestException("failed to resolve plugin jar: " + manifest.pluginId(), exception);
        }
    }

    private static void validatePluginJar(
            ClassLoader classLoader,
            PluginManifest manifest,
            PluginJar pluginJar
    ) {
        try (JarFile jarFile = new JarFile(pluginJar.path().toFile())) {
            Set<String> entries = jarFile.stream()
                    .filter(entry -> !entry.isDirectory())
                    .map(JarEntry::getName)
                    .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
            validateAutoConfigurationMetadata(jarFile, manifest, entries);
            validateForbiddenContent(manifest, entries);
            validateClassOwnershipAndDuplicates(classLoader, manifest, pluginJar, entries);
        } catch (PluginManifestException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new PluginManifestException("failed to inspect plugin jar: "
                    + manifest.pluginId() + ": " + pluginJar.path(), exception);
        }
    }

    private static void validateAutoConfigurationMetadata(
            JarFile jarFile,
            PluginManifest manifest,
            Set<String> entries
    ) throws IOException {
        if (!entries.contains(AUTO_CONFIGURATION_IMPORTS)) {
            throw new PluginManifestException("plugin AutoConfiguration imports metadata is missing: "
                    + manifest.pluginId());
        }
        Set<String> declaredImports = new LinkedHashSet<>();
        try (InputStream input = jarFile.getInputStream(jarFile.getJarEntry(AUTO_CONFIGURATION_IMPORTS))) {
            String content = new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            content.lines()
                    .map(String::trim)
                    .filter(line -> !line.isBlank() && !line.startsWith("#"))
                    .forEach(declaredImports::add);
        }
        for (String className : manifest.autoConfigurationClasses()) {
            if (!declaredImports.contains(className)) {
                throw new PluginManifestException("plugin AutoConfiguration is absent from imports metadata: "
                        + manifest.pluginId() + ": " + className);
            }
        }
    }

    private static void validateForbiddenContent(PluginManifest manifest, Set<String> entries) {
        for (String entry : entries) {
            if (FORBIDDEN_ROOT_RESOURCES.contains(entry)) {
                throw new PluginManifestException("plugin jar contains forbidden root configuration resource: "
                        + manifest.pluginId() + ": " + entry);
            }
            String classResource = runtimeClassResource(entry);
            if (classResource == null) {
                continue;
            }
            for (String prefix : FORBIDDEN_CLASS_PREFIXES) {
                if (classResource.startsWith(prefix)) {
                    throw new PluginManifestException("plugin jar contains forbidden host or shared dependency class: "
                            + manifest.pluginId() + ": " + entry);
                }
            }
        }
    }

    private static void validateClassOwnershipAndDuplicates(
            ClassLoader classLoader,
            PluginManifest manifest,
            PluginJar pluginJar,
            Set<String> entries
    ) throws IOException {
        Set<String> pluginClasses = entries.stream().map(PluginClasspathInspector::runtimeClassResource)
                .filter(java.util.Objects::nonNull).collect(
                java.util.stream.Collectors.toCollection(LinkedHashSet::new)
        );
        for (String className : manifest.autoConfigurationClasses()) {
            String resource = className.replace('.', '/') + ".class";
            if (!pluginClasses.contains(resource)) {
                throw new PluginManifestException("plugin AutoConfiguration class is not packaged by its own jar: "
                        + manifest.pluginId() + ": " + className);
            }
        }
        for (String resource : pluginClasses) {
            Set<String> origins = resourceOrigins(classLoader, resource);
            if (!origins.contains(pluginJar.identity())) {
                throw new PluginManifestException("plugin class is not visible from its declaring jar: "
                        + manifest.pluginId() + ": " + resource + ": " + origins);
            }
            if (origins.size() > 1) {
                throw new PluginManifestException("duplicate plugin class on classpath: "
                        + resource + ": " + origins);
            }
        }
    }

    private static String runtimeClassResource(String entry) {
        if (!entry.endsWith(".class")) {
            return null;
        }
        String resource = entry;
        if (entry.startsWith("META-INF/versions/")) {
            String remainder = entry.substring("META-INF/versions/".length());
            int separator = remainder.indexOf('/');
            if (separator < 1 || separator == remainder.length() - 1) {
                return null;
            }
            resource = remainder.substring(separator + 1);
        }
        return resource.equals("module-info.class") ? null : resource;
    }

    private static Set<String> resourceOrigins(ClassLoader classLoader, String resource) throws IOException {
        Set<String> origins = new LinkedHashSet<>();
        Enumeration<URL> resources = classLoader.getResources(resource);
        while (resources.hasMoreElements()) {
            origins.add(resourceOrigin(resources.nextElement(), resource));
        }
        return origins;
    }

    private static String resourceOrigin(URL resourceUrl, String resourceName) {
        try {
            if ("jar".equals(resourceUrl.getProtocol())) {
                JarURLConnection connection = (JarURLConnection) resourceUrl.openConnection();
                connection.setUseCaches(false);
                return pathIdentity(Path.of(connection.getJarFileURL().toURI()));
            }
            if ("file".equals(resourceUrl.getProtocol())) {
                Path path = Path.of(resourceUrl.toURI()).toAbsolutePath().normalize();
                int segments = resourceName.split("/").length;
                for (int index = 0; index < segments; index++) {
                    path = path.getParent();
                }
                return pathIdentity(path);
            }
            return resourceUrl.toExternalForm();
        } catch (Exception exception) {
            return resourceUrl.toExternalForm();
        }
    }

    private static void validateMavenCoordinates(
            ClassLoader classLoader,
            List<PluginManifest> manifests,
            java.util.Collection<PluginJar> pluginJars
    ) {
        Set<String> pluginPaths = pluginJars.stream()
                .map(PluginJar::identity)
                .collect(java.util.stream.Collectors.toSet());
        Map<ArtifactKey, Map<String, Set<String>>> inventory = new LinkedHashMap<>();
        Set<Path> classpathEntries = classpathEntries(classLoader);
        pluginJars.stream().map(PluginJar::path).forEach(classpathEntries::add);
        for (Path entry : classpathEntries) {
            for (MavenArtifact artifact : readMavenArtifacts(entry)) {
                inventory.computeIfAbsent(artifact.key(), ignored -> new LinkedHashMap<>())
                        .computeIfAbsent(artifact.version(), ignored -> new LinkedHashSet<>())
                        .add(pathIdentity(entry));
            }
        }
        for (Map.Entry<ArtifactKey, Map<String, Set<String>>> entry : inventory.entrySet()) {
            if (entry.getValue().size() > 1) {
                throw new PluginManifestException("conflicting Maven artifact versions on classpath: "
                        + entry.getKey() + ": " + entry.getValue());
            }
        }
        Set<MavenArtifact> hostArtifacts = new HashSet<>();
        inventory.forEach((key, versions) -> versions.forEach((version, origins) -> {
            if (origins.stream().anyMatch(origin -> !pluginPaths.contains(origin))) {
                hostArtifacts.add(new MavenArtifact(key, version));
            }
        }));
        for (PluginManifest manifest : manifests) {
            for (String required : manifest.requiredHostArtifacts()) {
                MavenArtifact artifact = parseRequiredHostArtifact(manifest, required);
                if (!hostArtifacts.contains(artifact)) {
                    throw new PluginManifestException("required host artifact is missing: "
                            + manifest.pluginId() + ": " + required);
                }
            }
        }
    }

    private static MavenArtifact parseRequiredHostArtifact(PluginManifest manifest, String value) {
        String[] segments = value.split(":", -1);
        if (segments.length != 3 || Arrays.stream(segments).anyMatch(String::isBlank)) {
            throw new PluginManifestException("invalid required_host_artifacts coordinate: "
                    + manifest.pluginId() + ": " + value);
        }
        return new MavenArtifact(new ArtifactKey(segments[0].trim(), segments[1].trim()), segments[2].trim());
    }

    private static Set<Path> classpathEntries(ClassLoader classLoader) {
        Set<Path> entries = new LinkedHashSet<>();
        String rawClasspath = System.getProperty("java.class.path", "");
        for (String item : rawClasspath.split(java.util.regex.Pattern.quote(File.pathSeparator))) {
            addClasspathItem(entries, item);
        }
        for (ClassLoader current = classLoader; current != null; current = current.getParent()) {
            if (current instanceof URLClassLoader urlClassLoader) {
                for (URL url : urlClassLoader.getURLs()) {
                    if ("file".equals(url.getProtocol())) {
                        try {
                            addClasspathPath(entries, Path.of(url.toURI()));
                        } catch (Exception ignored) {
                            // 非文件 URL 不属于可扫描的启动构件。
                        }
                    }
                }
            }
        }
        return entries;
    }

    private static void addClasspathItem(Set<Path> entries, String item) {
        if (item == null || item.isBlank()) {
            return;
        }
        if (item.endsWith("*")) {
            Path directory = Path.of(item.substring(0, item.length() - 1));
            if (Files.isDirectory(directory)) {
                try (var children = Files.list(directory)) {
                    children.filter(path -> path.getFileName().toString().endsWith(".jar"))
                            .forEach(path -> addClasspathPath(entries, path));
                } catch (IOException exception) {
                    throw new PluginManifestException("failed to enumerate classpath directory: " + directory, exception);
                }
            }
            return;
        }
        addClasspathPath(entries, Path.of(item));
    }

    private static void addClasspathPath(Set<Path> entries, Path path) {
        Path normalized = path.toAbsolutePath().normalize();
        if (Files.isRegularFile(normalized) || Files.isDirectory(normalized)) {
            entries.add(normalized);
        }
    }

    private static List<MavenArtifact> readMavenArtifacts(Path classpathEntry) {
        if (Files.isRegularFile(classpathEntry) && classpathEntry.getFileName().toString().endsWith(".jar")) {
            return readJarMavenArtifacts(classpathEntry);
        }
        if (Files.isDirectory(classpathEntry)) {
            return readDirectoryMavenArtifacts(classpathEntry);
        }
        return List.of();
    }

    private static List<MavenArtifact> readJarMavenArtifacts(Path jarPath) {
        List<MavenArtifact> artifacts = new ArrayList<>();
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            List<JarEntry> propertiesEntries = jarFile.stream()
                    .filter(entry -> !entry.isDirectory())
                    .filter(entry -> entry.getName().startsWith("META-INF/maven/"))
                    .filter(entry -> entry.getName().endsWith("/pom.properties"))
                    .toList();
            for (JarEntry entry : propertiesEntries) {
                try (InputStream input = jarFile.getInputStream(entry)) {
                    readMavenArtifact(input).ifPresent(artifacts::add);
                }
            }
            String fileName = jarPath.getFileName().toString();
            List<MavenArtifact> primaryArtifacts = artifacts.stream()
                    .filter(artifact -> fileName.startsWith(
                            artifact.key().artifactId() + "-" + artifact.version()
                    ))
                    .toList();
            if (!primaryArtifacts.isEmpty()) {
                return primaryArtifacts;
            }
            return artifacts.size() == 1 ? artifacts : List.of();
        } catch (IOException exception) {
            throw new PluginManifestException("failed to inspect Maven metadata: " + jarPath, exception);
        }
    }

    private static List<MavenArtifact> readDirectoryMavenArtifacts(Path directory) {
        Path mavenRoot = directory.resolve("META-INF/maven");
        if (!Files.isDirectory(mavenRoot)) {
            return List.of();
        }
        List<MavenArtifact> artifacts = new ArrayList<>();
        try (var paths = Files.walk(mavenRoot)) {
            for (Path path : paths.filter(Files::isRegularFile)
                    .filter(item -> item.getFileName().toString().equals("pom.properties")).toList()) {
                try (InputStream input = Files.newInputStream(path)) {
                    readMavenArtifact(input).ifPresent(artifacts::add);
                }
            }
            return artifacts;
        } catch (IOException exception) {
            throw new PluginManifestException("failed to inspect Maven metadata directory: " + directory, exception);
        }
    }

    private static java.util.Optional<MavenArtifact> readMavenArtifact(InputStream input) throws IOException {
        Properties properties = new Properties();
        properties.load(input);
        String groupId = properties.getProperty("groupId", "").trim();
        String artifactId = properties.getProperty("artifactId", "").trim();
        String version = properties.getProperty("version", "").trim();
        if (groupId.isBlank() || artifactId.isBlank() || version.isBlank()) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(new MavenArtifact(new ArtifactKey(groupId, artifactId), version));
    }

    private static String sha256(Path path) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = Files.newInputStream(path)) {
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
            throw new PluginManifestException("failed to calculate plugin jar checksum: " + path, exception);
        }
    }

    private static String pathIdentity(Path path) {
        try {
            return path.toRealPath().toString();
        } catch (IOException exception) {
            return path.toAbsolutePath().normalize().toString();
        }
    }

    private record PluginJar(Path path, String identity) {
    }

    private record ArtifactKey(String groupId, String artifactId) {

        @Override
        public String toString() {
            return groupId + ":" + artifactId;
        }
    }

    private record MavenArtifact(ArtifactKey key, String version) {
    }
}
