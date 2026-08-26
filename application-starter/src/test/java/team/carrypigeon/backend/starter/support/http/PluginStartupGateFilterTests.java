package team.carrypigeon.backend.starter.support.http;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import team.carrypigeon.backend.starter.bootstrap.plugin.PluginReadinessGate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * 插件 HTTP 启动门禁测试。
 * 职责：验证插件未就绪时拒绝 Servlet HTTP 请求，就绪后恢复正常过滤链。
 */
@Tag("contract")
class PluginStartupGateFilterTests {

    /**
     * 验证插件未就绪时返回稳定 503 错误。
     */
    @Test
    void filter_gateNotReady_returns503() throws Exception {
        PluginReadinessGate gate = new PluginReadinessGate();
        PluginStartupGateFilter filter = new PluginStartupGateFilter(gate);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(new MockHttpServletRequest(), response, new MockFilterChain());

        assertEquals(503, response.getStatus());
        assertFalse(response.getContentAsString().isBlank());
    }

    /**
     * 验证插件就绪后请求进入后续过滤链。
     */
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
