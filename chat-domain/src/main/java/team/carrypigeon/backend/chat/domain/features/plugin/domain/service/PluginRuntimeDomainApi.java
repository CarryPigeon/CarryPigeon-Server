package team.carrypigeon.backend.chat.domain.features.plugin.domain.service;

import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.chat.domain.features.plugin.domain.api.PluginRuntimeApi;
import team.carrypigeon.backend.chat.domain.features.plugin.domain.extension.PluginHealth;
import team.carrypigeon.backend.chat.domain.features.plugin.domain.extension.PluginMigration;
import team.carrypigeon.backend.chat.domain.features.plugin.domain.extension.PluginMigrationContext;
import team.carrypigeon.backend.chat.domain.features.plugin.domain.extension.SystemPlugin;
import team.carrypigeon.backend.chat.domain.features.plugin.domain.extension.SystemPluginContext;
import team.carrypigeon.backend.chat.domain.features.plugin.domain.projection.PluginRuntimeStatusResult;
import team.carrypigeon.backend.infrastructure.basic.InfrastructureBasics;
import team.carrypigeon.backend.infrastructure.basic.plugin.PluginConfigurationProvider;
import team.carrypigeon.backend.infrastructure.basic.plugin.manifest.PluginManifest;
import team.carrypigeon.backend.infrastructure.basic.plugin.manifest.PluginManifestCatalog;
import team.carrypigeon.backend.infrastructure.basic.plugin.manifest.PluginManifestException;
import team.carrypigeon.backend.infrastructure.service.database.api.model.PluginMigrationRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.PluginMigrationDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.api.transaction.TransactionRunner;

/**
 * 启动期插件运行时领域服务。
 * 职责：按 Manifest 依赖顺序启动逻辑启用插件，执行迁移、健康检查和逆序停止。
 * 边界：不负责发现 JAR；Manifest 发现和宿主兼容性校验由 starter 在 Spring 前完成。
 */
@Service
@Slf4j
public class PluginRuntimeDomainApi implements PluginRuntimeApi {

    private final PluginManifestCatalog manifestCatalog;
    private final PluginConfigurationProvider configurationProvider;
    private final List<SystemPlugin> systemPlugins;
    private final List<PluginMigration> migrations;
    private final Optional<PluginMigrationDatabaseService> migrationDatabaseService;
    private final Optional<DataSource> dataSource;
    private final Optional<TransactionRunner> transactionRunner;
    private final ApplicationContext applicationContext;
    private final InfrastructureBasics infrastructureBasics;
    private final Map<String, SystemPlugin> startedPlugins = new LinkedHashMap<>();
    private final List<PluginRuntimeStatusResult> statuses = new ArrayList<>();
    private boolean started;

    public PluginRuntimeDomainApi(
            PluginManifestCatalog manifestCatalog,
            PluginConfigurationProvider configurationProvider,
            List<SystemPlugin> systemPlugins,
            List<PluginMigration> migrations,
            Optional<PluginMigrationDatabaseService> migrationDatabaseService,
            Optional<DataSource> dataSource,
            Optional<TransactionRunner> transactionRunner,
            ApplicationContext applicationContext,
            InfrastructureBasics infrastructureBasics
    ) {
        this.manifestCatalog = manifestCatalog;
        this.configurationProvider = configurationProvider;
        this.systemPlugins = List.copyOf(systemPlugins == null ? List.of() : systemPlugins);
        this.migrations = List.copyOf(migrations == null ? List.of() : migrations);
        this.migrationDatabaseService = migrationDatabaseService;
        this.dataSource = dataSource;
        this.transactionRunner = transactionRunner;
        this.applicationContext = applicationContext;
        this.infrastructureBasics = infrastructureBasics;
    }

