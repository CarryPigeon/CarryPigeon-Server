package team.carrypigeon.backend.infrastructure.basic.plugin.manifest;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 插件 Manifest 与 classpath 启动预检测试。
 * 职责：验证合法插件发现、宿主精确绑定、入口归属、依赖图、宿主构件和冲突拒绝契约。
 */
class PluginManifestLoaderTests {

    private static final PluginHostIdentity HOST = new PluginHostIdentity("1.0.0", "build-a", "21", "3.5.3");

    /**
     * 验证包含自身入口、Boot 元数据和精确宿主构件依赖的插件 JAR可以通过预检。
     */
    @Test
    void load_validPluginJar_returnsCatalog(@TempDir Path tempDirectory) throws IOException {
        PluginTestJarFactory factory = new PluginTestJarFactory(tempDirectory);
        Path hostApi = factory.createLibrary("host-api.jar", "test", "host-api", "1.0.0", Map.of());
        Path plugin = factory.createPlugin(PluginTestJarFactory.PluginJarSpec.valid("plugin-a")
                .withRequiredHostArtifacts(List.of("test:host-api:1.0.0")));

        try (URLClassLoader classLoader = classLoader(hostApi, plugin)) {
            PluginManifestCatalog catalog = PluginManifestLoader.load(classLoader, HOST);

            assertEquals(List.of("plugin-a"), catalog.manifests().stream().map(PluginManifest::pluginId).toList());
            assertEquals(List.of("test:host-api:1.0.0"), catalog.manifests().getFirst().requiredHostArtifacts());
            assertTrue(catalog.manifests().getFirst().sha256().matches("[0-9a-f]{64}"));
        }
    }

    /**
     * 验证插件绑定错误宿主 build hash 时拒绝启动。
     */
    @Test
    void load_hostBuildHashMismatch_rejectsStartup(@TempDir Path tempDirectory) throws IOException {
        PluginTestJarFactory factory = new PluginTestJarFactory(tempDirectory);
        Path plugin = factory.createPlugin(PluginTestJarFactory.PluginJarSpec.valid("plugin-a")
                .withHostBuildHash("other"));

        try (URLClassLoader classLoader = classLoader(plugin)) {
            assertThrows(PluginManifestException.class, () -> PluginManifestLoader.load(classLoader, HOST));
        }
    }

    /**
     * 验证插件版本不是语义化版本时拒绝启动。
     */
    @Test
    void load_pluginVersionIsNotSemantic_rejectsStartup(@TempDir Path tempDirectory) throws IOException {
        PluginTestJarFactory factory = new PluginTestJarFactory(tempDirectory);
        Path plugin = factory.createPlugin(PluginTestJarFactory.PluginJarSpec.valid("plugin-a")
                .withVersion("release-one"));

        try (URLClassLoader classLoader = classLoader(plugin)) {
            PluginManifestException exception = assertThrows(
                    PluginManifestException.class,
                    () -> PluginManifestLoader.load(classLoader, HOST)
            );

            assertTrue(exception.getMessage().contains("semantic versioning"));
        }
    }

    /**
     * 验证同一 classpath 出现重复 plugin_id 时拒绝启动。
     */
    @Test
    void load_duplicatePluginId_rejectsStartup(@TempDir Path tempDirectory) throws IOException {
        PluginTestJarFactory factory = new PluginTestJarFactory(tempDirectory);
        Path first = factory.createPlugin(PluginTestJarFactory.PluginJarSpec.valid("plugin-a"));
        Path second = factory.createPlugin(PluginTestJarFactory.PluginJarSpec.valid("plugin-a"));

        try (URLClassLoader classLoader = classLoader(first, second)) {
            assertThrows(PluginManifestException.class, () -> PluginManifestLoader.load(classLoader, HOST));
        }
    }

    /**
     * 验证缺失强依赖插件时拒绝启动。
     */
    @Test
    void load_missingRequiredPlugin_rejectsStartup(@TempDir Path tempDirectory) throws IOException {
        PluginTestJarFactory factory = new PluginTestJarFactory(tempDirectory);
        Path plugin = factory.createPlugin(PluginTestJarFactory.PluginJarSpec.valid("plugin-a")
                .withRequiredPlugins(List.of("plugin-b")));

        try (URLClassLoader classLoader = classLoader(plugin)) {
            assertThrows(PluginManifestException.class, () -> PluginManifestLoader.load(classLoader, HOST));
        }
    }

