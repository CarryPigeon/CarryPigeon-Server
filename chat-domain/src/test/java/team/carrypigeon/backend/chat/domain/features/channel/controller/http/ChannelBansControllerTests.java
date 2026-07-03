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
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.HandlerInterceptor;
import team.carrypigeon.backend.chat.domain.shared.controller.support.RequestAuthenticationContext;
import team.carrypigeon.backend.chat.domain.shared.domain.auth.AuthenticatedAccount;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.BanChannelMemberUntilCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.projection.ChannelBanResult;
import team.carrypigeon.backend.chat.domain.features.channel.domain.projection.ChannelBanListItemResult;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.ChannelGovernanceApi;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.ChannelLifecycleApi;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.ChannelQueryApi;
import team.carrypigeon.backend.chat.domain.shared.controller.advice.GlobalExceptionHandler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
/**
 * `ChannelBansController` 契约测试。
 * 职责：验证当前测试类覆盖对象的关键成功路径、失败路径或边界行为。
 */

@Tag("contract")
class ChannelBansControllerTests {

    private ChannelQueryApi channelQueryDomainApi;
    private ChannelLifecycleApi channelLifecycleDomainApi;
    private ChannelGovernanceApi channelGovernanceDomainApi;
    private RequestAuthenticationContext authRequestContext;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        channelQueryDomainApi = mock(ChannelQueryApi.class);
        channelLifecycleDomainApi = mock(ChannelLifecycleApi.class);
        channelGovernanceDomainApi = mock(ChannelGovernanceApi.class);
        authRequestContext = new RequestAuthenticationContext();
        mockMvc = MockMvcBuilders.standaloneSetup(controller())
                .setMessageConverters(snakeCaseConverter())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    /**
     * 验证 `listChannelBans` 在 `returnsItems` 场景下的测试契约。
     */
    @Test
    @DisplayName("list channel bans returns items")
    void listChannelBans_returnsItems() throws Exception {
        mockMvc = authenticatedMockMvc();
        when(channelQueryDomainApi.listChannelBans(any()))
                .thenReturn(List.of(new ChannelBanListItemResult(9L, 1002L, Instant.parse("2026-04-24T12:05:00Z"), "spam", Instant.parse("2026-04-24T12:00:00Z"))));

        mockMvc.perform(get("/api/channels/9/bans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].cid").value("9"))
                .andExpect(jsonPath("$.items[0].uid").value("1002"))
                .andExpect(jsonPath("$.items[0].reason").value("spam"));
    }

    /**
     * 验证 `banChannelMemberV1` 在 `returnsPayload` 场景下的测试契约。
     */
    @Test
    @DisplayName("ban channel member v1 path returns payload")
    void banChannelMemberV1_returnsPayload() throws Exception {
        mockMvc = authenticatedMockMvc();
        when(channelGovernanceDomainApi.banChannelMemberUntil(any()))
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
        ArgumentCaptor<BanChannelMemberUntilCommand> commandCaptor = ArgumentCaptor.forClass(BanChannelMemberUntilCommand.class);
        verify(channelGovernanceDomainApi).banChannelMemberUntil(commandCaptor.capture());
        assertEquals(1776819600000L, commandCaptor.getValue().untilEpochMillis());
    }

    private MockMvc authenticatedMockMvc() {
        return MockMvcBuilders.standaloneSetup(controller())
                .addInterceptors(new BindPrincipalInterceptor(authRequestContext))
                .setMessageConverters(snakeCaseConverter())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private ChannelController controller() {
        return new ChannelController(
                channelQueryDomainApi,
                channelLifecycleDomainApi,
                channelGovernanceDomainApi,
                authRequestContext
        );
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
