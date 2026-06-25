package team.carrypigeon.backend.chat.domain.features.channel.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.BanChannelMemberCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.DemoteChannelAdminCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.KickChannelMemberCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.MuteChannelMemberCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.PromoteChannelMemberCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.TransferChannelOwnershipCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.UnbanChannelMemberCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.UnmuteChannelMemberCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.query.ListChannelBansQuery;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelBan;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMember;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMemberRole;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.chat.domain.features.channel.application.service.ChannelApplicationServiceTestSupport.TestContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static team.carrypigeon.backend.chat.domain.features.channel.application.service.ChannelApplicationServiceTestSupport.BASE_TIME;
import static team.carrypigeon.backend.chat.domain.features.channel.application.service.ChannelApplicationServiceTestSupport.newContext;
import static team.carrypigeon.backend.chat.domain.features.channel.application.service.ChannelApplicationServiceTestSupport.privateChannel;
import static team.carrypigeon.backend.chat.domain.features.channel.application.service.ChannelApplicationServiceTestSupport.profile;

/**
 * 频道治理操作契约测试。
 * 职责：验证成员治理、所有权转移、禁言、封禁与封禁列表相关的应用层语义。
 * 边界：不验证 HTTP 协议层与真实数据库访问，只使用内存替身验证业务语义。
 */
@Tag("contract")
class ChannelApplicationServiceGovernanceTests {

    /**
     * 验证 OWNER 可以读取封禁列表。
     */
    @Test
    @DisplayName("list channel bans owner returns ban items")
    void listChannelBans_owner_returnsBanItems() {
        TestContext context = newContext();
        context.channelRepository.channels.put(9L, privateChannel(9L, "project-alpha"));
        context.channelMemberRepository.save(new ChannelMember(9L, 1001L, ChannelMemberRole.OWNER, BASE_TIME, null));
        context.channelBanRepository.channelBan = new ChannelBan(9L, 1002L, 1001L, "spam", BASE_TIME.plusSeconds(300), BASE_TIME, null);
        ChannelQueryApplicationService service = context.createQueryService();

        var result = service.listChannelBans(new ListChannelBansQuery(1001L, 9L));

        assertEquals(1, result.size());
        assertEquals(1002L, result.getFirst().bannedAccountId());
        assertEquals("spam", result.getFirst().reason());
    }

    /**
     * 验证 OWNER 可以把 MEMBER 提升为 ADMIN，并写入审计日志。
     */
    @Test
    @DisplayName("promote channel member owner promotes member to admin")
    void promoteChannelMember_owner_promotesMemberToAdmin() {
        TestContext context = newContext();
        context.channelRepository.channels.put(9L, privateChannel(9L, "project-alpha"));
        context.channelMemberRepository.save(new ChannelMember(9L, 1001L, ChannelMemberRole.OWNER, BASE_TIME, null));
        context.channelMemberRepository.save(new ChannelMember(9L, 1002L, ChannelMemberRole.MEMBER, BASE_TIME.plusSeconds(60), null));
        context.userProfileRepository.save(profile(1002L, "carry-admin"));
        ChannelGovernanceApplicationService service = context.createGovernanceService();

        var result = service.promoteChannelMember(new PromoteChannelMemberCommand(1001L, 9L, 1002L));

        assertEquals("ADMIN", result.role());
        assertEquals(ChannelMemberRole.ADMIN, context.channelMemberRepository.findByChannelIdAndAccountId(9L, 1002L).orElseThrow().role());
        assertEquals("MEMBER_PROMOTED_TO_ADMIN", context.channelAuditLogRepository.logs.getFirst().actionType());
    }

    /**
     * 验证 OWNER 可以把 ADMIN 降级为 MEMBER。
     */
    @Test
    @DisplayName("demote channel admin owner demotes admin to member")
    void demoteChannelAdmin_owner_demotesAdminToMember() {
        TestContext context = newContext();
        context.channelRepository.channels.put(9L, privateChannel(9L, "project-alpha"));
        context.channelMemberRepository.save(new ChannelMember(9L, 1001L, ChannelMemberRole.OWNER, BASE_TIME, null));
        context.channelMemberRepository.save(new ChannelMember(9L, 1002L, ChannelMemberRole.ADMIN, BASE_TIME.plusSeconds(60), null));
        context.userProfileRepository.save(profile(1002L, "carry-member"));
        ChannelGovernanceApplicationService service = context.createGovernanceService();

        var result = service.demoteChannelAdmin(new DemoteChannelAdminCommand(1001L, 9L, 1002L));

        assertEquals("MEMBER", result.role());
        assertEquals(ChannelMemberRole.MEMBER, context.channelMemberRepository.findByChannelIdAndAccountId(9L, 1002L).orElseThrow().role());
    }

