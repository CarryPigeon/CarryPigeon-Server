package team.carrypigeon.backend.chat.domain.features.message.controller.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.HandlerInterceptor;
import team.carrypigeon.backend.chat.domain.features.message.controller.support.ChannelMessageV1ResponseMapper;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.ChannelMessagePublishingApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.MessageStatus;
import team.carrypigeon.backend.chat.domain.features.message.domain.projection.ChannelMessageResult;
import team.carrypigeon.backend.chat.domain.shared.controller.advice.GlobalExceptionHandler;
import team.carrypigeon.backend.chat.domain.shared.controller.support.RequestAuthenticationContext;
import team.carrypigeon.backend.chat.domain.shared.domain.auth.AuthenticatedAccount;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MessageController 转发协议测试。
 * 职责：验证单条/合并转发请求解析、201 状态和 canonical 响应。
 */
@Tag("contract")
class MessageForwardControllerTests {

    private ChannelMessagePublishingApi publishingApi;
    private RequestAuthenticationContext authenticationContext;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        publishingApi = mock(ChannelMessagePublishingApi.class);
        authenticationContext = new RequestAuthenticationContext();
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new MessageController(publishingApi, authenticationContext, new ChannelMessageV1ResponseMapper())
                )
                .addInterceptors(new BindPrincipalInterceptor(authenticationContext))
                .setMessageConverters(snakeCaseConverter())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    /**
     * 验证单条转发返回 201 且来源关系只在 data 内。
     */
    @Test
    @DisplayName("forward returns canonical created message")
    void forwardMessage_singleMessage_returnsCanonicalCreatedMessage() throws Exception {
        when(publishingApi.forwardChannelMessage(any())).thenReturn(forwardResult());

        mockMvc.perform(post("/api/messages/5001/forward")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "forward-1")
                        .content("{\"target_cid\":\"2\",\"comment\":\"FYI\",\"idempotency_key\":\"forward-1\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.domain").value("Core:Forward"))
                .andExpect(jsonPath("$.data.forwarded_from.mid").value("5001"))
                .andExpect(jsonPath("$.forwarded_from").doesNotExist());

        verify(publishingApi).forwardChannelMessage(any());
    }

    /**
     * 验证 header 与 body 幂等键冲突时返回校验错误。
     */
    @Test
    @DisplayName("forward rejects conflicting idempotency keys")
    void forwardMessage_conflictingIdempotencyKeys_rejectsRequest() throws Exception {
        mockMvc.perform(post("/api/messages/5001/forward")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "header-key")
                        .content("{\"target_cid\":\"2\",\"idempotency_key\":\"body-key\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.reason").value("validation_failed"));
    }

    /**
     * 验证同一幂等键绑定不同请求时返回稳定的 409 业务语义。
     */
    @Test
    @DisplayName("forward reused idempotency key returns conflict")
    void forwardMessage_reusedIdempotencyKey_returnsConflict() throws Exception {
        when(publishingApi.forwardChannelMessage(any())).thenThrow(ProblemException.conflict(
                "idempotency_key_reused",
                "idempotency key has already been used for a different request"
        ));

        mockMvc.perform(post("/api/messages/5001/forward")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "forward-1")
                        .content("{\"target_cid\":\"2\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.reason").value("idempotency_key_reused"));
    }

    /**
     * 验证请求体中的超长幂等键在进入领域服务前返回校验错误。
     */
    @Test
    @DisplayName("forward oversized body idempotency key rejects request")
    void forwardMessage_oversizedBodyIdempotencyKey_rejectsRequest() throws Exception {
        mockMvc.perform(post("/api/messages/5001/forward")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"target_cid\":\"2\",\"idempotency_key\":\"" + "x".repeat(129) + "\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.reason").value("validation_failed"));
    }

    private ChannelMessageResult forwardResult() {
        return new ChannelMessageResult(
                6001L, 1001L, 2L, "Core:Forward", "1.0.0",
                Map.of(
                        "domain", "Core:Text",
                        "domain_version", "1.0.0",
                        "content", Map.of("text", "FYI"),
                        "forwarded_from", Map.of(
                                "mid", "5001", "cid", "1", "uid", "1002",
                                "preview", "hello", "send_time", 1713744000000L
                        )
                ),
                Instant.parse("2026-04-22T00:00:00Z"),
                List.of(),
                "FYI",
                MessageStatus.SENT
        );
    }

    private MappingJackson2HttpMessageConverter snakeCaseConverter() {
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        return new MappingJackson2HttpMessageConverter(mapper);
    }

    private static class BindPrincipalInterceptor implements HandlerInterceptor {
        private final RequestAuthenticationContext context;

        private BindPrincipalInterceptor(RequestAuthenticationContext context) {
            this.context = context;
        }

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
            context.bind(request, new AuthenticatedAccount(1001L, "carry-user"));
            return true;
        }
    }
}
