package team.carrypigeon.backend.chat.domain.features.channel.controller.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
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
import team.carrypigeon.backend.chat.domain.features.auth.controller.support.AuthRequestContext;
import team.carrypigeon.backend.chat.domain.features.auth.controller.support.AuthenticatedPrincipal;
import team.carrypigeon.backend.chat.domain.features.channel.application.dto.ChannelBanResult;
import team.carrypigeon.backend.chat.domain.features.channel.application.dto.ChannelBanListItemResult;
import team.carrypigeon.backend.chat.domain.features.channel.application.service.ChannelApplicationService;
import team.carrypigeon.backend.chat.domain.shared.controller.advice.GlobalExceptionHandler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("contract")
class ChannelBansControllerTests {

    private ChannelApplicationService channelApplicationService;
    private AuthRequestContext authRequestContext;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        channelApplicationService = mock(ChannelApplicationService.class);
        authRequestContext = new AuthRequestContext();
        mockMvc = MockMvcBuilders.standaloneSetup(new ChannelController(channelApplicationService, authRequestContext))
                .setMessageConverters(snakeCaseConverter())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("list channel bans returns items")
    void listChannelBans_returnsItems() throws Exception {
        mockMvc = authenticatedMockMvc();
        when(channelApplicationService.listChannelBans(any()))
                .thenReturn(List.of(new ChannelBanListItemResult(9L, 1002L, Instant.parse("2026-04-24T12:05:00Z"), "spam", Instant.parse("2026-04-24T12:00:00Z"))));

        mockMvc.perform(get("/api/channels/9/bans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].cid").value("9"))
                .andExpect(jsonPath("$.items[0].uid").value("1002"))
                .andExpect(jsonPath("$.items[0].reason").value("spam"));
    }

    @Test
    @DisplayName("ban channel member v1 path returns payload")
    void banChannelMemberV1_returnsPayload() throws Exception {
        mockMvc = authenticatedMockMvc();
        when(channelApplicationService.banChannelMember(any()))
                .thenReturn(new ChannelBanResult(9L, 1002L, 1001L, "spam", Instant.parse("2026-04-24T13:00:00Z"), Instant.parse("2026-04-24T12:00:00Z"), null));

        mockMvc.perform(put("/api/channels/9/bans/1002")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"until\":1776819600000," +
                                "\"reason\":\"spam\"" +
                                "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cid").value("9"))
                .andExpect(jsonPath("$.uid").value("1002"))
                .andExpect(jsonPath("$.reason").value("spam"));
    }

    private MockMvc authenticatedMockMvc() {
        return MockMvcBuilders.standaloneSetup(new ChannelController(channelApplicationService, authRequestContext))
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
