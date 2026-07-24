package team.carrypigeon.backend.starter.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import team.carrypigeon.backend.chat.domain.features.plugin.domain.api.PluginRuntimeApi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * 插件启动装配测试。
 * 职责：验证启动协调成功后开放 readiness，以及未就绪 HTTP 请求返回 503。
 */
class PluginRuntimeConfigurationTests {

    @Test
    void runner_startSucceeds_marksGateReady() throws Exception {
        PluginRuntimeApi runtimeApi = mock(PluginRuntimeApi.class);
        PluginReadinessGate gate = new PluginReadinessGate();
        PluginStartupRunner runner = new PluginStartupRunner(runtimeApi, gate);

        runner.run(new DefaultApplicationArguments(new String[0]));

        verify(runtimeApi).start();
        assertTrue(gate.isReady());
    }

    /**
     * 验证插件运行时启动失败时异常继续向 Spring Boot 传播且门禁保持关闭。
     */
    @Test
    void runner_startFails_keepsGateClosedAndPropagatesFailure() {
        PluginRuntimeApi runtimeApi = mock(PluginRuntimeApi.class);
        doThrow(new IllegalStateException("plugin failed")).when(runtimeApi).start();
        PluginReadinessGate gate = new PluginReadinessGate();
        PluginStartupRunner runner = new PluginStartupRunner(runtimeApi, gate);

        assertThrows(
                IllegalStateException.class,
                () -> runner.run(new DefaultApplicationArguments(new String[0]))
        );

        assertFalse(gate.isReady());
    }

    @Test
    void filter_gateNotReady_returns503() throws Exception {
        PluginReadinessGate gate = new PluginReadinessGate();
        PluginStartupGateFilter filter = new PluginStartupGateFilter(gate);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(new MockHttpServletRequest(), response, new MockFilterChain());

        assertEquals(503, response.getStatus());
        assertFalse(response.getContentAsString().isBlank());
    }

    @Test
    void filter_gateReady_forwardsRequest() throws Exception {
        PluginReadinessGate gate = new PluginReadinessGate();
        gate.markReady();
        PluginStartupGateFilter filter = new PluginStartupGateFilter(gate);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(new MockHttpServletRequest(), response, new MockFilterChain());

        assertEquals(200, response.getStatus());
    }
}
