package team.carrypigeon.backend.infrastructure.basic.plugin.manifest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 插件预检测试 JAR 工厂。
 * 职责：在临时目录编译隔离类并生成包含 Manifest、Boot imports 和 Maven 元数据的真实 JAR。
 * 边界：仅供测试构造输入，不参与正式插件打包。
 */
final class PluginTestJarFactory {

    private final Path root;
    private final AtomicInteger sequence = new AtomicInteger();

    PluginTestJarFactory(Path root) {
        this.root = root;
    }

    Path createPlugin(PluginJarSpec spec) throws IOException {
        int index = sequence.incrementAndGet();
        Path workDirectory = Files.createDirectories(root.resolve("plugin-" + index));
        Map<String, String> sources = new LinkedHashMap<>(spec.extraSources());
        if (spec.packageEntrypoint()) {
            sources.putIfAbsent(spec.autoConfigurationClass(), source(spec.autoConfigurationClass()));
        }
        Path classes = compile(workDirectory, sources);
        Path jar = workDirectory.resolve(spec.pluginId() + "-" + spec.version() + "-" + index + ".jar");
        try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(jar))) {
            addString(output, PluginManifestLoader.MANIFEST_RESOURCE, manifest(spec));
            if (spec.includeImportsMetadata()) {
                addString(output, PluginClasspathInspector.AUTO_CONFIGURATION_IMPORTS,
                        String.join(System.lineSeparator(), spec.importedAutoConfigurations())
                                + System.lineSeparator());
            }
            addClasses(output, classes);
            addMavenMetadata(output, spec.groupId(), spec.artifactId(), spec.version());
            for (Map.Entry<String, String> resource : spec.extraResources().entrySet()) {
                addString(output, resource.getKey(), resource.getValue());
            }
        }
        return jar;
    }

    Path createLibrary(
            String fileName,
            String groupId,
            String artifactId,
            String version,
            Map<String, String> sources
    ) throws IOException {
        int index = sequence.incrementAndGet();
        Path workDirectory = Files.createDirectories(root.resolve("library-" + index));
        Path classes = compile(workDirectory, sources);
        Path jar = workDirectory.resolve(fileName);
        try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(jar))) {
            addClasses(output, classes);
            addMavenMetadata(output, groupId, artifactId, version);
        }
        return jar;
    }

    private Path compile(Path workDirectory, Map<String, String> sources) throws IOException {
        Path classes = Files.createDirectories(workDirectory.resolve("classes"));
        if (sources.isEmpty()) {
            return classes;
        }
        Path sourceRoot = Files.createDirectories(workDirectory.resolve("sources"));
        List<Path> sourceFiles = new java.util.ArrayList<>();
        for (Map.Entry<String, String> entry : sources.entrySet()) {
            Path sourceFile = sourceRoot.resolve(entry.getKey().replace('.', '/') + ".java");
            Files.createDirectories(sourceFile.getParent());
            Files.writeString(sourceFile, entry.getValue(), StandardCharsets.UTF_8);
            sourceFiles.add(sourceFile);
        }
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "plugin preflight tests require a JDK");
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null)) {
            Iterable<? extends JavaFileObject> units = fileManager.getJavaFileObjectsFromPaths(sourceFiles);
            List<String> options = List.of("-proc:none", "-d", classes.toString());
            boolean compiled = compiler.getTask(null, fileManager, diagnostics, options, null, units).call();
            assertTrue(compiled, () -> diagnostics.getDiagnostics().stream()
                    .map(Object::toString)
                    .reduce((left, right) -> left + System.lineSeparator() + right)
                    .orElse("plugin test source compilation failed without diagnostics"));
        }
        return classes;
    }

    private void addClasses(JarOutputStream output, Path classes) throws IOException {
        try (var paths = Files.walk(classes)) {
            for (Path path : paths.filter(Files::isRegularFile).sorted().toList()) {
                String entryName = classes.relativize(path).toString().replace('\\', '/');
                addBytes(output, entryName, Files.readAllBytes(path));
            }
        }
    }

    private void addMavenMetadata(
            JarOutputStream output,
            String groupId,
            String artifactId,
            String version
    ) throws IOException {
        String properties = "groupId=" + groupId + System.lineSeparator()
                + "artifactId=" + artifactId + System.lineSeparator()
                + "version=" + version + System.lineSeparator();
        addString(output, "META-INF/maven/" + groupId + "/" + artifactId + "/pom.properties", properties);
    }

    private void addString(JarOutputStream output, String name, String content) throws IOException {
        addBytes(output, name, content.getBytes(StandardCharsets.UTF_8));
    }

    private void addBytes(JarOutputStream output, String name, byte[] content) throws IOException {
        output.putNextEntry(new JarEntry(name));
        output.write(content);
        output.closeEntry();
    }

    private String manifest(PluginJarSpec spec) {
        String requiredPlugins = yamlList(spec.requiredPlugins());
        String requiredHostArtifacts = yamlList(spec.requiredHostArtifacts());
        return """
                plugin_id: %s
                name: Test Plugin
                version: %s
                publisher: test
                trust_level: SYSTEM
                load_mode: STARTUP_CLASSPATH
                host:
                  version: 1.0.0
                  build_hash: %s
                  java_version: 21
                  spring_boot_version: 3.5.3
                entrypoint:
                  auto_configuration:
                    - %s
                dependencies:
                  required_plugins: %s
                  optional_plugins: []
                  required_host_artifacts: %s
                extensions:
                  database_migrations: false
                configuration:
                  prefix: cp.plugin.configs.%s
                """.formatted(
                spec.pluginId(),
                spec.version(),
                spec.hostBuildHash(),
                spec.autoConfigurationClass(),
                requiredPlugins,
                requiredHostArtifacts,
                spec.pluginId()
        );
    }

    private String yamlList(List<String> values) {
        return values.isEmpty() ? "[]" : "[" + String.join(", ", values) + "]";
    }

    static String source(String className) {
        int separator = className.lastIndexOf('.');
        String packageName = separator < 0 ? "" : className.substring(0, separator);
        String simpleName = separator < 0 ? className : className.substring(separator + 1);
        String packageLine = packageName.isBlank() ? "" : "package " + packageName + ";";
        return packageLine + System.lineSeparator() + "public final class " + simpleName + " {}";
    }

    record PluginJarSpec(
            String pluginId,
            String version,
            String hostBuildHash,
            String groupId,
            String artifactId,
            String autoConfigurationClass,
            boolean packageEntrypoint,
            boolean includeImportsMetadata,
            List<String> importedAutoConfigurations,
            List<String> requiredPlugins,
            List<String> requiredHostArtifacts,
            Map<String, String> extraSources,
            Map<String, String> extraResources
    ) {

        static PluginJarSpec valid(String pluginId) {
            String suffix = pluginId.replace('-', '.');
            String autoConfiguration = "test.plugin." + suffix + ".AutoConfiguration";
            return new PluginJarSpec(
                    pluginId,
                    "1.0.0",
                    "build-a",
                    "test.plugin",
                    pluginId,
                    autoConfiguration,
                    true,
                    true,
                    List.of(autoConfiguration),
                    List.of(),
                    List.of(),
                    Map.of(),
                    Map.of()
            );
        }

        PluginJarSpec withHostBuildHash(String value) {
            return copy(value, autoConfigurationClass, packageEntrypoint, includeImportsMetadata,
                    importedAutoConfigurations, requiredPlugins, requiredHostArtifacts, extraSources, extraResources);
        }

        PluginJarSpec withVersion(String value) {
            return new PluginJarSpec(
                    pluginId,
                    value,
                    hostBuildHash,
                    groupId,
                    artifactId,
                    autoConfigurationClass,
                    packageEntrypoint,
                    includeImportsMetadata,
                    importedAutoConfigurations,
                    requiredPlugins,
                    requiredHostArtifacts,
                    extraSources,
                    extraResources
            );
        }

        PluginJarSpec withRequiredPlugins(List<String> values) {
            return copy(hostBuildHash, autoConfigurationClass, packageEntrypoint, includeImportsMetadata,
                    importedAutoConfigurations, values, requiredHostArtifacts, extraSources, extraResources);
        }

        PluginJarSpec withRequiredHostArtifacts(List<String> values) {
            return copy(hostBuildHash, autoConfigurationClass, packageEntrypoint, includeImportsMetadata,
                    importedAutoConfigurations, requiredPlugins, values, extraSources, extraResources);
        }

        PluginJarSpec withoutImportsMetadata() {
            return copy(hostBuildHash, autoConfigurationClass, packageEntrypoint, false,
                    importedAutoConfigurations, requiredPlugins, requiredHostArtifacts, extraSources, extraResources);
        }

        PluginJarSpec withImportedAutoConfigurations(List<String> values) {
            return copy(hostBuildHash, autoConfigurationClass, packageEntrypoint, includeImportsMetadata,
                    values, requiredPlugins, requiredHostArtifacts, extraSources, extraResources);
        }

        PluginJarSpec borrowingEntrypoint(String className) {
            return copy(hostBuildHash, className, false, includeImportsMetadata,
                    List.of(className), requiredPlugins, requiredHostArtifacts, extraSources, extraResources);
        }

        PluginJarSpec withExtraSources(Map<String, String> values) {
            return copy(hostBuildHash, autoConfigurationClass, packageEntrypoint, includeImportsMetadata,
                    importedAutoConfigurations, requiredPlugins, requiredHostArtifacts, values, extraResources);
        }

        PluginJarSpec withExtraResources(Map<String, String> values) {
            return copy(hostBuildHash, autoConfigurationClass, packageEntrypoint, includeImportsMetadata,
                    importedAutoConfigurations, requiredPlugins, requiredHostArtifacts, extraSources, values);
        }

        private PluginJarSpec copy(
                String copiedHostBuildHash,
                String copiedAutoConfigurationClass,
                boolean copiedPackageEntrypoint,
                boolean copiedIncludeImportsMetadata,
                List<String> copiedImports,
                List<String> copiedRequiredPlugins,
                List<String> copiedRequiredHostArtifacts,
                Map<String, String> copiedExtraSources,
                Map<String, String> copiedExtraResources
        ) {
            return new PluginJarSpec(
                    pluginId,
                    version,
                    copiedHostBuildHash,
                    groupId,
                    artifactId,
                    copiedAutoConfigurationClass,
                    copiedPackageEntrypoint,
                    copiedIncludeImportsMetadata,
                    copiedImports,
                    copiedRequiredPlugins,
                    copiedRequiredHostArtifacts,
                    copiedExtraSources,
                    copiedExtraResources
            );
        }
    }
}
