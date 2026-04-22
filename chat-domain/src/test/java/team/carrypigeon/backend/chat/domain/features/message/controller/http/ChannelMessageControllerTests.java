package team.carrypigeon.backend.chat.domain.features.message.controller.http;

import java.time.Instant;
import java.util.List;
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
import team.carrypigeon.backend.chat.domain.features.message.application.dto.ChannelMessageHistoryResult;
import team.carrypigeon.backend.chat.domain.features.message.application.dto.ChannelMessageResult;
import team.carrypigeon.backend.chat.domain.features.message.application.service.MessageApplicationService;
import team.carrypigeon.backend.chat.domain.shared.controller.advice.GlobalExceptionHandler;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ChannelMessageController 协议测试。
 * 职责：验证频道历史消息入口的统一响应码与异常映射契约。
 * 边界：不验证真实数据库访问，只验证协议层请求到响应的稳定行为。
 */
class ChannelMessageControllerTests {

    private MessageApplicationService messageApplicationService;
    private AuthRequestContext authRequestContext;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        messageApplicationService = mock(MessageApplicationService.class);
        authRequestContext = new AuthRequestContext();
        mockMvc = MockMvcBuilders.standaloneSetup(new ChannelMessageController(messageApplicationService, authRequestContext))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    /**
     * 验证已认证成员可以查询频道历史消息。
     */
    @Test
    @DisplayName("get channel messages authenticated request returns code 100")
    void getChannelMessages_authenticatedRequest_returnsCode100() throws Exception {
        mockMvc = authenticatedMockMvc();
        when(messageApplicationService.getChannelMessageHistory(any())).thenReturn(new ChannelMessageHistoryResult(
                List.of(new ChannelMessageResult(
                        5001L, "carrypigeon-local", 1L, 1L, 1001L, "text", "hello world", null, null, "sent",
                        Instant.parse("2026-04-22T00:00:00Z")
                )),
                5001L
        ));

        mockMvc.perform(get("/api/channels/1/messages?limit=20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(100))
                .andExpect(jsonPath("$.data.messages[0].messageId").value(5001L))
                .andExpect(jsonPath("$.data.messages[0].serverId").value("carrypigeon-local"))
                .andExpect(jsonPath("$.data.nextCursor").value(5001L));
    }

    /**
     * 验证非法 limit 参数会返回 200 响应码。
     */
    @Test
    @DisplayName("get channel messages invalid limit returns code 200")
    void getChannelMessages_invalidLimit_returnsCode200() throws Exception {
        mockMvc = authenticatedMockMvc();
        when(messageApplicationService.getChannelMessageHistory(any()))
                .thenThrow(ProblemException.validationFailed("limit must be between 1 and 100"));

        mockMvc.perform(get("/api/channels/1/messages?limit=0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    /**
     * 验证非成员查询消息时会返回 300 响应码。
     */
    @Test
    @DisplayName("get channel messages non member returns code 300")
    void getChannelMessages_nonMember_returnsCode300() throws Exception {
        mockMvc = authenticatedMockMvc();
        when(messageApplicationService.getChannelMessageHistory(any()))
                .thenThrow(ProblemException.forbidden("channel_member_required", "channel membership is required"));

        mockMvc.perform(get("/api/channels/1/messages?limit=20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(300));
    }

    /**
     * 验证频道不存在时会返回 404 响应码。
     */
    @Test
    @DisplayName("get channel messages missing channel returns code 404")
    void getChannelMessages_missingChannel_returnsCode404() throws Exception {
        mockMvc = authenticatedMockMvc();
        when(messageApplicationService.getChannelMessageHistory(any()))
                .thenThrow(ProblemException.notFound("channel does not exist"));

        mockMvc.perform(get("/api/channels/9/messages?limit=20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    /**
     * 验证未预期异常会被映射为 500 响应码。
     */
    @Test
    @DisplayName("get channel messages unexpected failure returns code 500")
    void getChannelMessages_unexpectedFailure_returnsCode500() throws Exception {
        mockMvc = authenticatedMockMvc();
        when(messageApplicationService.getChannelMessageHistory(any())).thenThrow(new IllegalStateException("boom"));

        mockMvc.perform(get("/api/channels/1/messages?limit=20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    private MockMvc authenticatedMockMvc() {
        return MockMvcBuilders.standaloneSetup(new ChannelMessageController(messageApplicationService, authRequestContext))
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
