package team.carrypigeon.backend.chat.domain.shared.controller.support;

import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import team.carrypigeon.backend.infrastructure.basic.logging.LogKeys;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * HttpRequestMdcFilter 契约测试。
 * 职责：验证 HTTP 请求链路中的 MDC 写入与清理行为。
 * 边界：不验证具体日志输出格式，只验证请求级上下文字段。
 */
@Tag("contract")
class HttpRequestMdcFilterTests {

    /**
     * 验证过滤器会为请求写入最小上下文，并在请求结束后清理。
     */
    @Test
    @DisplayName("doFilter writes and clears request mdc context")
    void doFilter_requestContext_writesAndClearsMdc() throws ServletException, IOException {
        HttpRequestMdcFilter filter = new HttpRequestMdcFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/server/presence/me");
        request.addHeader(HttpRequestMdcFilter.REQUEST_ID_HEADER, "req-1");
        request.addHeader(HttpRequestMdcFilter.TRACE_ID_HEADER, "trace-1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        CapturingFilterChain chain = new CapturingFilterChain();
        filter.doFilter(request, response, chain);

        assertEquals("req-1", chain.requestId);
        assertEquals("trace-1", chain.traceId);
        assertEquals("/api/server/presence/me", chain.route);
        assertNull(MDC.get(LogKeys.REQUEST_ID));
        assertNull(MDC.get(LogKeys.TRACE_ID));
        assertNull(MDC.get(LogKeys.ROUTE));
        assertNull(MDC.get(LogKeys.UID));
    }

    /**
     * 验证未提供头部时会生成 requestId / traceId。
     */
    @Test
    @DisplayName("doFilter missing headers generates request and trace ids")
    void doFilter_missingHeaders_generatesRequestAndTraceIds() throws ServletException, IOException {
        HttpRequestMdcFilter filter = new HttpRequestMdcFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/.well-known/carrypigeon-server");
        MockHttpServletResponse response = new MockHttpServletResponse();

        CapturingFilterChain chain = new CapturingFilterChain();
        filter.doFilter(request, response, chain);

        assertNotNull(chain.requestId);
        assertNotNull(chain.traceId);
        assertEquals(chain.requestId, chain.traceId);
        assertEquals("/.well-known/carrypigeon-server", chain.route);
    }

    private static final class CapturingFilterChain extends MockFilterChain {
        private String requestId;
        private String traceId;
        private String route;

        @Override
        public void doFilter(jakarta.servlet.ServletRequest request, jakarta.servlet.ServletResponse response) {
            requestId = MDC.get(LogKeys.REQUEST_ID);
            traceId = MDC.get(LogKeys.TRACE_ID);
            route = MDC.get(LogKeys.ROUTE);
        }
    }
}