    /**
     * 验证强依赖列表重复声明同一插件时拒绝启动。
     */
    @Test
    void load_requiredPluginDeclaredTwice_rejectsStartup(@TempDir Path tempDirectory) throws IOException {
        PluginTestJarFactory factory = new PluginTestJarFactory(tempDirectory);
        Path first = factory.createPlugin(PluginTestJarFactory.PluginJarSpec.valid("plugin-a")
                .withRequiredPlugins(List.of("plugin-b", "plugin-b")));
        Path second = factory.createPlugin(PluginTestJarFactory.PluginJarSpec.valid("plugin-b"));

        try (URLClassLoader classLoader = classLoader(first, second)) {
            PluginManifestException exception = assertThrows(
                    PluginManifestException.class,
                    () -> PluginManifestLoader.load(classLoader, HOST)
            );

            assertTrue(exception.getMessage().contains("must not contain duplicates"));
        }
    }

    /**
     * 验证插件强依赖形成环时拒绝启动。
     */
    @Test
    void load_dependencyCycle_rejectsStartup(@TempDir Path tempDirectory) throws IOException {
        PluginTestJarFactory factory = new PluginTestJarFactory(tempDirectory);
        Path first = factory.createPlugin(PluginTestJarFactory.PluginJarSpec.valid("plugin-a")
                .withRequiredPlugins(List.of("plugin-b")));
        Path second = factory.createPlugin(PluginTestJarFactory.PluginJarSpec.valid("plugin-b")
                .withRequiredPlugins(List.of("plugin-a")));

        try (URLClassLoader classLoader = classLoader(first, second)) {
            assertThrows(PluginManifestException.class, () -> PluginManifestLoader.load(classLoader, HOST));
        }
    }

    /**
     * 验证插件缺少 Spring Boot AutoConfiguration imports 元数据时拒绝启动。
     */
    @Test
    void load_missingAutoConfigurationImports_rejectsStartup(@TempDir Path tempDirectory) throws IOException {
        PluginTestJarFactory factory = new PluginTestJarFactory(tempDirectory);
        Path plugin = factory.createPlugin(PluginTestJarFactory.PluginJarSpec.valid("plugin-a")
                .withoutImportsMetadata());

        try (URLClassLoader classLoader = classLoader(plugin)) {
            PluginManifestException exception = assertThrows(
                    PluginManifestException.class,
                    () -> PluginManifestLoader.load(classLoader, HOST)
            );

            assertTrue(exception.getMessage().contains("imports metadata is missing"));
        }
    }

    /**
     * 验证 Manifest 入口未列入插件自身 Boot imports 元数据时拒绝启动。
     */
    @Test
    void load_entrypointAbsentFromImports_rejectsStartup(@TempDir Path tempDirectory) throws IOException {
        PluginTestJarFactory factory = new PluginTestJarFactory(tempDirectory);
        Path plugin = factory.createPlugin(PluginTestJarFactory.PluginJarSpec.valid("plugin-a")
                .withImportedAutoConfigurations(List.of("test.plugin.OtherConfiguration")));

        try (URLClassLoader classLoader = classLoader(plugin)) {
            PluginManifestException exception = assertThrows(
                    PluginManifestException.class,
                    () -> PluginManifestLoader.load(classLoader, HOST)
            );

            assertTrue(exception.getMessage().contains("absent from imports metadata"));
        }
    }

    /**
     * 验证插件不能借用宿主 classpath 中可加载的类冒充自身入口。
     */
    @Test
    void load_entrypointOwnedByHost_rejectsStartup(@TempDir Path tempDirectory) throws IOException {
        PluginTestJarFactory factory = new PluginTestJarFactory(tempDirectory);
        String borrowedClass = "test.host.BorrowedAutoConfiguration";
        Path host = factory.createLibrary(
                "host-entrypoint.jar",
                "test",
                "host-entrypoint",
                "1.0.0",
                Map.of(borrowedClass, PluginTestJarFactory.source(borrowedClass))
        );
        Path plugin = factory.createPlugin(PluginTestJarFactory.PluginJarSpec.valid("plugin-a")
                .borrowingEntrypoint(borrowedClass));

        try (URLClassLoader classLoader = classLoader(host, plugin)) {
            PluginManifestException exception = assertThrows(
                    PluginManifestException.class,
                    () -> PluginManifestLoader.load(classLoader, HOST)
            );

            assertTrue(exception.getMessage().contains("not packaged by its own jar"));
        }
    }

    /**
     * 验证插件 JAR重复打包 CarryPigeon 核心命名空间时拒绝启动。
     */
    @Test
    void load_pluginPackagesCoreClass_rejectsStartup(@TempDir Path tempDirectory) throws IOException {
        PluginTestJarFactory factory = new PluginTestJarFactory(tempDirectory);
        String coreClass = "team.carrypigeon.backend.fake.RepackagedCore";
        Path plugin = factory.createPlugin(PluginTestJarFactory.PluginJarSpec.valid("plugin-a")
                .withExtraSources(Map.of(coreClass, PluginTestJarFactory.source(coreClass))));

        try (URLClassLoader classLoader = classLoader(plugin)) {
            PluginManifestException exception = assertThrows(
                    PluginManifestException.class,
                    () -> PluginManifestLoader.load(classLoader, HOST)
            );

            assertTrue(exception.getMessage().contains("forbidden host or shared dependency class"));
        }
    }

