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
import team.carrypigeon.backend.chat.domain.features.channel.application.dto.ChannelBanListItemResult;
import team.carrypigeon.backend.chat.domain.features.channel.application.dto.ChannelBanResult;
import team.carrypigeon.backend.chat.domain.features.channel.application.dto.ChannelMemberResult;
import team.carrypigeon.backend.chat.domain.features.channel.application.dto.ChannelResult;
import team.carrypigeon.backend.chat.domain.features.channel.application.dto.DiscoverChannelResult;
import team.carrypigeon.backend.chat.domain.features.channel.application.service.ChannelApplicationService;
import team.carrypigeon.backend.chat.domain.features.server.application.service.NotificationPreferenceApplicationService;
import team.carrypigeon.backend.chat.domain.shared.controller.advice.GlobalExceptionHandler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
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

    private ChannelApplicationService channelApplicationService;
    private NotificationPreferenceApplicationService notificationPreferenceApplicationService;
    private AuthRequestContext authRequestContext;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        channelApplicationService = mock(ChannelApplicationService.class);
        notificationPreferenceApplicationService = mock(NotificationPreferenceApplicationService.class);
        authRequestContext = new AuthRequestContext();
        mockMvc = MockMvcBuilders.standaloneSetup(new ChannelController(channelApplicationService, notificationPreferenceApplicationService, authRequestContext))
                .setMessageConverters(snakeCaseConverter())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("list channels returns channel summaries")
    void listChannels_returnsChannelSummaries() throws Exception {
        mockMvc = authenticatedMockMvc();
        when(channelApplicationService.listChannels(1001L)).thenReturn(List.of(
                new ChannelResult(1L, 1L, "General", "讨论区", "avatars/ch/1.png", "1001", "public", true, Instant.parse("2026-04-22T00:00:00Z"), Instant.parse("2026-04-22T00:00:00Z")),
                new ChannelResult(9L, 9L, "project-alpha", "研发讨论", "avatars/ch/9.png", "1001", "private", false, Instant.parse("2026-04-24T12:00:00Z"), Instant.parse("2026-04-24T12:00:00Z"))
        ));

        mockMvc.perform(get("/api/channels"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.channels[0].cid").value("1"))
                .andExpect(jsonPath("$.channels[0].owner_uid").value("1001"))
                .andExpect(jsonPath("$.channels[1].cid").value("9"));
    }

    @Test
    @DisplayName("get channel by id returns channel summary")
    void getChannelById_returnsChannelSummary() throws Exception {
        mockMvc = authenticatedMockMvc();
        when(channelApplicationService.getChannelById(1001L, 9L)).thenReturn(new ChannelResult(
                9L, 9L, "project-alpha", "研发讨论", "avatars/ch/9.png", "1001", "private", false,
                Instant.parse("2026-04-24T12:00:00Z"), Instant.parse("2026-04-24T12:00:00Z")
        ));

        mockMvc.perform(get("/api/channels/9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cid").value("9"))
                .andExpect(jsonPath("$.name").value("project-alpha"))
                .andExpect(jsonPath("$.owner_uid").value("1001"));
    }

    @Test
    @DisplayName("discover channels returns cursor page")
    void discoverChannels_returnsCursorPage() throws Exception {
        mockMvc = authenticatedMockMvc();
        when(channelApplicationService.discoverChannels(any())).thenReturn(List.of(
                new DiscoverChannelResult("9", "General", "讨论区", "avatars/ch/9.png", 42L, false)
        ));

        mockMvc.perform(get("/api/channels/discover").param("q", "gen").param("type", "text").param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].cid").value("9"))
                .andExpect(jsonPath("$.items[0].member_count").value(42))
                .andExpect(jsonPath("$.has_more").value(false));
    }

    @Test
    @DisplayName("create channel returns 201")
    void createChannel_returns201() throws Exception {
        mockMvc = authenticatedMockMvc();
        when(channelApplicationService.createChannel(any())).thenReturn(new ChannelResult(
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
    }

    @Test
    @DisplayName("delete channel returns 204")
    void deleteChannel_returns204() throws Exception {
        mockMvc = authenticatedMockMvc();
        doNothing().when(channelApplicationService).deleteChannel(any());

        mockMvc.perform(delete("/api/channels/9"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("patch channel profile returns 204")
    void patchChannelProfile_returns204() throws Exception {
        mockMvc = authenticatedMockMvc();
        when(channelApplicationService.updateChannelProfile(any())).thenReturn(new ChannelResult(
                9L, 9L, "project-alpha", "new brief", "avatars/ch/9.png", "1001", "private", false,
                Instant.parse("2026-04-24T12:00:00Z"), Instant.parse("2026-04-24T12:00:00Z")
        ));

        mockMvc.perform(patch("/api/channels/9")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"project-alpha","brief":"new brief"}
                                """))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("list channel members returns items")
    void listChannelMembers_returnsItems() throws Exception {
        mockMvc = authenticatedMockMvc();
        when(channelApplicationService.listChannelMembers(any())).thenReturn(List.of(
                new ChannelMemberResult(1001L, "carry-owner", "", "OWNER", Instant.parse("2026-04-24T12:00:00Z"), null)
        ));

        mockMvc.perform(get("/api/channels/9/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].uid").value("1001"))
                .andExpect(jsonPath("$.items[0].role").value("owner"));
    }

    @Test
    @DisplayName("put admin resource returns 204")
    void promoteChannelMemberV1_returns204() throws Exception {
        mockMvc = authenticatedMockMvc();
        when(channelApplicationService.promoteChannelMember(any())).thenReturn(new ChannelMemberResult(
                1002L, "carry-admin", "", "ADMIN", Instant.parse("2026-04-24T12:00:00Z"), null
        ));

        mockMvc.perform(put("/api/channels/9/admins/1002"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("delete admin resource returns 204")
    void demoteChannelAdminV1_returns204() throws Exception {
        mockMvc = authenticatedMockMvc();
        when(channelApplicationService.demoteChannelAdmin(any())).thenReturn(new ChannelMemberResult(
                1002L, "carry-member", "", "MEMBER", Instant.parse("2026-04-24T12:00:00Z"), null
        ));

        mockMvc.perform(delete("/api/channels/9/admins/1002"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("kick channel member returns 204")
    void kickChannelMember_returns204() throws Exception {
        mockMvc = authenticatedMockMvc();
        doNothing().when(channelApplicationService).kickChannelMember(any());

        mockMvc.perform(delete("/api/channels/9/members/1002"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("put ban resource returns payload")
    void banChannelMemberV1_returnsPayload() throws Exception {
        mockMvc = authenticatedMockMvc();
        when(channelApplicationService.banChannelMember(any()))
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
    }

    @Test
    @DisplayName("delete ban resource returns 204")
    void unbanChannelMember_returns204() throws Exception {
        mockMvc = authenticatedMockMvc();
        when(channelApplicationService.unbanChannelMember(any()))
                .thenReturn(new ChannelBanResult(9L, 1002L, 1001L, "spam", Instant.parse("2026-04-24T13:00:00Z"), Instant.parse("2026-04-24T12:00:00Z"), Instant.parse("2026-04-24T12:10:00Z")));

        mockMvc.perform(delete("/api/channels/9/bans/1002"))
                .andExpect(status().isNoContent());
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
                .andExpect(jsonPath("$.items[0].uid").value("1002"));
    }

    @Test
    @DisplayName("update channel notification preference returns 204")
    void updateChannelNotificationPreference_returns204() throws Exception {
        mockMvc = authenticatedMockMvc();
        doNothing().when(notificationPreferenceApplicationService).updateChannelPreference(any());

        mockMvc.perform(put("/api/channels/9/notification_preference")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"mode":"inherit","muted_until":0}
                                """))
                .andExpect(status().isNoContent());
    }

    private MockMvc authenticatedMockMvc() {
        return MockMvcBuilders.standaloneSetup(new ChannelController(channelApplicationService, notificationPreferenceApplicationService, authRequestContext))
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
