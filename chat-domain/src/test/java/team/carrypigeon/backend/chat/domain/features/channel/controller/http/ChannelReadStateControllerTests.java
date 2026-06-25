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
import team.carrypigeon.backend.chat.domain.shared.application.auth.AuthenticatedAccount;
import team.carrypigeon.backend.chat.domain.features.channel.application.dto.ChannelReadStateResult;
import team.carrypigeon.backend.chat.domain.features.channel.application.dto.ChannelUnreadResult;
import team.carrypigeon.backend.chat.domain.features.channel.application.service.ChannelAccessApplicationService;
import team.carrypigeon.backend.chat.domain.features.channel.application.service.ChannelQueryApplicationService;
import team.carrypigeon.backend.chat.domain.shared.controller.advice.GlobalExceptionHandler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("contract")
class ChannelReadStateControllerTests {

    private ChannelAccessApplicationService channelAccessApplicationService;
    private ChannelQueryApplicationService channelQueryApplicationService;
    private RequestAuthenticationContext authRequestContext;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        channelAccessApplicationService = mock(ChannelAccessApplicationService.class);
        channelQueryApplicationService = mock(ChannelQueryApplicationService.class);
        authRequestContext = new RequestAuthenticationContext();
        mockMvc = MockMvcBuilders.standaloneSetup(new ChannelReadStateController(channelAccessApplicationService, channelQueryApplicationService, authRequestContext))
                .setMessageConverters(snakeCaseConverter())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("update channel read state returns payload")
    void updateChannelReadState_returnsPayload() throws Exception {
        mockMvc = authenticatedMockMvc();
        when(channelAccessApplicationService.updateChannelReadState(any()))
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

    @Test
    @DisplayName("list unreads returns items")
    void listUnreads_returnsItems() throws Exception {
        mockMvc = authenticatedMockMvc();
        when(channelQueryApplicationService.listUnreads(1001L))
                .thenReturn(List.of(new ChannelUnreadResult("9", 3L, 1700000000000L)));

        mockMvc.perform(get("/api/unreads"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].cid").value("9"))
                .andExpect(jsonPath("$.items[0].unread_count").value(3));
    }

    private MockMvc authenticatedMockMvc() {
        return MockMvcBuilders.standaloneSetup(new ChannelReadStateController(channelAccessApplicationService, channelQueryApplicationService, authRequestContext))
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