    /**
     * 验证插件 JAR携带另一份 Spring 类时拒绝启动。
     */
    @Test
    void load_pluginPackagesHighRiskSharedClass_rejectsStartup(@TempDir Path tempDirectory) throws IOException {
        PluginTestJarFactory factory = new PluginTestJarFactory(tempDirectory);
        String springClass = "org.springframework.fake.RepackagedSpring";
        Path plugin = factory.createPlugin(PluginTestJarFactory.PluginJarSpec.valid("plugin-a")
                .withExtraSources(Map.of(springClass, PluginTestJarFactory.source(springClass))));

        try (URLClassLoader classLoader = classLoader(plugin)) {
            PluginManifestException exception = assertThrows(
                    PluginManifestException.class,
                    () -> PluginManifestLoader.load(classLoader, HOST)
            );

            assertTrue(exception.getMessage().contains("forbidden host or shared dependency class"));
        }
    }

    /**
     * 验证 multi-release 路径不能绕过核心类前缀检查。
     */
    @Test
    void load_pluginPackagesVersionedCoreClass_rejectsStartup(@TempDir Path tempDirectory) throws IOException {
        PluginTestJarFactory factory = new PluginTestJarFactory(tempDirectory);
        Path plugin = factory.createPlugin(PluginTestJarFactory.PluginJarSpec.valid("plugin-a")
                .withExtraResources(Map.of(
                        "META-INF/versions/21/team/carrypigeon/backend/fake/VersionedCore.class",
                        "not-a-real-class"
                )));

        try (URLClassLoader classLoader = classLoader(plugin)) {
            PluginManifestException exception = assertThrows(
                    PluginManifestException.class,
                    () -> PluginManifestLoader.load(classLoader, HOST)
            );

            assertTrue(exception.getMessage().contains("forbidden host or shared dependency class"));
        }
    }

    /**
     * 验证插件类与宿主 JAR出现同名定义时拒绝启动。
     */
    @Test
    void load_pluginClassDuplicatesHostClass_rejectsStartup(@TempDir Path tempDirectory) throws IOException {
        PluginTestJarFactory factory = new PluginTestJarFactory(tempDirectory);
        String duplicateClass = "test.shared.DuplicateType";
        Path host = factory.createLibrary(
                "duplicate-host.jar",
                "test",
                "duplicate-host",
                "1.0.0",
                Map.of(duplicateClass, PluginTestJarFactory.source(duplicateClass))
        );
        Path plugin = factory.createPlugin(PluginTestJarFactory.PluginJarSpec.valid("plugin-a")
                .withExtraSources(Map.of(duplicateClass, PluginTestJarFactory.source(duplicateClass))));

        try (URLClassLoader classLoader = classLoader(host, plugin)) {
            PluginManifestException exception = assertThrows(
                    PluginManifestException.class,
                    () -> PluginManifestLoader.load(classLoader, HOST)
            );

            assertTrue(exception.getMessage().contains("duplicate plugin class on classpath"));
        }
    }

    /**
     * 验证两个插件携带同一全限定类名时拒绝启动。
     */
    @Test
    void load_pluginClassDuplicatesAnotherPlugin_rejectsStartup(@TempDir Path tempDirectory) throws IOException {
        PluginTestJarFactory factory = new PluginTestJarFactory(tempDirectory);
        String duplicateClass = "test.shared.PluginDuplicateType";
        Map<String, String> duplicateSource = Map.of(
                duplicateClass,
                PluginTestJarFactory.source(duplicateClass)
        );
        Path first = factory.createPlugin(PluginTestJarFactory.PluginJarSpec.valid("plugin-a")
                .withExtraSources(duplicateSource));
        Path second = factory.createPlugin(PluginTestJarFactory.PluginJarSpec.valid("plugin-b")
                .withExtraSources(duplicateSource));

        try (URLClassLoader classLoader = classLoader(first, second)) {
            PluginManifestException exception = assertThrows(
                    PluginManifestException.class,
                    () -> PluginManifestLoader.load(classLoader, HOST)
            );

            assertTrue(exception.getMessage().contains("duplicate plugin class on classpath"));
        }
    }

