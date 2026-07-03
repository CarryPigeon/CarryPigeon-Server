package team.carrypigeon.backend.chat.domain.features.message.controller.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.HandlerInterceptor;
import team.carrypigeon.backend.chat.domain.shared.controller.support.RequestAuthenticationContext;
import team.carrypigeon.backend.chat.domain.shared.domain.auth.AuthenticatedAccount;
import team.carrypigeon.backend.chat.domain.features.message.domain.projection.ChannelMessageResult;
import team.carrypigeon.backend.chat.domain.features.message.controller.support.ChannelMessageV1ResponseMapper;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.ChannelMessageLifecycleApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.ChannelMessagePublishingApi;
import team.carrypigeon.backend.chat.domain.shared.controller.advice.GlobalExceptionHandler;
import team.carrypigeon.backend.infrastructure.basic.json.JsonProvider;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
/**
 * `MessageForwardController` 契约测试。
 * 职责：验证当前测试类覆盖对象的关键成功路径、失败路径或边界行为。
 */

@Tag("contract")
class MessageForwardControllerTests {

    private ChannelMessagePublishingApi channelMessagePublishingApi;
    private ChannelMessageLifecycleApi channelMessageLifecycleApi;
    private RequestAuthenticationContext authRequestContext;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        channelMessagePublishingApi = mock(ChannelMessagePublishingApi.class);
        channelMessageLifecycleApi = mock(ChannelMessageLifecycleApi.class);
        authRequestContext = new RequestAuthenticationContext();
        mockMvc = MockMvcBuilders.standaloneSetup(new MessageController(channelMessagePublishingApi, channelMessageLifecycleApi, authRequestContext, responseMapper()))
                .setMessageConverters(snakeCaseConverter())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    /**
     * 验证 `forwardMessage` 在 `returnsCreatedTargetMessagePayload` 场景下的测试契约。
     */
    @Test
    @DisplayName("forward message returns created target message payload")
    void forwardMessage_returnsCreatedTargetMessagePayload() throws Exception {
        mockMvc = authenticatedMockMvc();
        when(channelMessagePublishingApi.forwardChannelMessage(any()))
                .thenReturn(new ChannelMessageResult(
                        6001L,
                        "550e8400-e29b-41d4-a716-446655440000",
                        2L,
                        2L,
                        1001L,
                        "text",
                        "FYI\n\n[Forwarded] hello",
                        "FYI [Forwarded] hello",
                        null,
                        null,
                        null,
                        "{\"mid\":\"5001\",\"cid\":\"1\",\"uid\":\"1002\",\"preview\":\"hello\",\"send_time\":1713744000000}",
                        "sent",
                        Instant.parse("2026-04-22T00:00:00Z"),
                        null,
                        1L
                ));

                mockMvc.perform(post("/api/messages/5001/forward")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"target_cid\":\"2\"," +
                                "\"comment\":\"FYI\"" +
                                "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mid").value("6001"))
                .andExpect(jsonPath("$.cid").value("2"))
                .andExpect(jsonPath("$.forwarded_from.mid").value("5001"))
                .andExpect(jsonPath("$.forwarded_from.cid").value("1"));
    }

    private MockMvc authenticatedMockMvc() {
        return MockMvcBuilders.standaloneSetup(new MessageController(channelMessagePublishingApi, channelMessageLifecycleApi, authRequestContext, responseMapper()))
                .addInterceptors(new BindPrincipalInterceptor(authRequestContext))
                .setMessageConverters(snakeCaseConverter())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private MappingJackson2HttpMessageConverter snakeCaseConverter() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        return new MappingJackson2HttpMessageConverter(objectMapper);
    }

    private ChannelMessageV1ResponseMapper responseMapper() {
        return new ChannelMessageV1ResponseMapper(null, new JsonProvider(new ObjectMapper().findAndRegisterModules()));
    }

    /**
     * `BindPrincipalInterceptor` 测试辅助类型。
     * 职责：隔离外部依赖，使测试只验证当前契约边界。
     */
    private static class BindPrincipalInterceptor implements HandlerInterceptor {
        private final RequestAuthenticationContext authRequestContext;

        private BindPrincipalInterceptor(RequestAuthenticationContext authRequestContext) {
            this.authRequestContext = authRequestContext;
        }

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
            authRequestContext.bind(request, new AuthenticatedAccount(1001L, "carry-user"));
            return true;
        }
    }
}