    @Override
    public synchronized void start() {
        if (started) {
            return;
        }
        statuses.clear();
        Map<String, PluginManifest> enabledManifests = enabledManifests();
        Map<String, SystemPlugin> pluginBeans = resolvePluginBeans(enabledManifests);
        List<PluginManifest> startupOrder = topologicalOrder(enabledManifests);
        PluginManifest startingManifest = null;
        try {
            for (PluginManifest manifest : startupOrder) {
                startingManifest = manifest;
                SystemPlugin plugin = pluginBeans.get(manifest.pluginId());
                runMigrations(manifest);
                startedPlugins.put(manifest.pluginId(), plugin);
                plugin.start(new SystemPluginContext(applicationContext, manifest));
                PluginHealth health = plugin.health();
                if (health == null || health.status() == PluginHealth.Status.FAILED) {
                    throw new PluginManifestException("plugin health check failed: " + manifest.pluginId()
                            + ": " + (health == null ? "null" : health.message()));
                }
                statuses.add(new PluginRuntimeStatusResult(
                        manifest.pluginId(), manifest.version(), health.status().name(), health.message()
                ));
            }
            statuses.addAll(manifestCatalog.manifests().stream()
                    .filter(manifest -> !enabledManifests.containsKey(manifest.pluginId()))
                    .filter(manifest -> !startedPlugins.containsKey(manifest.pluginId()))
                    .map(manifest -> new PluginRuntimeStatusResult(
                            manifest.pluginId(), manifest.version(), "SKIPPED", "plugin disabled"
                    ))
                    .toList());
            started = true;
        } catch (RuntimeException exception) {
            if (startingManifest != null) {
                PluginManifest failedManifest = startingManifest;
                statuses.removeIf(status -> failedManifest.pluginId().equals(status.pluginId()));
                statuses.add(new PluginRuntimeStatusResult(
                        failedManifest.pluginId(), failedManifest.version(), "FAILED", exception.getMessage()
                ));
            }
            stopStartedPlugins();
            throw exception;
        }
    }

    @Override
    @PreDestroy
    public synchronized void stop() {
        stopStartedPlugins();
        started = false;
    }

    @Override
    public synchronized List<PluginRuntimeStatusResult> statuses() {
        return List.copyOf(statuses);
    }

    private Map<String, PluginManifest> enabledManifests() {
        Map<String, PluginManifest> enabled = new LinkedHashMap<>();
        for (PluginManifest manifest : manifestCatalog.manifests()) {
            if (configurationProvider.isEnabled(manifest.pluginId())) {
                enabled.put(manifest.pluginId(), manifest);
            }
        }
        return enabled;
    }

    private Map<String, SystemPlugin> resolvePluginBeans(Map<String, PluginManifest> enabledManifests) {
        Map<String, SystemPlugin> resolved = new HashMap<>();
        for (SystemPlugin plugin : systemPlugins) {
            if (!enabledManifests.containsKey(plugin.pluginId())) {
                continue;
            }
            if (resolved.put(plugin.pluginId(), plugin) != null) {
                throw new PluginManifestException("multiple SystemPlugin beans for: " + plugin.pluginId());
            }
        }
        for (String pluginId : enabledManifests.keySet()) {
            if (!resolved.containsKey(pluginId)) {
                throw new PluginManifestException("enabled plugin has no SystemPlugin bean: " + pluginId);
            }
        }
        return resolved;
    }

    private List<PluginManifest> topologicalOrder(Map<String, PluginManifest> enabledManifests) {
        List<PluginManifest> result = new ArrayList<>();
        Set<String> visiting = new HashSet<>();
        Set<String> visited = new HashSet<>();
        for (PluginManifest manifest : enabledManifests.values().stream()
                .sorted(Comparator.comparing(PluginManifest::pluginId)).toList()) {
            visit(manifest.pluginId(), enabledManifests, visiting, visited, result);
        }
        return result;
    }

