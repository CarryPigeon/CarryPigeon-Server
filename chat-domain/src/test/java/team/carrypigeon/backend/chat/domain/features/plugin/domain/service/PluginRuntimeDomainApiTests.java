package team.carrypigeon.backend.chat.domain.features.plugin.domain.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import team.carrypigeon.backend.chat.domain.features.plugin.domain.extension.PluginHealth;
import team.carrypigeon.backend.chat.domain.features.plugin.domain.extension.PluginMigration;
import team.carrypigeon.backend.chat.domain.features.plugin.domain.extension.PluginMigrationContext;
import team.carrypigeon.backend.chat.domain.features.plugin.domain.extension.SystemPlugin;
import team.carrypigeon.backend.chat.domain.features.plugin.domain.extension.SystemPluginContext;
import team.carrypigeon.backend.infrastructure.basic.InfrastructureBasics;
import team.carrypigeon.backend.infrastructure.basic.plugin.PluginConfigurationProvider;
import team.carrypigeon.backend.infrastructure.basic.plugin.PluginProperties;
import team.carrypigeon.backend.infrastructure.basic.plugin.manifest.PluginManifest;
import team.carrypigeon.backend.infrastructure.basic.plugin.manifest.PluginManifestCatalog;
import team.carrypigeon.backend.infrastructure.basic.plugin.manifest.PluginManifestException;
import team.carrypigeon.backend.infrastructure.service.database.api.model.PluginMigrationRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.PluginMigrationDatabaseService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 插件运行时领域服务测试。
 * 职责：验证逻辑启停、强依赖拓扑、健康失败清理和逆序停止契约。
 */
class PluginRuntimeDomainApiTests {

    @Test
    void start_enabledDependencies_startsTopologicallyAndStopsInReverse() {
        List<String> events = new ArrayList<>();
        RecordingPlugin dependency = new RecordingPlugin("plugin-b", events, PluginHealth.active());
        RecordingPlugin consumer = new RecordingPlugin("plugin-a", events, PluginHealth.active());
        PluginRuntimeDomainApi runtime = runtime(
                List.of(manifest("plugin-a", List.of("plugin-b")), manifest("plugin-b", List.of())),
                List.of(consumer, dependency),
                Map.of("plugin-a", enabled(), "plugin-b", enabled())
        );

        runtime.start();
        runtime.stop();

        assertEquals(List.of("start:plugin-b", "start:plugin-a", "stop:plugin-a", "stop:plugin-b"), events);
    }

    @Test
    void start_disabledPlugin_skipsLifecycle() {
        PluginRuntimeDomainApi runtime = runtime(
                List.of(manifest("plugin-a", List.of())),
                List.of(),
                Map.of()
        );

        runtime.start();

        assertEquals("SKIPPED", runtime.statuses().getFirst().status());
    }

    @Test
    void start_failedHealth_stopsPreviouslyStartedPlugins() {
        List<String> events = new ArrayList<>();
        RecordingPlugin dependency = new RecordingPlugin("plugin-b", events, PluginHealth.active());
        RecordingPlugin consumer = new RecordingPlugin("plugin-a", events, PluginHealth.failed("broken"));
        PluginRuntimeDomainApi runtime = runtime(
                List.of(manifest("plugin-a", List.of("plugin-b")), manifest("plugin-b", List.of())),
                List.of(consumer, dependency),
                Map.of("plugin-a", enabled(), "plugin-b", enabled())
        );

        assertThrows(PluginManifestException.class, runtime::start);
        assertEquals(List.of(
                "start:plugin-b", "start:plugin-a", "stop:plugin-a", "stop:plugin-b"
        ), events);
    }

    /**
     * 验证启动回调抛出异常时，当前插件和此前插件仍按逆序执行清理。
     */
    @Test
    void start_callbackFails_stopsCurrentAndPreviousPlugins() {
        List<String> events = new ArrayList<>();
        RecordingPlugin dependency = new RecordingPlugin("plugin-b", events, PluginHealth.active());
        RecordingPlugin consumer = new RecordingPlugin("plugin-a", events, PluginHealth.active(), true);
        PluginRuntimeDomainApi runtime = runtime(
                List.of(manifest("plugin-a", List.of("plugin-b")), manifest("plugin-b", List.of())),
                List.of(consumer, dependency),
                Map.of("plugin-a", enabled(), "plugin-b", enabled())
        );

        assertThrows(IllegalStateException.class, runtime::start);
        assertEquals(List.of(
                "start:plugin-b", "start:plugin-a", "stop:plugin-a", "stop:plugin-b"
        ), events);
    }

