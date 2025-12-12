package team.carrypigeon.backend.chat.domain.plugin;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import team.carrypigeon.backend.api.plugin.CPPlugin;
import team.carrypigeon.backend.api.starter.server.ServerInfoConfig;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Central manager for all backend plugins.
 *
 * This manager collects all {@link CPPlugin} beans from the Spring context
 * and calls their {@link CPPlugin#init()} method during application startup.
 *
 * Plugin implementations should live in separate JARs under package
 * {@code team.carrypigeon.backend.plugin.*} and be annotated as Spring beans.
 */
@Slf4j
@Component
public class CPPluginManager {

    private final List<CPPlugin> plugins;
    private final ServerInfoConfig serverInfoConfig;

    public CPPluginManager(List<CPPlugin> plugins, ServerInfoConfig serverInfoConfig) {
        // avoid NPE if there is no plugin at all
        this.plugins = plugins == null ? Collections.emptyList() : plugins;
        this.serverInfoConfig = serverInfoConfig;
    }

    /**
     * Initialize all discovered plugins once Spring context is ready.
     */
    @PostConstruct
    public void initPlugins() {
        if (plugins.isEmpty()) {
            log.info("no CPPlugin found, plugin system is idle");
            return;
        }
        String serverName = serverInfoConfig != null ? serverInfoConfig.getServerName() : null;
        log.info("initializing {} plugin(s) for server: {}", plugins.size(),
                Objects.requireNonNullElse(serverName, "unknown"));
        for (CPPlugin plugin : plugins) {
            try {
                log.info("initializing plugin: name={}, version={}", plugin.getName(), plugin.getVersion());
                plugin.init();
                log.info("plugin initialized: name={}, version={}", plugin.getName(), plugin.getVersion());
            } catch (Exception e) {
                log.error("failed to initialize plugin: name={}, version={}, error={}",
                        plugin.getName(), plugin.getVersion(), e.getMessage(), e);
            }
        }
    }
}
