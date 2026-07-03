package team.carrypigeon.backend.chat.domain.features.channel.controller.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
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
import team.carrypigeon.backend.chat.domain.features.channel.domain.projection.ChannelReadStateResult;
import team.carrypigeon.backend.chat.domain.features.channel.domain.projection.ChannelUnreadResult;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.ChannelAccessApi;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.ChannelQueryApi;
import team.carrypigeon.backend.chat.domain.shared.controller.advice.GlobalExceptionHandler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
/**
 * `ChannelReadStateController` 契约测试。
 * 职责：验证当前测试类覆盖对象的关键成功路径、失败路径或边界行为。
 */

@Tag("contract")
class ChannelReadStateControllerTests {

    private ChannelAccessApi channelAccessDomainApi;
    private ChannelQueryApi channelQueryDomainApi;
    private RequestAuthenticationContext authRequestContext;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        channelAccessDomainApi = mock(ChannelAccessApi.class);
        channelQueryDomainApi = mock(ChannelQueryApi.class);
        authRequestContext = new RequestAuthenticationContext();
        mockMvc = MockMvcBuilders.standaloneSetup(new ChannelReadStateController(channelAccessDomainApi, channelQueryDomainApi, authRequestContext))
                .setMessageConverters(snakeCaseConverter())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    /**
     * 验证 `updateChannelReadState` 在 `returnsPayload` 场景下的测试契约。
     */
    @Test
    @DisplayName("update channel read state returns payload")
    void updateChannelReadState_returnsPayload() throws Exception {
        mockMvc = authenticatedMockMvc();
        when(channelAccessDomainApi.updateChannelReadState(any()))
                .thenReturn(new ChannelReadStateResult("9", "1001", "5001", 1700000000000L));

        mockMvc.perform(put("/api/channels/9/read_state")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"last_read_mid\":\"5001\"," +
                                "\"last_read_time\":1700000000000" +
                                "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cid").value("9"))
                .andExpect(jsonPath("$.uid").value("1001"))
                .andExpect(jsonPath("$.last_read_mid").value("5001"));
    }

    /**
     * 验证 `listUnreads` 在 `returnsItems` 场景下的测试契约。
     */
    @Test
    @DisplayName("list unreads returns items")
    void listUnreads_returnsItems() throws Exception {
        mockMvc = authenticatedMockMvc();
        when(channelQueryDomainApi.listUnreads(1001L))
                .thenReturn(List.of(new ChannelUnreadResult("9", 3L, 1700000000000L)));

        mockMvc.perform(get("/api/unreads"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].cid").value("9"))
                .andExpect(jsonPath("$.items[0].unread_count").value(3));
    }

    private MockMvc authenticatedMockMvc() {
        return MockMvcBuilders.standaloneSetup(new ChannelReadStateController(channelAccessDomainApi, channelQueryDomainApi, authRequestContext))
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