    /**
     * 验证 OWNER 转移所有权后自己会退回 ADMIN，目标成为 OWNER。
     */
    @Test
    @DisplayName("transfer channel ownership owner transfers and keeps previous owner as admin")
    void transferChannelOwnership_owner_transfersAndKeepsPreviousOwnerAsAdmin() {
        TestContext context = newContext();
        context.channelRepository.channels.put(9L, privateChannel(9L, "project-alpha"));
        context.channelMemberRepository.save(new ChannelMember(9L, 1001L, ChannelMemberRole.OWNER, BASE_TIME, null));
        context.channelMemberRepository.save(new ChannelMember(9L, 1002L, ChannelMemberRole.ADMIN, BASE_TIME.plusSeconds(60), null));
        ChannelGovernanceApplicationService service = context.createGovernanceService();

        var result = service.transferChannelOwnership(new TransferChannelOwnershipCommand(1001L, 9L, 1002L));

        assertEquals("ADMIN", result.previousOwnerRole());
        assertEquals("OWNER", result.newOwnerRole());
        assertEquals(ChannelMemberRole.ADMIN, context.channelMemberRepository.findByChannelIdAndAccountId(9L, 1001L).orElseThrow().role());
        assertEquals(ChannelMemberRole.OWNER, context.channelMemberRepository.findByChannelIdAndAccountId(9L, 1002L).orElseThrow().role());
    }

    /**
     * 验证 OWNER / ADMIN 可以禁言 MEMBER，并保留禁言截止时间。
     */
    @Test
    @DisplayName("mute channel member owner mutes member until future instant")
    void muteChannelMember_owner_mutesMemberUntilFutureInstant() {
        TestContext context = newContext();
        context.channelRepository.channels.put(9L, privateChannel(9L, "project-alpha"));
        context.channelMemberRepository.save(new ChannelMember(9L, 1001L, ChannelMemberRole.OWNER, BASE_TIME, null));
        context.channelMemberRepository.save(new ChannelMember(9L, 1002L, ChannelMemberRole.MEMBER, BASE_TIME.plusSeconds(60), null));
        context.userProfileRepository.save(profile(1002L, "carry-member"));
        ChannelGovernanceApplicationService service = context.createGovernanceService();

        var result = service.muteChannelMember(new MuteChannelMemberCommand(1001L, 9L, 1002L, 300));

        assertEquals(BASE_TIME.plusSeconds(300), result.mutedUntil());
        assertEquals(BASE_TIME.plusSeconds(300), context.channelMemberRepository.findByChannelIdAndAccountId(9L, 1002L).orElseThrow().mutedUntil());
    }

    /**
     * 验证解除禁言会把 mutedUntil 清空。
     */
    @Test
    @DisplayName("unmute channel member owner clears muted until")
    void unmuteChannelMember_owner_clearsMutedUntil() {
        TestContext context = newContext();
        context.channelRepository.channels.put(9L, privateChannel(9L, "project-alpha"));
        context.channelMemberRepository.save(new ChannelMember(9L, 1001L, ChannelMemberRole.OWNER, BASE_TIME, null));
        context.channelMemberRepository.save(new ChannelMember(9L, 1002L, ChannelMemberRole.MEMBER, BASE_TIME.plusSeconds(60), BASE_TIME.plusSeconds(300)));
        context.userProfileRepository.save(profile(1002L, "carry-member"));
        ChannelGovernanceApplicationService service = context.createGovernanceService();

        var result = service.unmuteChannelMember(new UnmuteChannelMemberCommand(1001L, 9L, 1002L));

        assertEquals(null, result.mutedUntil());
        assertEquals(null, context.channelMemberRepository.findByChannelIdAndAccountId(9L, 1002L).orElseThrow().mutedUntil());
    }