    /**
     * 验证一个插件停止失败时仍继续清理其余已启动插件。
     */
    @Test
    void stop_callbackFails_continuesReverseCleanup() {
        List<String> events = new ArrayList<>();
        RecordingPlugin dependency = new RecordingPlugin("plugin-b", events, PluginHealth.active());
        RecordingPlugin consumer = new RecordingPlugin(
                "plugin-a", events, PluginHealth.active(), false, true
        );
        PluginRuntimeDomainApi runtime = runtime(
                List.of(manifest("plugin-a", List.of("plugin-b")), manifest("plugin-b", List.of())),
                List.of(consumer, dependency),
                Map.of("plugin-a", enabled(), "plugin-b", enabled())
        );

        runtime.start();
        runtime.stop();

        assertEquals(List.of(
                "start:plugin-b", "start:plugin-a", "stop:plugin-a", "stop:plugin-b"
        ), events);
    }

    /**
     * 验证同一插件声明重复迁移版本时在访问数据库前直接拒绝启动。
     */
    @Test
    void start_duplicateMigrationVersion_rejectsStartup() {
        PluginRuntimeDomainApi runtime = runtime(
                List.of(manifest("plugin-a", List.of())),
                List.of(new RecordingPlugin("plugin-a", new ArrayList<>(), PluginHealth.active())),
                List.of(new RecordingMigration("plugin-a", "V1"), new RecordingMigration("plugin-a", "V1")),
                Map.of("plugin-a", enabled())
        );

        PluginManifestException exception = assertThrows(PluginManifestException.class, runtime::start);

        assertEquals("duplicate plugin migration version: plugin-a:V1", exception.getMessage());
    }

    /**
     * 验证数据库迁移回调失败时插件启动回调不会执行。
     */
    @Test
    void start_migrationFails_rejectsPluginStartup() {
        List<String> events = new ArrayList<>();
        PluginMigrationDatabaseService history = mock(PluginMigrationDatabaseService.class);
        when(history.find("plugin-a", "V1")).thenReturn(Optional.empty());
        PluginRuntimeDomainApi runtime = runtime(
                List.of(manifest("plugin-a", List.of(), true)),
                List.of(new RecordingPlugin("plugin-a", events, PluginHealth.active())),
                List.of(new RecordingMigration("plugin-a", "V1", true)),
                Map.of("plugin-a", enabled()),
                Optional.of(history),
                Optional.of(mock(DataSource.class))
        );

        assertThrows(IllegalStateException.class, runtime::start);

        assertEquals(List.of(), events);
    }

    /**
     * 验证已执行迁移 checksum 变化时拒绝启动且不重新执行迁移。
     */
    @Test
    void start_migrationChecksumChanged_rejectsPluginStartup() {
        PluginMigrationDatabaseService history = mock(PluginMigrationDatabaseService.class);
        when(history.find("plugin-a", "V1")).thenReturn(Optional.of(new PluginMigrationRecord(
                "plugin-a", "1.0.0", "V1", "test", "old-checksum", Instant.EPOCH, true
        )));
        PluginRuntimeDomainApi runtime = runtime(
                List.of(manifest("plugin-a", List.of(), true)),
                List.of(new RecordingPlugin("plugin-a", new ArrayList<>(), PluginHealth.active())),
                List.of(new RecordingMigration("plugin-a", "V1")),
                Map.of("plugin-a", enabled()),
                Optional.of(history),
                Optional.of(mock(DataSource.class))
        );

        PluginManifestException exception = assertThrows(PluginManifestException.class, runtime::start);

        assertEquals("plugin migration checksum changed: plugin-a:V1", exception.getMessage());
    }

    private PluginRuntimeDomainApi runtime(
            List<PluginManifest> manifests,
            List<SystemPlugin> plugins,
            Map<String, PluginProperties.PluginConfig> configs
    ) {
        return runtime(manifests, plugins, List.of(), configs);
    }