    /**
     * 验证共享 classpath 出现同 Maven 坐标不同版本时拒绝启动。
     */
    @Test
    void load_mavenCoordinateHasDifferentVersions_rejectsStartup(@TempDir Path tempDirectory) throws IOException {
        PluginTestJarFactory factory = new PluginTestJarFactory(tempDirectory);
        Path first = factory.createLibrary("shared-1.jar", "test", "shared", "1.0.0", Map.of());
        Path second = factory.createLibrary("shared-2.jar", "test", "shared", "2.0.0", Map.of());
        Path plugin = factory.createPlugin(PluginTestJarFactory.PluginJarSpec.valid("plugin-a"));

        try (URLClassLoader classLoader = classLoader(first, second, plugin)) {
            PluginManifestException exception = assertThrows(
                    PluginManifestException.class,
                    () -> PluginManifestLoader.load(classLoader, HOST)
            );

            assertTrue(exception.getMessage().contains("conflicting Maven artifact versions"));
        }
    }

    /**
     * 验证插件已 relocation 的私有依赖不会仅因保留嵌入式 pom.properties 被误判为顶层坐标冲突。
     */
    @Test
    void load_relocatedDependencyMetadataDiffersFromHost_doesNotRejectStartup(@TempDir Path tempDirectory)
            throws IOException {
        PluginTestJarFactory factory = new PluginTestJarFactory(tempDirectory);
        Path host = factory.createLibrary("shared-1.0.0.jar", "test", "shared", "1.0.0", Map.of());
        String relocatedType = "test.plugin.plugin.a.internal.RelocatedType";
        Path plugin = factory.createPlugin(PluginTestJarFactory.PluginJarSpec.valid("plugin-a")
                .withExtraSources(Map.of(relocatedType, PluginTestJarFactory.source(relocatedType)))
                .withExtraResources(Map.of(
                        "META-INF/maven/test/shared/pom.properties",
                        "groupId=test\nartifactId=shared\nversion=2.0.0\n"
                )));

        try (URLClassLoader classLoader = classLoader(host, plugin)) {
            PluginManifestCatalog catalog = PluginManifestLoader.load(classLoader, HOST);

            assertEquals(List.of("plugin-a"), catalog.manifests().stream().map(PluginManifest::pluginId).toList());
        }
    }

    /**
     * 验证 required_host_artifacts 缺失或版本不匹配时拒绝启动。
     */
    @Test
    void load_requiredHostArtifactMissing_rejectsStartup(@TempDir Path tempDirectory) throws IOException {
        PluginTestJarFactory factory = new PluginTestJarFactory(tempDirectory);
        Path host = factory.createLibrary("host-api.jar", "test", "host-api", "2.0.0", Map.of());
        Path plugin = factory.createPlugin(PluginTestJarFactory.PluginJarSpec.valid("plugin-a")
                .withRequiredHostArtifacts(List.of("test:host-api:1.0.0")));

        try (URLClassLoader classLoader = classLoader(host, plugin)) {
            PluginManifestException exception = assertThrows(
                    PluginManifestException.class,
                    () -> PluginManifestLoader.load(classLoader, HOST)
            );

            assertTrue(exception.getMessage().contains("required host artifact is missing"));
        }
    }

    /**
     * 验证非法 required_host_artifacts 坐标无法进入运行时。
     */
    @Test
    void load_requiredHostArtifactCoordinateInvalid_rejectsStartup(@TempDir Path tempDirectory) throws IOException {
        PluginTestJarFactory factory = new PluginTestJarFactory(tempDirectory);
        Path plugin = factory.createPlugin(PluginTestJarFactory.PluginJarSpec.valid("plugin-a")
                .withRequiredHostArtifacts(List.of("invalid-coordinate")));

        try (URLClassLoader classLoader = classLoader(plugin)) {
            PluginManifestException exception = assertThrows(
                    PluginManifestException.class,
                    () -> PluginManifestLoader.load(classLoader, HOST)
            );

            assertTrue(exception.getMessage().contains("invalid required_host_artifacts coordinate"));
        }
    }

    /**
     * 验证插件不能携带可能覆盖宿主配置的根级 application 文件。
     */
    @Test
    void load_pluginContainsRootApplicationYaml_rejectsStartup(@TempDir Path tempDirectory) throws IOException {
        PluginTestJarFactory factory = new PluginTestJarFactory(tempDirectory);
        Path plugin = factory.createPlugin(PluginTestJarFactory.PluginJarSpec.valid("plugin-a")
                .withExtraResources(Map.of("application.yaml", "server.port: 0")));

        try (URLClassLoader classLoader = classLoader(plugin)) {
            PluginManifestException exception = assertThrows(
                    PluginManifestException.class,
                    () -> PluginManifestLoader.load(classLoader, HOST)
            );

            assertTrue(exception.getMessage().contains("forbidden root configuration resource"));
        }
    }

    private URLClassLoader classLoader(Path... entries) throws IOException {
        URL[] urls = new URL[entries.length];
        for (int index = 0; index < entries.length; index++) {
            urls[index] = entries[index].toUri().toURL();
        }
        return new URLClassLoader(urls, getClass().getClassLoader());
    }
}
