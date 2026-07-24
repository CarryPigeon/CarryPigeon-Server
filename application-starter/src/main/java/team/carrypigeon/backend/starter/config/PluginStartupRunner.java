package team.carrypigeon.backend.starter.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import team.carrypigeon.backend.chat.domain.features.plugin.domain.api.PluginRuntimeApi;

/**
 * 插件启动协调器。
 *
 * <p>职责：在主 Spring Context 创建完成后启动逻辑启用插件，并在全部插件健康检查通过后开放流量。
 * 边界：不负责 Manifest 发现或 ClassLoader 管理；启动失败直接向 Spring Boot 抛出异常。</p>
 */
@Slf4j
@Order(Ordered.LOWEST_PRECEDENCE)
public final class PluginStartupRunner implements ApplicationRunner {

    private final PluginRuntimeApi pluginRuntimeApi;
    private final PluginReadinessGate readinessGate;

    public PluginStartupRunner(PluginRuntimeApi pluginRuntimeApi, PluginReadinessGate readinessGate) {
        this.pluginRuntimeApi = pluginRuntimeApi;
        this.readinessGate = readinessGate;
    }

    @Override
    public void run(ApplicationArguments args) {
        readinessGate.markNotReady();
        try {
            pluginRuntimeApi.start();
            readinessGate.markReady();
            log.info("Plugin runtime is ready: {} plugin status entries", pluginRuntimeApi.statuses().size());
        } catch (RuntimeException exception) {
            readinessGate.markNotReady();
            log.error("Plugin runtime startup failed; application will not accept traffic", exception);
            throw exception;
        }
    }
}
