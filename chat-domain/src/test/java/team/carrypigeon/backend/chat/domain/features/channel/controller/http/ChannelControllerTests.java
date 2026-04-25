package team.carrypigeon.backend.chat.domain.features.channel.controller.http;

import java.time.Instant;
import java.util.List;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.HandlerInterceptor;
import team.carrypigeon.backend.chat.domain.features.auth.controller.support.AuthRequestContext;
import team.carrypigeon.backend.chat.domain.features.auth.controller.support.AuthenticatedPrincipal;
import team.carrypigeon.backend.chat.domain.features.channel.application.dto.ChannelBanResult;
import team.carrypigeon.backend.chat.domain.features.channel.application.dto.ChannelInviteResult;
import team.carrypigeon.backend.chat.domain.features.channel.application.dto.ChannelMemberResult;
import team.carrypigeon.backend.chat.domain.features.channel.application.dto.ChannelOwnershipTransferResult;
import team.carrypigeon.backend.chat.domain.features.channel.application.dto.ChannelResult;
import team.carrypigeon.backend.chat.domain.features.channel.application.service.ChannelApplicationService;
import team.carrypigeon.backend.chat.domain.shared.controller.advice.GlobalExceptionHandler;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ChannelController 协议测试。
 * 职责：验证频道默认查询、private channel 创建、邀请接受与成员列表入口的统一响应码与异常映射契约。
 * 边界：不验证真实数据库访问，只验证协议层请求到响应的稳定行为。
 */
@Tag("contract")
class ChannelControllerTests {

