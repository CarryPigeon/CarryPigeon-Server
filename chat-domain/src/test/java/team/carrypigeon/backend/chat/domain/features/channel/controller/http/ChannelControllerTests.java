package team.carrypigeon.backend.chat.domain.features.channel.controller.http;

import java.time.Instant;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.HandlerInterceptor;
import team.carrypigeon.backend.chat.domain.features.auth.controller.support.AuthRequestContext;
import team.carrypigeon.backend.chat.domain.features.auth.controller.support.AuthenticatedPrincipal;
import team.carrypigeon.backend.chat.domain.features.channel.application.dto.ChannelResult;
import team.carrypigeon.backend.chat.domain.features.channel.application.service.ChannelApplicationService;
import team.carrypigeon.backend.chat.domain.shared.controller.advice.GlobalExceptionHandler;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ChannelController 协议测试。
 * 职责：验证默认频道查询入口的统一响应码与异常映射契约。
 * 边界：不验证真实数据库访问，只验证协议层请求到响应的稳定行为。
 */
class ChannelControllerTests {

    private ChannelApplicationService channelApplicationService;
    private AuthRequestContext authRequestContext;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        channelApplicationService = mock(ChannelApplicationService.class);
        authRequestContext = new AuthRequestContext();
        mockMvc = MockMvcBuilders.standaloneSetup(new ChannelController(channelApplicationService, authRequestContext))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    /**
     * 验证已认证请求可以读取默认频道。
     */
    @Test
    @DisplayName("get default channel authenticated request returns code 100")
    void getDefaultChannel_authenticatedRequest_returnsCode100() throws Exception {
        mockMvc = authenticatedMockMvc();
        when(channelApplicationService.getDefaultChannel(any())).thenReturn(new ChannelResult(
                1L, 1L, "public", "public", true,
                Instant.parse("2026-04-22T00:00:00Z"), Instant.parse("2026-04-22T00:00:00Z")
        ));

        mockMvc.perform(get("/api/channels/default"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(100))
                .andExpect(jsonPath("$.data.channelId").value(1L))
                .andExpect(jsonPath("$.data.type").value("public"));
    }

    /**
     * 验证未认证请求访问默认频道接口时会返回 300 响应码。
     */
    @Test
    @DisplayName("get default channel anonymous request returns code 300")
    void getDefaultChannel_anonymousRequest_returnsCode300() throws Exception {
        mockMvc.perform(get("/api/channels/default"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(300));
    }

    /**
     * 验证默认频道缺失时接口会返回 404 响应码。
     */
    @Test
    @DisplayName("get default channel missing channel returns code 404")
    void getDefaultChannel_missingChannel_returnsCode404() throws Exception {
        mockMvc = authenticatedMockMvc();
        when(channelApplicationService.getDefaultChannel(any())).thenThrow(ProblemException.notFound("default channel does not exist"));

        mockMvc.perform(get("/api/channels/default"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    private MockMvc authenticatedMockMvc() {
        return MockMvcBuilders.standaloneSetup(new ChannelController(channelApplicationService, authRequestContext))
                .addInterceptors(new BindPrincipalInterceptor(authRequestContext))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private static class BindPrincipalInterceptor implements HandlerInterceptor {

        private final AuthRequestContext authRequestContext;

        private BindPrincipalInterceptor(AuthRequestContext authRequestContext) {
            this.authRequestContext = authRequestContext;
        }

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
            authRequestContext.bind(request, new AuthenticatedPrincipal(1001L, "carry-user"));
            return true;
        }
    }
}
