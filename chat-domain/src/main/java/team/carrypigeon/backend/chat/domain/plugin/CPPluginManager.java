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
 * 插件管理器。
 * <p>
 * 负责收集、初始化并暴露系统已加载插件集合。
 */
@Slf4j
@Component
public class CPPluginManager {

    private final List<CPPlugin> plugins;
    private final ServerInfoConfig serverInfoConfig;

    /**
     * 构造插件管理器。
     *
     * @param plugins Spring 扫描到的插件集合。
     * @param serverInfoConfig 服务端基础信息配置。
     */
    public CPPluginManager(List<CPPlugin> plugins, ServerInfoConfig serverInfoConfig) {
        this.plugins = plugins == null ? Collections.emptyList() : plugins;
        this.serverInfoConfig = serverInfoConfig;
    }

    /**
     * 在容器启动完成后初始化全部插件。
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