    private PluginRuntimeDomainApi runtime(
            List<PluginManifest> manifests,
            List<SystemPlugin> plugins,
            List<PluginMigration> migrations,
            Map<String, PluginProperties.PluginConfig> configs
    ) {
        return runtime(manifests, plugins, migrations, configs, Optional.empty(), Optional.empty());
    }

    private PluginRuntimeDomainApi runtime(
            List<PluginManifest> manifests,
            List<SystemPlugin> plugins,
            List<PluginMigration> migrations,
            Map<String, PluginProperties.PluginConfig> configs,
            Optional<PluginMigrationDatabaseService> migrationDatabaseService,
            Optional<DataSource> dataSource
    ) {
        return new PluginRuntimeDomainApi(
                new PluginManifestCatalog(manifests),
                new PluginConfigurationProvider(new PluginProperties(new LinkedHashMap<>(configs))),
                plugins,
                migrations,
                migrationDatabaseService,
                dataSource,
                Optional.empty(),
                mock(ApplicationContext.class),
                mock(InfrastructureBasics.class)
        );
    }

    private PluginProperties.PluginConfig enabled() {
        return new PluginProperties.PluginConfig(true, Map.of());
    }

    private PluginManifest manifest(String pluginId, List<String> dependencies) {
        return manifest(pluginId, dependencies, false);
    }

    private PluginManifest manifest(String pluginId, List<String> dependencies, boolean databaseMigrations) {
        return new PluginManifest(
                pluginId,
                pluginId,
                "1.0.0",
                "test",
                "SYSTEM",
                "STARTUP_CLASSPATH",
                new PluginManifest.PluginHostRequirement("1.0.0", "build", "21", "3.5.3"),
                List.of("test.AutoConfiguration"),
                dependencies,
                List.of(),
                List.of(),
                databaseMigrations,
                "cp.plugin.configs." + pluginId,
                "test",
                ""
        );
    }

    /** 记录生命周期顺序的测试插件。 */
    private static final class RecordingPlugin implements SystemPlugin {

        private final String pluginId;
        private final List<String> events;
        private final PluginHealth health;
        private final boolean failOnStart;
        private final boolean failOnStop;

        private RecordingPlugin(String pluginId, List<String> events, PluginHealth health) {
            this(pluginId, events, health, false, false);
        }

        private RecordingPlugin(
                String pluginId,
                List<String> events,
                PluginHealth health,
                boolean failOnStart
        ) {
            this(pluginId, events, health, failOnStart, false);
        }

        private RecordingPlugin(
                String pluginId,
                List<String> events,
                PluginHealth health,
                boolean failOnStart,
                boolean failOnStop
        ) {
            this.pluginId = pluginId;
            this.events = events;
            this.health = health;
            this.failOnStart = failOnStart;
            this.failOnStop = failOnStop;
        }

        @Override
        public String pluginId() {
            return pluginId;
        }

        @Override
        public void start(SystemPluginContext context) {
            events.add("start:" + pluginId);
            if (failOnStart) {
                throw new IllegalStateException("start failed");
            }
        }

        @Override
        public PluginHealth health() {
            return health;
        }

        @Override
        public void stop() {
            events.add("stop:" + pluginId);
            if (failOnStop) {
                throw new IllegalStateException("stop failed");
            }
        }
    }

    /** 仅用于重复迁移版本校验的测试迁移。 */
    private static final class RecordingMigration implements PluginMigration {

        private final String pluginId;
        private final String migrationVersion;
        private final boolean failOnMigrate;

        private RecordingMigration(String pluginId, String migrationVersion) {
            this(pluginId, migrationVersion, false);
        }

        private RecordingMigration(String pluginId, String migrationVersion, boolean failOnMigrate) {
            this.pluginId = pluginId;
            this.migrationVersion = migrationVersion;
            this.failOnMigrate = failOnMigrate;
        }

        @Override
        public String pluginId() {
            return pluginId;
        }

        @Override
        public String pluginVersion() {
            return "1.0.0";
        }

        @Override
        public String migrationVersion() {
            return migrationVersion;
        }

        @Override
        public String description() {
            return "test";
        }

        @Override
        public String checksum() {
            return "new-checksum";
        }

        @Override
        public void migrate(PluginMigrationContext context) {
            if (failOnMigrate) {
                throw new IllegalStateException("migration failed");
            }
        }
    }
}