    private void visit(
            String pluginId,
            Map<String, PluginManifest> manifests,
            Set<String> visiting,
            Set<String> visited,
            List<PluginManifest> result
    ) {
        if (visited.contains(pluginId)) {
            return;
        }
        if (!visiting.add(pluginId)) {
            throw new PluginManifestException("enabled plugin dependency cycle: " + pluginId);
        }
        PluginManifest manifest = manifests.get(pluginId);
        if (manifest == null) {
            throw new PluginManifestException("enabled plugin dependency is disabled: " + pluginId);
        }
        for (String dependency : manifest.requiredPluginIds()) {
            visit(dependency, manifests, visiting, visited, result);
        }
        visiting.remove(pluginId);
        visited.add(pluginId);
        result.add(manifest);
    }

    private void runMigrations(PluginManifest manifest) {
        List<PluginMigration> pluginMigrations = migrations.stream()
                .filter(migration -> manifest.pluginId().equals(migration.pluginId()))
                .sorted(Comparator.comparing(PluginMigration::migrationVersion))
                .toList();
        Set<String> migrationVersions = new HashSet<>();
        for (PluginMigration migration : pluginMigrations) {
            if (!migrationVersions.add(migration.migrationVersion())) {
                throw new PluginManifestException("duplicate plugin migration version: "
                        + manifest.pluginId() + ":" + migration.migrationVersion());
            }
        }
        if (pluginMigrations.isEmpty()) {
            if (manifest.databaseMigrations()) {
                throw new PluginManifestException(
                        "plugin declares database migrations but provides none: " + manifest.pluginId()
                );
            }
            return;
        }
        if (!manifest.databaseMigrations()) {
            throw new PluginManifestException(
                    "plugin provides database migrations without declaring them: " + manifest.pluginId()
            );
        }
        PluginMigrationDatabaseService history = migrationDatabaseService.orElseThrow(
                () -> new PluginManifestException("database migration service is unavailable: " + manifest.pluginId())
        );
        DataSource source = dataSource.orElseThrow(
                () -> new PluginManifestException("DataSource is unavailable for plugin migration: " + manifest.pluginId())
        );
        history.ensureHistoryStorage();
        for (PluginMigration migration : pluginMigrations) {
            validateMigration(migration, manifest);
            Optional<PluginMigrationRecord> existing = history.find(manifest.pluginId(), migration.migrationVersion());
            if (existing.isPresent()) {
                if (!migration.checksum().equals(existing.get().checksum())) {
                    throw new PluginManifestException("plugin migration checksum changed: "
                            + manifest.pluginId() + ":" + migration.migrationVersion());
                }
                continue;
            }
            Runnable action = () -> {
                migration.migrate(new PluginMigrationContext(source, applicationContext, manifest));
                history.insert(new PluginMigrationRecord(
                        manifest.pluginId(),
                        migration.pluginVersion(),
                        migration.migrationVersion(),
                        migration.description(),
                        migration.checksum(),
                        infrastructureBasics.time().nowInstant(),
                        true
                ));
            };
            transactionRunner.ifPresentOrElse(runner -> runner.runInTransaction(action), action);
        }
    }

    private void validateMigration(PluginMigration migration, PluginManifest manifest) {
        if (!manifest.pluginId().equals(migration.pluginId())
                || !manifest.version().equals(migration.pluginVersion())
                || migration.migrationVersion() == null || migration.migrationVersion().isBlank()
                || migration.checksum() == null || migration.checksum().isBlank()) {
            throw new PluginManifestException("invalid plugin migration metadata: " + manifest.pluginId());
        }
    }

    private void stopStartedPlugins() {
        List<SystemPlugin> reverse = new ArrayList<>(startedPlugins.values());
        java.util.Collections.reverse(reverse);
        for (SystemPlugin plugin : reverse) {
            try {
                plugin.stop();
            } catch (RuntimeException exception) {
                log.warn("Plugin stop callback failed; continuing reverse cleanup: pluginId={}",
                        plugin.pluginId(), exception);
            }
        }
        startedPlugins.clear();
    }
}
