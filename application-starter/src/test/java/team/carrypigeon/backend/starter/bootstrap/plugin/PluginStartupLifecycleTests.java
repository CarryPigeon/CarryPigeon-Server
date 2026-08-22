package team.carrypigeon.backend.starter.bootstrap.plugin;

import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.chat.domain.features.plugin.domain.api.PluginRuntimeApi;
import team.carrypigeon.backend.chat.domain.features.server.config.RealtimeServerProperties;
import team.carrypigeon.backend.chat.domain.features.server.config.RealtimeServerRuntime;
import team.carrypigeon.backend.chat.domain.features.server.controller.ws.RealtimeChannelInitializer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * 插件启动生命周期测试。
 * 职责：验证插件生命周期顺序、幂等性与 readiness 状态。
 */
class PluginStartupLifecycleTests {

    @Test
    void lifecycle_startSucceeds_marksGateReady() {
        PluginRuntimeApi runtimeApi = mock(PluginRuntimeApi.class);
        PluginReadinessGate gate = new PluginReadinessGate();
        PluginStartupLifecycle lifecycle = new PluginStartupLifecycle(runtimeApi, gate);

        lifecycle.start();

        verify(runtimeApi).start();
        assertTrue(gate.isReady());
        assertTrue(lifecycle.isRunning());
    }

    /**
     * 验证插件运行时启动失败时异常继续向 Spring Boot 传播且门禁保持关闭。
     */
    @Test
    void lifecycle_startFails_keepsGateClosedAndPropagatesFailure() {
        PluginRuntimeApi runtimeApi = mock(PluginRuntimeApi.class);
        doThrow(new IllegalStateException("plugin failed")).when(runtimeApi).start();
        PluginReadinessGate gate = new PluginReadinessGate();
        PluginStartupLifecycle lifecycle = new PluginStartupLifecycle(runtimeApi, gate);

        assertThrows(
                IllegalStateException.class,
                lifecycle::start
        );

        assertFalse(gate.isReady());
        assertFalse(lifecycle.isRunning());
    }

    /**
     * 验证生命周期使用最早 phase，使最大 phase 的 realtime 只能在插件就绪后启动。
     */
    @Test
    void lifecycle_phase_startsBeforeRealtimePhase() {
        PluginStartupLifecycle lifecycle = new PluginStartupLifecycle(
                mock(PluginRuntimeApi.class),
                new PluginReadinessGate()
        );
        RealtimeServerRuntime realtimeRuntime = new RealtimeServerRuntime(
                new RealtimeServerProperties(false, "127.0.0.1", 18080, "/api/ws", 1, 0, 10, 60),
                mock(RealtimeChannelInitializer.class)
        );

        assertEquals(Integer.MIN_VALUE, lifecycle.getPhase());
        assertTrue(lifecycle.getPhase() < realtimeRuntime.getPhase());
    }

    /**
     * 验证重复启动不会重复调用插件运行时。
     */
    @Test
    void lifecycle_alreadyRunning_doesNotStartTwice() {
        PluginRuntimeApi runtimeApi = mock(PluginRuntimeApi.class);
        PluginStartupLifecycle lifecycle = new PluginStartupLifecycle(runtimeApi, new PluginReadinessGate());

        lifecycle.start();
        lifecycle.start();

        verify(runtimeApi, times(1)).start();
    }

    /**
     * 验证停止时先关闭流量门禁，再停止插件运行时并清除运行状态。
     */
    @Test
    void lifecycle_running_stopClosesGateAndStopsRuntime() {
        PluginRuntimeApi runtimeApi = mock(PluginRuntimeApi.class);
        PluginReadinessGate gate = new PluginReadinessGate();
        PluginStartupLifecycle lifecycle = new PluginStartupLifecycle(runtimeApi, gate);
        lifecycle.start();

        lifecycle.stop();

        verify(runtimeApi).stop();
        assertFalse(gate.isReady());
        assertFalse(lifecycle.isRunning());
    }

    /**
     * 验证插件运行时停止失败时异常继续传播，但门禁和生命周期状态仍会关闭。
     */
    @Test
    void lifecycle_stopFails_closesGateAndClearsRunningState() {
        PluginRuntimeApi runtimeApi = mock(PluginRuntimeApi.class);
        PluginReadinessGate gate = new PluginReadinessGate();
        PluginStartupLifecycle lifecycle = new PluginStartupLifecycle(runtimeApi, gate);
        lifecycle.start();
        doThrow(new IllegalStateException("plugin stop failed")).when(runtimeApi).stop();

        assertThrows(IllegalStateException.class, lifecycle::stop);

        assertFalse(gate.isReady());
        assertFalse(lifecycle.isRunning());
    }
}
