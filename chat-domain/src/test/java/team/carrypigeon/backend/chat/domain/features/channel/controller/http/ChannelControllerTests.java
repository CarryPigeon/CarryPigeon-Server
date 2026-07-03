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
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.CreateChannelCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.DeleteChannelCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.DemoteChannelAdminCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.KickChannelMemberCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.PromoteChannelMemberCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.UnbanChannelMemberCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.UpdateChannelProfileCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.projection.ChannelBanListItemResult;
import team.carrypigeon.backend.chat.domain.features.channel.domain.projection.ChannelBanResult;
import team.carrypigeon.backend.chat.domain.features.channel.domain.projection.ChannelMemberResult;
import team.carrypigeon.backend.chat.domain.features.channel.domain.projection.ChannelResult;
import team.carrypigeon.backend.chat.domain.features.channel.domain.projection.DiscoverChannelResult;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.ChannelGovernanceApi;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.ChannelLifecycleApi;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.ChannelQueryApi;
import team.carrypigeon.backend.chat.domain.features.channel.domain.query.ListChannelMembersQuery;
import team.carrypigeon.backend.chat.domain.features.server.domain.api.NotificationPreferenceApi;
import team.carrypigeon.backend.chat.domain.features.server.domain.command.UpdateNotificationChannelPreferenceCommand;
import team.carrypigeon.backend.chat.domain.shared.controller.advice.GlobalExceptionHandler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ChannelController 协议测试。
 * 职责：验证 channels family 当前保留的 v1 资源路径契约。
 * 边界：不验证真实数据库访问，只验证协议层输入输出。
 */
@Tag("contract")
class ChannelControllerTests {

