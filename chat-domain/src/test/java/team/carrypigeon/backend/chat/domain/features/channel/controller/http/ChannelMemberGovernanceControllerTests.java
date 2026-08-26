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
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.HandlerInterceptor;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.ChannelGovernanceApi;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.ChannelQueryApi;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.DemoteChannelAdminCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.KickChannelMemberCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.PromoteChannelMemberCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.projection.ChannelMemberResult;
import team.carrypigeon.backend.chat.domain.features.channel.domain.query.ListChannelMembersQuery;
import team.carrypigeon.backend.chat.domain.shared.controller.advice.GlobalExceptionHandler;
import team.carrypigeon.backend.chat.domain.shared.controller.support.RequestAuthenticationContext;
import team.carrypigeon.backend.chat.domain.shared.domain.auth.AuthenticatedAccount;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ChannelMemberGovernanceController 协议测试。
 * 职责：验证成员查询、管理员调整和成员移除的 HTTP 映射。
 * 边界：领域查询与治理操作由替身提供，不验证内部业务规则。
 */
@Tag("contract")
class ChannelMemberGovernanceControllerTests {

    private ChannelQueryApi channelQueryDomainApi;
    private ChannelGovernanceApi channelGovernanceDomainApi;
    private RequestAuthenticationContext authRequestContext;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        channelQueryDomainApi = mock(ChannelQueryApi.class);
        channelGovernanceDomainApi = mock(ChannelGovernanceApi.class);
        authRequestContext = new RequestAuthenticationContext();
        mockMvc = authenticatedMockMvc();
    }

    /**
     * 验证成员列表返回 v1 成员字段并传递当前账户与频道 ID。
     */
    @Test
    @DisplayName("list channel members returns items")
    void listChannelMembers_returnsItems() throws Exception {
        when(channelQueryDomainApi.listChannelMembers(any())).thenReturn(List.of(
                new ChannelMemberResult(
                        1001L, "carry-owner", "", "OWNER", Instant.parse("2026-04-24T12:00:00Z"), null
                )
        ));

        mockMvc.perform(get("/api/channels/9/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].uid").value("1001"))
                .andExpect(jsonPath("$.items[0].role").value("owner"));
        ArgumentCaptor<ListChannelMembersQuery> captor = ArgumentCaptor.forClass(ListChannelMembersQuery.class);
        verify(channelQueryDomainApi).listChannelMembers(captor.capture());
        assertEquals(1001L, captor.getValue().accountId());
        assertEquals(9L, captor.getValue().channelId());
    }

    /**
     * 验证设置管理员返回 204 并映射三个领域标识。
     */
    @Test
    @DisplayName("put admin resource returns 204")
    void promoteChannelMemberV1_returns204() throws Exception {
        when(channelGovernanceDomainApi.promoteChannelMember(any())).thenReturn(member("ADMIN"));

        mockMvc.perform(put("/api/channels/9/admins/1002"))
                .andExpect(status().isNoContent());
        ArgumentCaptor<PromoteChannelMemberCommand> captor =
                ArgumentCaptor.forClass(PromoteChannelMemberCommand.class);
        verify(channelGovernanceDomainApi).promoteChannelMember(captor.capture());
        assertCommandIds(captor.getValue().operatorAccountId(), captor.getValue().channelId(),
                captor.getValue().targetAccountId());
    }

    /**
     * 验证撤销管理员返回 204 并映射三个领域标识。
     */
    @Test
    @DisplayName("delete admin resource returns 204")
    void demoteChannelAdminV1_returns204() throws Exception {
        when(channelGovernanceDomainApi.demoteChannelAdmin(any())).thenReturn(member("MEMBER"));

        mockMvc.perform(delete("/api/channels/9/admins/1002"))
                .andExpect(status().isNoContent());
        ArgumentCaptor<DemoteChannelAdminCommand> captor = ArgumentCaptor.forClass(DemoteChannelAdminCommand.class);
        verify(channelGovernanceDomainApi).demoteChannelAdmin(captor.capture());
        assertCommandIds(captor.getValue().operatorAccountId(), captor.getValue().channelId(),
                captor.getValue().targetAccountId());
    }

    /**
     * 验证踢出成员返回 204 并映射三个领域标识。
     */
    @Test
    @DisplayName("kick channel member returns 204")
    void kickChannelMember_returns204() throws Exception {
        doNothing().when(channelGovernanceDomainApi).kickChannelMember(any());

        mockMvc.perform(delete("/api/channels/9/members/1002"))
                .andExpect(status().isNoContent());
        ArgumentCaptor<KickChannelMemberCommand> captor = ArgumentCaptor.forClass(KickChannelMemberCommand.class);
        verify(channelGovernanceDomainApi).kickChannelMember(captor.capture());
        assertCommandIds(captor.getValue().operatorAccountId(), captor.getValue().channelId(),
                captor.getValue().targetAccountId());
    }

    private ChannelMemberResult member(String role) {
        return new ChannelMemberResult(
                1002L, "carry-member", "", role, Instant.parse("2026-04-24T12:00:00Z"), null
        );
    }

    private void assertCommandIds(long operatorAccountId, long channelId, long targetAccountId) {
        assertEquals(1001L, operatorAccountId);
        assertEquals(9L, channelId);
        assertEquals(1002L, targetAccountId);
    }

    private MockMvc authenticatedMockMvc() {
        return MockMvcBuilders.standaloneSetup(new ChannelMemberGovernanceController(
                        channelQueryDomainApi,
                        channelGovernanceDomainApi,
                        authRequestContext
                ))
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

    /** 测试用认证主体绑定拦截器。 */
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
