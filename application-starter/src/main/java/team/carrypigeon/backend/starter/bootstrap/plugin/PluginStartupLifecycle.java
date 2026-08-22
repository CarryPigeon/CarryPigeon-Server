package team.carrypigeon.backend.starter.bootstrap.plugin;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;
import team.carrypigeon.backend.chat.domain.features.plugin.domain.api.PluginRuntimeApi;

/**
 * 插件启动生命周期协调器。
 * 职责：在独立监听器启动前初始化逻辑启用插件，并在全部插件健康检查通过后开放流量。
 * 边界：不负责 Manifest 发现或 ClassLoader 管理；启动失败直接中止 Spring Context 刷新。
 */
@Slf4j
public final class PluginStartupLifecycle implements SmartLifecycle {

    /**
     * 使用最早阶段，确保最大 phase 的 realtime 监听器只能在插件就绪后启动。
     */
    public static final int PHASE = Integer.MIN_VALUE;

    private final PluginRuntimeApi pluginRuntimeApi;
    private final PluginReadinessGate readinessGate;
    private volatile boolean running;

    public PluginStartupLifecycle(PluginRuntimeApi pluginRuntimeApi, PluginReadinessGate readinessGate) {
        this.pluginRuntimeApi = pluginRuntimeApi;
        this.readinessGate = readinessGate;
    }

    /**
     * 初始化插件并开放宿主流量入口。
     */
    @Override
    public void start() {
        if (running) {
            return;
        }
        readinessGate.markNotReady();
        try {
            pluginRuntimeApi.start();
            running = true;
            readinessGate.markReady();
            log.info("Plugin runtime is ready: {} plugin status entries", pluginRuntimeApi.statuses().size());
        } catch (RuntimeException exception) {
            readinessGate.markNotReady();
            log.error("Plugin runtime startup failed; application will not accept traffic", exception);
            throw exception;
        }
    }

    /**
     * 关闭流量门禁并停止已启动插件。
     */
    @Override
    public void stop() {
        readinessGate.markNotReady();
        if (!running) {
            return;
        }
        try {
            pluginRuntimeApi.stop();
        } finally {
            running = false;
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return PHASE;
    }
}
