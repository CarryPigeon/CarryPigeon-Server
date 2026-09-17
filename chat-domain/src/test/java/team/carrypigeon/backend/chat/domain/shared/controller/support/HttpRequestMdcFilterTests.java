package team.carrypigeon.backend.chat.domain.shared.controller.support;

import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
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
     * 验证 MDC 过滤器在常规业务和请求摘要过滤器之前建立日志上下文。
     */
    @Test
    @DisplayName("filter order establishes mdc before regular filters")
    void getOrder_default_runsBeforeRegularFilters() {
        assertEquals(Ordered.HIGHEST_PRECEDENCE + 1, new HttpRequestMdcFilter().getOrder());
    }

    /**
     * 验证过滤器会为请求写入最小上下文，并在请求结束后清理。
     */
    @Test
    @DisplayName("doFilter writes and clears request mdc context")
    void doFilter_requestContext_writesAndClearsMdc() throws ServletException, IOException {
        HttpRequestMdcFilter filter = new HttpRequestMdcFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/server");
        request.addHeader(HttpRequestMdcFilter.REQUEST_ID_HEADER, "req-1");
        request.addHeader(HttpRequestMdcFilter.TRACE_ID_HEADER, "trace-1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        CapturingFilterChain chain = new CapturingFilterChain();
        filter.doFilter(request, response, chain);

        assertEquals("req-1", chain.requestId);
        assertEquals("trace-1", chain.traceId);
        assertEquals("/api/server", chain.route);
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
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/tokens");
        MockHttpServletResponse response = new MockHttpServletResponse();

        CapturingFilterChain chain = new CapturingFilterChain();
        filter.doFilter(request, response, chain);

        assertNotNull(chain.requestId);
        assertNotNull(chain.traceId);
        assertEquals(chain.requestId, chain.traceId);
        assertEquals("/api/auth/tokens", chain.route);
    }

    /**
     * 验证请求作用域隔离线程已有字段，并在请求结束后恢复外层上下文。
     */
    @Test
    @DisplayName("doFilter isolates and restores outer mdc context")
    void doFilter_existingOuterContext_isolatesAndRestoresContext() throws ServletException, IOException {
        MDC.put(LogKeys.UID, "outer-user");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/server");
        MockHttpServletResponse response = new MockHttpServletResponse();
        CapturingFilterChain chain = new CapturingFilterChain();

        try {
            new HttpRequestMdcFilter().doFilter(request, response, chain);

            assertNull(chain.uid);
            assertEquals("outer-user", MDC.get(LogKeys.UID));
        } finally {
            MDC.clear();
        }
    }

    /**
     * `CapturingFilterChain` 测试辅助类型。
     * 职责：隔离外部依赖，使测试只验证当前契约边界。
     */
    private static final class CapturingFilterChain extends MockFilterChain {
        private String requestId;
        private String traceId;
        private String route;
        private String uid;

        @Override
        public void doFilter(jakarta.servlet.ServletRequest request, jakarta.servlet.ServletResponse response) {
            requestId = MDC.get(LogKeys.REQUEST_ID);
            traceId = MDC.get(LogKeys.TRACE_ID);
            route = MDC.get(LogKeys.ROUTE);
            uid = MDC.get(LogKeys.UID);
        }
    }
}
