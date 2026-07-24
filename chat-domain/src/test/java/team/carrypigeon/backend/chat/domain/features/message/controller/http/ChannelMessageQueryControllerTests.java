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
import team.carrypigeon.backend.chat.domain.features.message.domain.api.ChannelMessageAttachmentApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.ChannelMessageLifecycleApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.ChannelMessagePublishingApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.ChannelMessageTimelineApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.MessageStatus;
import team.carrypigeon.backend.chat.domain.features.message.domain.projection.ChannelMessageHistoryResult;
import team.carrypigeon.backend.chat.domain.features.message.domain.projection.ChannelMessageResult;
import team.carrypigeon.backend.chat.domain.shared.controller.advice.GlobalExceptionHandler;
import team.carrypigeon.backend.chat.domain.shared.controller.support.RequestAuthenticationContext;
import team.carrypigeon.backend.chat.domain.shared.domain.auth.AuthenticatedAccount;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ChannelMessageController canonical 协议测试。
 * 职责：验证历史、发送和撤回共享同一消息 Wire 模型。
 */
@Tag("contract")
class ChannelMessageQueryControllerTests {

    private ChannelMessagePublishingApi publishingApi;
    private ChannelMessageTimelineApi timelineApi;
    private ChannelMessageLifecycleApi lifecycleApi;
    private RequestAuthenticationContext authenticationContext;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        publishingApi = mock(ChannelMessagePublishingApi.class);
        timelineApi = mock(ChannelMessageTimelineApi.class);
        lifecycleApi = mock(ChannelMessageLifecycleApi.class);
        authenticationContext = new RequestAuthenticationContext();
        ChannelMessageController controller = new ChannelMessageController(
                publishingApi,
                timelineApi,
                mock(ChannelMessageAttachmentApi.class),
                lifecycleApi,
                authenticationContext,
                new ChannelMessageV1ResponseMapper()
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .addInterceptors(new BindPrincipalInterceptor(authenticationContext))
                .setMessageConverters(snakeCaseConverter())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    /**
     * 验证历史列表返回 canonical 字段且不包含 sender 快照。
     */
    @Test
    @DisplayName("history returns canonical wire model")
    void getChannelMessages_existingMessage_returnsCanonicalWireModel() throws Exception {
        when(timelineApi.getChannelMessageHistory(any())).thenReturn(
                new ChannelMessageHistoryResult(List.of(textResult()), null)
        );

        mockMvc.perform(get("/api/channels/1/messages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].mid").value("5001"))
                .andExpect(jsonPath("$.items[0].domain").value("Core:Text"))
                .andExpect(jsonPath("$.items[0].data.text").value("hello"))
                .andExpect(jsonPath("$.items[0].mentions[0]").value("1002"))
                .andExpect(jsonPath("$.items[0].sender").doesNotExist());
    }

    /**
     * 验证发送请求把 mentions 作为顶层 ID 数组解析。
     */
    @Test
    @DisplayName("send parses mention id metadata")
    void sendChannelMessage_validRequest_parsesMentionIds() throws Exception {
        when(publishingApi.sendChannelMessage(any())).thenReturn(textResult());

        mockMvc.perform(post("/api/channels/1/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "domain":"Core:Text",
                                  "domain_version":"1.0.0",
                                  "data":{"text":"hello"},
                                  "mentions":["1002"]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mentions[0]").value("1002"))
                .andExpect(jsonPath("$.status").value("sent"));
    }

    /**
     * 验证撤回响应清空 data/mentions 并保留 canonical 身份字段。
     */
    @Test
    @DisplayName("recall returns redacted canonical message")
    void recallChannelMessage_existingMessage_returnsRedactedCanonicalMessage() throws Exception {
        when(lifecycleApi.recallChannelMessage(any())).thenReturn(new ChannelMessageResult(
                5001L, 1001L, 1L, "Core:Text", "1.0.0", Map.of(),
                Instant.parse("2026-04-22T00:00:00Z"), List.of(), "消息已撤回", MessageStatus.RECALLED
        ));

        mockMvc.perform(post("/api/channels/1/messages/5001/recall"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.mentions").isEmpty())
                .andExpect(jsonPath("$.status").value("recalled"));
    }

    private ChannelMessageResult textResult() {
        return new ChannelMessageResult(
                5001L, 1001L, 1L, "Core:Text", "1.0.0", Map.of("text", "hello"),
                Instant.parse("2026-04-22T00:00:00Z"), List.of(1002L), "hello", MessageStatus.SENT
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