    private ChannelApplicationService channelApplicationService;
    private AuthRequestContext authRequestContext;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        channelApplicationService = mock(ChannelApplicationService.class);
        authRequestContext = new AuthRequestContext();
        mockMvc = MockMvcBuilders.standaloneSetup(new ChannelController(channelApplicationService, authRequestContext))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    /**
     * 验证已认证请求可以读取默认频道。
     */
    @Test
    @DisplayName("get default channel authenticated request returns code 100")
    void getDefaultChannel_authenticatedRequest_returnsCode100() throws Exception {
        mockMvc = authenticatedMockMvc();
        when(channelApplicationService.getDefaultChannel(any())).thenReturn(new ChannelResult(
                1L, 1L, "public", "public", true,
                Instant.parse("2026-04-22T00:00:00Z"), Instant.parse("2026-04-22T00:00:00Z")
        ));

        mockMvc.perform(get("/api/channels/default"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(100))
                .andExpect(jsonPath("$.data.channelId").value(1L))
                .andExpect(jsonPath("$.data.type").value("public"));
    }

    /**
     * 验证已认证请求可以读取 system 频道。
     */
    @Test
    @DisplayName("get system channel authenticated request returns code 100")
    void getSystemChannel_authenticatedRequest_returnsCode100() throws Exception {
        mockMvc = authenticatedMockMvc();
        when(channelApplicationService.getSystemChannel(any())).thenReturn(new ChannelResult(
                2L, 2L, "system", "system", false,
                Instant.parse("2026-04-22T00:00:00Z"), Instant.parse("2026-04-22T00:00:00Z")
        ));

        mockMvc.perform(get("/api/channels/system"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(100))
                .andExpect(jsonPath("$.data.channelId").value(2L))
                .andExpect(jsonPath("$.data.type").value("system"));
    }

    /**
     * 验证已认证请求可以创建 private channel。
     */
    @Test
    @DisplayName("create private channel authenticated request returns code 100")
    void createPrivateChannel_authenticatedRequest_returnsCode100() throws Exception {
        mockMvc = authenticatedMockMvc();
        when(channelApplicationService.createPrivateChannel(any())).thenReturn(new ChannelResult(
                9L, 9L, "project-alpha", "private", false,
                Instant.parse("2026-04-24T12:00:00Z"), Instant.parse("2026-04-24T12:00:00Z")
        ));

        mockMvc.perform(post("/api/channels/private")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"name\":\"project-alpha\"" +
                                "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(100))
                .andExpect(jsonPath("$.data.channelId").value(9L))
                .andExpect(jsonPath("$.data.type").value("private"));
    }

    /**
     * 验证 OWNER / ADMIN 邀请接口会返回邀请结果。
     */
    @Test
    @DisplayName("invite channel member authenticated request returns code 100")
    void inviteChannelMember_authenticatedRequest_returnsCode100() throws Exception {
        mockMvc = authenticatedMockMvc();
        when(channelApplicationService.inviteChannelMember(any())).thenReturn(new ChannelInviteResult(
                9L,
                1002L,
                1001L,
                "PENDING",
                Instant.parse("2026-04-24T12:00:00Z"),
                null
        ));

        mockMvc.perform(post("/api/channels/9/invites")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"inviteeAccountId\":1002" +
                                "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(100))
                .andExpect(jsonPath("$.data.channelId").value(9L))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    /**
     * 验证接受邀请接口会返回 ACCEPTED 结果。
     */
    @Test
    @DisplayName("accept channel invite authenticated request returns code 100")
    void acceptChannelInvite_authenticatedRequest_returnsCode100() throws Exception {
        mockMvc = authenticatedMockMvc();
        when(channelApplicationService.acceptChannelInvite(any())).thenReturn(new ChannelInviteResult(
                9L,
                1001L,
                1002L,
                "ACCEPTED",
                Instant.parse("2026-04-24T12:00:00Z"),
                Instant.parse("2026-04-24T12:01:00Z")
        ));

        mockMvc.perform(post("/api/channels/9/invites/accept"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(100))
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"));
    }

    /**
     * 验证活跃成员可以获取频道成员列表。
     */
    @Test
    @DisplayName("list channel members authenticated request returns code 100")
    void listChannelMembers_authenticatedRequest_returnsCode100() throws Exception {
        mockMvc = authenticatedMockMvc();
        when(channelApplicationService.listChannelMembers(any())).thenReturn(List.of(
                new ChannelMemberResult(
                        1001L,
                        "carry-owner",
                        "",
                        "OWNER",
                        Instant.parse("2026-04-24T12:00:00Z"),
                        null
                )
        ));

        mockMvc.perform(get("/api/channels/9/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(100))
                .andExpect(jsonPath("$.data[0].accountId").value(1001L))
                .andExpect(jsonPath("$.data[0].role").value("OWNER"));
    }

    /**
     * 验证提升频道成员为 ADMIN 的接口会返回 100 响应码。
     */
    @Test
    @DisplayName("promote channel member authenticated request returns code 100")
    void promoteChannelMember_authenticatedRequest_returnsCode100() throws Exception {
        mockMvc = authenticatedMockMvc();
        when(channelApplicationService.promoteChannelMember(any())).thenReturn(new ChannelMemberResult(
                1002L,
                "carry-admin",
                "",
                "ADMIN",
                Instant.parse("2026-04-24T12:00:00Z"),
                null
        ));

        mockMvc.perform(post("/api/channels/9/members/1002/admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(100))
                .andExpect(jsonPath("$.data.accountId").value(1002L))
                .andExpect(jsonPath("$.data.role").value("ADMIN"));
    }

    /**
     * 验证转移频道所有权的接口会返回 100 响应码。
     */
    @Test
    @DisplayName("transfer channel ownership authenticated request returns code 100")
    void transferChannelOwnership_authenticatedRequest_returnsCode100() throws Exception {
        mockMvc = authenticatedMockMvc();
        when(channelApplicationService.transferChannelOwnership(any())).thenReturn(
                new team.carrypigeon.backend.chat.domain.features.channel.application.dto.ChannelOwnershipTransferResult(
                        9L,
                        1001L,
                        "ADMIN",
                        1002L,
                        "OWNER"
                )
        );

        mockMvc.perform(post("/api/channels/9/ownership-transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"targetAccountId\":1002" +
                                "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(100))
                .andExpect(jsonPath("$.data.previousOwnerRole").value("ADMIN"))
                .andExpect(jsonPath("$.data.newOwnerRole").value("OWNER"));
    }

    /**
     * 验证禁言成员的接口会返回 100 响应码。
     */
    @Test
    @DisplayName("mute channel member authenticated request returns code 100")
    void muteChannelMember_authenticatedRequest_returnsCode100() throws Exception {
        mockMvc = authenticatedMockMvc();
        when(channelApplicationService.muteChannelMember(any())).thenReturn(new ChannelMemberResult(
                1002L,
                "carry-member",
                "",
                "MEMBER",
                Instant.parse("2026-04-24T12:00:00Z"),
                Instant.parse("2026-04-24T12:05:00Z")
        ));

        mockMvc.perform(post("/api/channels/9/members/1002/mute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"durationSeconds\":300" +
                                "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(100))
                .andExpect(jsonPath("$.data.accountId").value(1002L));
    }

    /**
     * 验证踢出成员的接口会返回 100 响应码。
     */
    @Test
    @DisplayName("kick channel member authenticated request returns code 100")
    void kickChannelMember_authenticatedRequest_returnsCode100() throws Exception {
        mockMvc = authenticatedMockMvc();

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/channels/9/members/1002"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(100));
    }

    /**
     * 验证封禁成员的接口会返回 100 响应码。
     */
    @Test
    @DisplayName("ban channel member authenticated request returns code 100")
    void banChannelMember_authenticatedRequest_returnsCode100() throws Exception {
        mockMvc = authenticatedMockMvc();
        when(channelApplicationService.banChannelMember(any())).thenReturn(
                new team.carrypigeon.backend.chat.domain.features.channel.application.dto.ChannelBanResult(
                        9L,
                        1002L,
                        1001L,
                        "spam",
                        Instant.parse("2026-04-24T12:10:00Z"),
                        Instant.parse("2026-04-24T12:00:00Z"),
                        null
                )
        );

        mockMvc.perform(post("/api/channels/9/bans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"targetAccountId\":1002," +
                                "\"reason\":\"spam\"," +
                                "\"durationSeconds\":600" +
                                "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(100))
                .andExpect(jsonPath("$.data.bannedAccountId").value(1002L));
    }

    /**
     * 验证解除封禁的接口会返回 100 响应码。
     */
    @Test
    @DisplayName("unban channel member authenticated request returns code 100")
    void unbanChannelMember_authenticatedRequest_returnsCode100() throws Exception {
        mockMvc = authenticatedMockMvc();
        when(channelApplicationService.unbanChannelMember(any())).thenReturn(
                new team.carrypigeon.backend.chat.domain.features.channel.application.dto.ChannelBanResult(
                        9L,
                        1002L,
                        1001L,
                        "spam",
                        Instant.parse("2026-04-24T12:10:00Z"),
                        Instant.parse("2026-04-24T12:00:00Z"),
                        Instant.parse("2026-04-24T12:05:00Z")
                )
        );

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/channels/9/bans/1002"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(100))
                .andExpect(jsonPath("$.data.revokedAt").exists());
    }

    private MockMvc authenticatedMockMvc() {
        return MockMvcBuilders.standaloneSetup(new ChannelController(channelApplicationService, authRequestContext))
                .addInterceptors(new BindPrincipalInterceptor(authRequestContext))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
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