    /**
     * 验证踢人会删除活跃成员投影。
     */
    @Test
    @DisplayName("kick channel member owner removes active membership")
    void kickChannelMember_owner_removesActiveMembership() {
        TestContext context = newContext();
        context.channelRepository.channels.put(9L, privateChannel(9L, "project-alpha"));
        context.channelMemberRepository.save(new ChannelMember(9L, 1001L, ChannelMemberRole.OWNER, BASE_TIME, null));
        context.channelMemberRepository.save(new ChannelMember(9L, 1002L, ChannelMemberRole.MEMBER, BASE_TIME.plusSeconds(60), null));
        ChannelGovernanceApplicationService service = context.createGovernanceService();

        service.kickChannelMember(new KickChannelMemberCommand(1001L, 9L, 1002L));

        assertEquals(false, context.channelMemberRepository.exists(9L, 1002L));
        assertEquals("MEMBER_KICKED", context.channelAuditLogRepository.logs.getFirst().actionType());
        assertTrue(context.channelRealtimePublisher.channelChangedScopes.contains("members"));
        assertTrue(context.channelRealtimePublisher.channelsChangedAccountIds.contains(1002L));
    }

    /**
     * 验证封禁会写入 ban 记录并移除活跃成员。
     */
    @Test
    @DisplayName("ban channel member owner saves ban and removes membership")
    void banChannelMember_owner_savesBanAndRemovesMembership() {
        TestContext context = newContext();
        context.channelRepository.channels.put(9L, privateChannel(9L, "project-alpha"));
        context.channelMemberRepository.save(new ChannelMember(9L, 1001L, ChannelMemberRole.OWNER, BASE_TIME, null));
        context.channelMemberRepository.save(new ChannelMember(9L, 1002L, ChannelMemberRole.MEMBER, BASE_TIME.plusSeconds(60), null));
        ChannelGovernanceApplicationService service = context.createGovernanceService();

        var result = service.banChannelMember(new BanChannelMemberCommand(1001L, 9L, 1002L, "spam", 600L));

        assertEquals(1002L, result.bannedAccountId());
        assertEquals(BASE_TIME.plusSeconds(600), result.expiresAt());
        assertEquals(false, context.channelMemberRepository.exists(9L, 1002L));
        assertEquals(1002L, context.channelBanRepository.channelBan.bannedAccountId());
        assertTrue(context.channelRealtimePublisher.channelChangedScopes.contains("bans"));
        assertTrue(context.channelRealtimePublisher.channelsChangedAccountIds.contains(1002L));
    }

    /**
     * 验证解封会更新 revokedAt 并返回最新 ban 结果。
     */
    @Test
    @DisplayName("unban channel member active ban updates revoked at")
    void unbanChannelMember_activeBan_updatesRevokedAt() {
        TestContext context = newContext();
        context.channelRepository.channels.put(9L, privateChannel(9L, "project-alpha"));
        context.channelMemberRepository.save(new ChannelMember(9L, 1001L, ChannelMemberRole.OWNER, BASE_TIME, null));
        context.channelBanRepository.channelBan = new ChannelBan(9L, 1002L, 1001L, "spam", null, BASE_TIME, null);
        ChannelGovernanceApplicationService service = context.createGovernanceService();

        var result = service.unbanChannelMember(new UnbanChannelMemberCommand(1001L, 9L, 1002L));

        assertEquals(BASE_TIME, result.revokedAt());
        assertEquals(BASE_TIME, context.channelBanRepository.channelBan.revokedAt());
    }

    /**
     * 验证 ADMIN 不能直接禁言 OWNER。
     */
    @Test
    @DisplayName("mute channel member admin target owner throws forbidden problem")
    void muteChannelMember_adminTargetOwner_throwsForbiddenProblem() {
        TestContext context = newContext();
        context.channelRepository.channels.put(9L, privateChannel(9L, "project-alpha"));
        context.channelMemberRepository.save(new ChannelMember(9L, 1001L, ChannelMemberRole.ADMIN, BASE_TIME, null));
        context.channelMemberRepository.save(new ChannelMember(9L, 1002L, ChannelMemberRole.OWNER, BASE_TIME.plusSeconds(60), null));
        ChannelGovernanceApplicationService service = context.createGovernanceService();

        ProblemException exception = assertThrows(
                ProblemException.class,
                () -> service.muteChannelMember(new MuteChannelMemberCommand(1001L, 9L, 1002L, 300))
        );

        assertEquals("target member with OWNER role cannot be moderated", exception.getMessage());
    }
}