    private ChannelQueryApi channelQueryDomainApi;
    private ChannelLifecycleApi channelLifecycleDomainApi;
    private ChannelGovernanceApi channelGovernanceDomainApi;
    private NotificationPreferenceApi notificationPreferenceDomainApi;
    private RequestAuthenticationContext authRequestContext;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        channelQueryDomainApi = mock(ChannelQueryApi.class);
        channelLifecycleDomainApi = mock(ChannelLifecycleApi.class);
        channelGovernanceDomainApi = mock(ChannelGovernanceApi.class);
        notificationPreferenceDomainApi = mock(NotificationPreferenceApi.class);
        authRequestContext = new RequestAuthenticationContext();
        mockMvc = MockMvcBuilders.standaloneSetup(controller())
                .setMessageConverters(snakeCaseConverter())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    /**
     * 验证频道列表协议会使用当前登录账号查询，并返回 v1 频道摘要列表。
     */
    @Test
    @DisplayName("list channels returns channel summaries")
    void listChannels_returnsChannelSummaries() throws Exception {
        mockMvc = authenticatedMockMvc();
        when(channelQueryDomainApi.listChannels(1001L)).thenReturn(List.of(
                new ChannelResult(1L, 1L, "General", "讨论区", "avatars/ch/1.png", "1001", "public", true, Instant.parse("2026-04-22T00:00:00Z"), Instant.parse("2026-04-22T00:00:00Z")),
                new ChannelResult(9L, 9L, "project-alpha", "研发讨论", "avatars/ch/9.png", "1001", "private", false, Instant.parse("2026-04-24T12:00:00Z"), Instant.parse("2026-04-24T12:00:00Z"))
        ));

        mockMvc.perform(get("/api/channels"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.channels[0].cid").value("1"))
                .andExpect(jsonPath("$.channels[0].owner_uid").value("1001"))
                .andExpect(jsonPath("$.channels[1].cid").value("9"));
    }

    /**
     * 验证频道详情协议会使用当前登录账号与路径频道 ID 查询。
     */
    @Test
    @DisplayName("get channel by id returns channel summary")
    void getChannelById_returnsChannelSummary() throws Exception {
        mockMvc = authenticatedMockMvc();
        when(channelQueryDomainApi.getChannelById(1001L, 9L)).thenReturn(new ChannelResult(
                9L, 9L, "project-alpha", "研发讨论", "avatars/ch/9.png", "1001", "private", false,
                Instant.parse("2026-04-24T12:00:00Z"), Instant.parse("2026-04-24T12:00:00Z")
        ));

        mockMvc.perform(get("/api/channels/9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cid").value("9"))
                .andExpect(jsonPath("$.name").value("project-alpha"))
                .andExpect(jsonPath("$.owner_uid").value("1001"));
    }

    /**
     * 验证频道发现协议会返回游标分页结构和公开发现字段。
     */
    @Test
    @DisplayName("discover channels returns cursor page")
    void discoverChannels_returnsCursorPage() throws Exception {
        mockMvc = authenticatedMockMvc();
        when(channelQueryDomainApi.discoverChannels(any())).thenReturn(List.of(
                new DiscoverChannelResult("9", "General", "讨论区", "avatars/ch/9.png", 42L, false)
        ));

        mockMvc.perform(get("/api/channels/discover").param("q", "gen").param("type", "public").param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].cid").value("9"))
                .andExpect(jsonPath("$.items[0].member_count").value(42))
                .andExpect(jsonPath("$.has_more").value(false));
    }

    /**
     * 验证创建频道协议会返回 201，并把当前账号和请求体字段映射到领域命令。
     */
    @Test
    @DisplayName("create channel returns 201")
    void createChannel_returns201() throws Exception {
        mockMvc = authenticatedMockMvc();
        when(channelLifecycleDomainApi.createChannel(any())).thenReturn(new ChannelResult(
                9L, 9L, "project-alpha", "讨论区", "avatars/ch/9.png", "1001", "private", false,
                Instant.parse("2026-04-24T12:00:00Z"), Instant.parse("2026-04-24T12:00:00Z")
        ));

        mockMvc.perform(post("/api/channels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"project-alpha","brief":"讨论区","avatar":"avatars/ch/9.png"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cid").value("9"))
                .andExpect(jsonPath("$.brief").value("讨论区"));
        ArgumentCaptor<CreateChannelCommand> commandCaptor = ArgumentCaptor.forClass(CreateChannelCommand.class);
        verify(channelLifecycleDomainApi).createChannel(commandCaptor.capture());
        assertEquals(1001L, commandCaptor.getValue().accountId());
        assertEquals("project-alpha", commandCaptor.getValue().name());
        assertEquals("讨论区", commandCaptor.getValue().brief());
        assertEquals("avatars/ch/9.png", commandCaptor.getValue().avatar());
    }

    /**
     * 验证删除频道协议会把当前账号和路径频道 ID 映射到领域命令。
     */
    @Test
    @DisplayName("delete channel returns 204")
    void deleteChannel_returns204() throws Exception {
        mockMvc = authenticatedMockMvc();
        doNothing().when(channelLifecycleDomainApi).deleteChannel(any());

        mockMvc.perform(delete("/api/channels/9"))
                .andExpect(status().isNoContent());
        ArgumentCaptor<DeleteChannelCommand> commandCaptor = ArgumentCaptor.forClass(DeleteChannelCommand.class);
        verify(channelLifecycleDomainApi).deleteChannel(commandCaptor.capture());
        assertEquals(1001L, commandCaptor.getValue().operatorAccountId());
        assertEquals(9L, commandCaptor.getValue().channelId());
    }

    /**
     * 验证更新频道资料协议会把路径频道 ID 与请求体字段映射到领域命令。
     */
    @Test
    @DisplayName("patch channel profile returns 204")
    void patchChannelProfile_returns204() throws Exception {
        mockMvc = authenticatedMockMvc();
        when(channelLifecycleDomainApi.updateChannelProfile(any())).thenReturn(new ChannelResult(
                9L, 9L, "project-alpha", "new brief", "avatars/ch/9.png", "1001", "private", false,
                Instant.parse("2026-04-24T12:00:00Z"), Instant.parse("2026-04-24T12:00:00Z")
        ));

        mockMvc.perform(patch("/api/channels/9")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"project-alpha","brief":"new brief"}
                                """))
                .andExpect(status().isNoContent());
        ArgumentCaptor<UpdateChannelProfileCommand> commandCaptor = ArgumentCaptor.forClass(UpdateChannelProfileCommand.class);
        verify(channelLifecycleDomainApi).updateChannelProfile(commandCaptor.capture());
        assertEquals(1001L, commandCaptor.getValue().operatorAccountId());
        assertEquals(9L, commandCaptor.getValue().channelId());
        assertEquals("project-alpha", commandCaptor.getValue().name());
        assertEquals("new brief", commandCaptor.getValue().brief());
    }

    /**
     * 验证成员列表协议会使用当前账号和路径频道 ID 查询，并返回 v1 成员视图。
     */
    @Test
    @DisplayName("list channel members returns items")
    void listChannelMembers_returnsItems() throws Exception {
        mockMvc = authenticatedMockMvc();
        when(channelQueryDomainApi.listChannelMembers(any())).thenReturn(List.of(
                new ChannelMemberResult(1001L, "carry-owner", "", "OWNER", Instant.parse("2026-04-24T12:00:00Z"), null)
        ));

        mockMvc.perform(get("/api/channels/9/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].uid").value("1001"))
                .andExpect(jsonPath("$.items[0].role").value("owner"));
        ArgumentCaptor<ListChannelMembersQuery> queryCaptor = ArgumentCaptor.forClass(ListChannelMembersQuery.class);
        verify(channelQueryDomainApi).listChannelMembers(queryCaptor.capture());
        assertEquals(1001L, queryCaptor.getValue().accountId());
        assertEquals(9L, queryCaptor.getValue().channelId());
    }

    /**
     * 验证设为管理员协议会把当前账号、频道 ID 和目标账号映射到领域命令。
     */
    @Test
    @DisplayName("put admin resource returns 204")
    void promoteChannelMemberV1_returns204() throws Exception {
        mockMvc = authenticatedMockMvc();
        when(channelGovernanceDomainApi.promoteChannelMember(any())).thenReturn(new ChannelMemberResult(
                1002L, "carry-admin", "", "ADMIN", Instant.parse("2026-04-24T12:00:00Z"), null
        ));

        mockMvc.perform(put("/api/channels/9/admins/1002"))
                .andExpect(status().isNoContent());
        ArgumentCaptor<PromoteChannelMemberCommand> commandCaptor = ArgumentCaptor.forClass(PromoteChannelMemberCommand.class);
        verify(channelGovernanceDomainApi).promoteChannelMember(commandCaptor.capture());
        assertEquals(1001L, commandCaptor.getValue().operatorAccountId());
        assertEquals(9L, commandCaptor.getValue().channelId());
        assertEquals(1002L, commandCaptor.getValue().targetAccountId());
    }

    /**
     * 验证撤销管理员协议会把当前账号、频道 ID 和目标账号映射到领域命令。
     */
    @Test
    @DisplayName("delete admin resource returns 204")
    void demoteChannelAdminV1_returns204() throws Exception {
        mockMvc = authenticatedMockMvc();
        when(channelGovernanceDomainApi.demoteChannelAdmin(any())).thenReturn(new ChannelMemberResult(
                1002L, "carry-member", "", "MEMBER", Instant.parse("2026-04-24T12:00:00Z"), null
        ));

        mockMvc.perform(delete("/api/channels/9/admins/1002"))
                .andExpect(status().isNoContent());
        ArgumentCaptor<DemoteChannelAdminCommand> commandCaptor = ArgumentCaptor.forClass(DemoteChannelAdminCommand.class);
        verify(channelGovernanceDomainApi).demoteChannelAdmin(commandCaptor.capture());
        assertEquals(1001L, commandCaptor.getValue().operatorAccountId());
        assertEquals(9L, commandCaptor.getValue().channelId());
        assertEquals(1002L, commandCaptor.getValue().targetAccountId());
    }

    /**
     * 验证踢出成员协议会把当前账号、频道 ID 和目标账号映射到领域命令。
     */
    @Test
    @DisplayName("kick channel member returns 204")
    void kickChannelMember_returns204() throws Exception {
        mockMvc = authenticatedMockMvc();
        doNothing().when(channelGovernanceDomainApi).kickChannelMember(any());

        mockMvc.perform(delete("/api/channels/9/members/1002"))
                .andExpect(status().isNoContent());
        ArgumentCaptor<KickChannelMemberCommand> commandCaptor = ArgumentCaptor.forClass(KickChannelMemberCommand.class);
        verify(channelGovernanceDomainApi).kickChannelMember(commandCaptor.capture());
        assertEquals(1001L, commandCaptor.getValue().operatorAccountId());
        assertEquals(9L, commandCaptor.getValue().channelId());
        assertEquals(1002L, commandCaptor.getValue().targetAccountId());
    }

    /**
     * 验证封禁成员协议会返回封禁视图，并传递当前账号、目标成员与截止时间。
     */
    @Test
    @DisplayName("put ban resource returns payload")
    void banChannelMemberV1_returnsPayload() throws Exception {
        mockMvc = authenticatedMockMvc();
        when(channelGovernanceDomainApi.banChannelMemberUntil(any()))
                .thenReturn(new ChannelBanResult(9L, 1002L, 1001L, "spam", Instant.parse("2026-04-24T13:00:00Z"), Instant.parse("2026-04-24T12:00:00Z"), null));

        mockMvc.perform(put("/api/channels/9/bans/1002")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"until":1776819600000,"reason":"spam"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cid").value("9"))
                .andExpect(jsonPath("$.uid").value("1002"))
                .andExpect(jsonPath("$.reason").value("spam"));
        ArgumentCaptor<BanChannelMemberUntilCommand> commandCaptor = ArgumentCaptor.forClass(BanChannelMemberUntilCommand.class);
        verify(channelGovernanceDomainApi).banChannelMemberUntil(commandCaptor.capture());
        assertEquals(1001L, commandCaptor.getValue().operatorAccountId());
        assertEquals(9L, commandCaptor.getValue().channelId());
        assertEquals(1002L, commandCaptor.getValue().targetAccountId());
        assertEquals("spam", commandCaptor.getValue().reason());
        assertEquals(1776819600000L, commandCaptor.getValue().untilEpochMillis());
    }

    /**
     * 验证解除封禁协议会把当前账号、频道 ID 和目标账号映射到领域命令。
     */
    @Test
    @DisplayName("delete ban resource returns 204")
    void unbanChannelMember_returns204() throws Exception {
        mockMvc = authenticatedMockMvc();
        when(channelGovernanceDomainApi.unbanChannelMember(any()))
                .thenReturn(new ChannelBanResult(9L, 1002L, 1001L, "spam", Instant.parse("2026-04-24T13:00:00Z"), Instant.parse("2026-04-24T12:00:00Z"), Instant.parse("2026-04-24T12:10:00Z")));

        mockMvc.perform(delete("/api/channels/9/bans/1002"))
                .andExpect(status().isNoContent());
        ArgumentCaptor<UnbanChannelMemberCommand> commandCaptor = ArgumentCaptor.forClass(UnbanChannelMemberCommand.class);
        verify(channelGovernanceDomainApi).unbanChannelMember(commandCaptor.capture());
        assertEquals(1001L, commandCaptor.getValue().operatorAccountId());
        assertEquals(9L, commandCaptor.getValue().channelId());
        assertEquals(1002L, commandCaptor.getValue().targetAccountId());
    }

    /**
     * 验证封禁列表协议会返回指定频道的 v1 封禁条目。
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
                .andExpect(jsonPath("$.items[0].uid").value("1002"));
    }

    /**
     * 验证频道通知偏好协议会把当前账号、频道 ID 和请求体字段映射到领域命令。
     */
    @Test
    @DisplayName("update channel notification preference returns 204")
    void updateChannelNotificationPreference_returns204() throws Exception {
        mockMvc = authenticatedMockMvc();
        doNothing().when(notificationPreferenceDomainApi).updateChannelPreference(any());

        mockMvc.perform(put("/api/channels/9/notification_preference")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"mode":"inherit","muted_until":0}
                                """))
                .andExpect(status().isNoContent());
        ArgumentCaptor<UpdateNotificationChannelPreferenceCommand> commandCaptor =
                ArgumentCaptor.forClass(UpdateNotificationChannelPreferenceCommand.class);
        verify(notificationPreferenceDomainApi).updateChannelPreference(commandCaptor.capture());
        assertEquals(1001L, commandCaptor.getValue().accountId());
        assertEquals(9L, commandCaptor.getValue().channelId());
        assertEquals("inherit", commandCaptor.getValue().mode());
        assertEquals(0L, commandCaptor.getValue().mutedUntil());
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
                notificationPreferenceDomainApi,
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
