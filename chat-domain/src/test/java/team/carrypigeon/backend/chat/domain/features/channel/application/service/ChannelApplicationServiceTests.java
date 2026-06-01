package team.carrypigeon.backend.chat.domain.features.channel.application.service;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.AcceptChannelInviteCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.BanChannelMemberCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.CreatePrivateChannelCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.CreateChannelApplicationCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.DecideChannelApplicationCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.DeleteChannelCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.DemoteChannelAdminCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.GetDefaultChannelCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.GetSystemChannelCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.InviteChannelMemberCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.KickChannelMemberCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.MuteChannelMemberCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.PromoteChannelMemberCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.TransferChannelOwnershipCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.UpdateChannelProfileCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.UnbanChannelMemberCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.UnmuteChannelMemberCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.dto.ChannelBanResult;
import team.carrypigeon.backend.chat.domain.features.channel.application.dto.ChannelBanListItemResult;
import team.carrypigeon.backend.chat.domain.features.channel.application.dto.ChannelApplicationResult;
import team.carrypigeon.backend.chat.domain.features.channel.application.dto.ChannelInviteResult;
import team.carrypigeon.backend.chat.domain.features.channel.application.dto.ChannelMemberResult;
import team.carrypigeon.backend.chat.domain.features.channel.application.dto.ChannelOwnershipTransferResult;
import team.carrypigeon.backend.chat.domain.features.channel.application.dto.ChannelResult;
import team.carrypigeon.backend.chat.domain.features.channel.application.query.ListChannelMembersQuery;
import team.carrypigeon.backend.chat.domain.features.channel.application.query.ListChannelApplicationsQuery;
import team.carrypigeon.backend.chat.domain.features.channel.application.query.ListChannelBansQuery;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.Channel;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelAuditLog;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelBan;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelInvite;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelInviteStatus;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMember;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMemberRole;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelAuditLogRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelBanRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelInviteRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelMemberRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelReadStateRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.service.ChannelGovernancePolicy;
import team.carrypigeon.backend.chat.domain.features.channel.domain.service.ChannelRealtimePublisher;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelReadState;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelUnread;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MessageRepository;
import team.carrypigeon.backend.chat.domain.features.user.domain.model.UserProfile;
import team.carrypigeon.backend.chat.domain.features.user.domain.repository.UserProfileRepository;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.id.IdGenerator;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;
import team.carrypigeon.backend.infrastructure.service.database.api.transaction.TransactionRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ChannelApplicationService 契约测试。
 * 职责：验证默认频道、private channel 创建、邀请接受与成员列表的应用层编排契约。
 * 边界：不验证 HTTP 协议层与真实数据库访问，只使用内存替身验证业务语义。
 */
@Tag("contract")
class ChannelApplicationServiceTests {

    private static final Instant BASE_TIME = Instant.parse("2026-04-24T12:00:00Z");

    /**
     * 验证默认频道存在时会返回稳定频道结果。
     */
    @Test
    @DisplayName("get default channel existing channel returns result")
    void getDefaultChannel_existingChannel_returnsResult() {
        TestContext context = new TestContext();
        ChannelApplicationService service = context.createService();

        ChannelResult result = service.getDefaultChannel(new GetDefaultChannelCommand(1001L));

        assertEquals(1L, result.channelId());
        assertEquals(1L, result.conversationId());
        assertEquals("public", result.name());
        assertEquals("1001", result.ownerUid());
        assertEquals("public", result.type());
    }

    /**
     * 验证 system 频道存在且当前账户为成员时会返回稳定结果。
     */
    @Test
    @DisplayName("get system channel existing member returns result")
    void getSystemChannel_existingMember_returnsResult() {
        TestContext context = new TestContext();
        context.channelRepository.channels.put(2L, systemChannel());
        context.channelMemberRepository.save(new ChannelMember(2L, 1001L, ChannelMemberRole.MEMBER, BASE_TIME, null));
        ChannelApplicationService service = context.createService();

        ChannelResult result = service.getSystemChannel(new GetSystemChannelCommand(1001L));

        assertEquals(2L, result.channelId());
        assertEquals("system", result.type());
    }

    /**
     * 验证 system 频道成员列表对普通成员不可见。
     */
    @Test
    @DisplayName("list channel members system channel throws forbidden problem")
    void listChannelMembers_systemChannel_throwsForbiddenProblem() {
        TestContext context = new TestContext();
        context.channelRepository.channels.put(2L, systemChannel());
        context.channelMemberRepository.save(new ChannelMember(2L, 1001L, ChannelMemberRole.MEMBER, BASE_TIME, null));
        ChannelApplicationService service = context.createService();

        ProblemException exception = assertThrows(
                ProblemException.class,
                () -> service.listChannelMembers(new ListChannelMembersQuery(1001L, 2L))
        );

        assertEquals("system channel member list is not available", exception.getMessage());
    }

    /**
     * 验证创建 private channel 时会写入频道记录，并把创建者登记为 OWNER。
     */
    @Test
    @DisplayName("create private channel valid command saves private channel and owner membership")
    void createPrivateChannel_validCommand_savesPrivateChannelAndOwnerMembership() {
        TestContext context = new TestContext();
        ChannelApplicationService service = context.createService();

        ChannelResult result = service.createPrivateChannel(new CreatePrivateChannelCommand(1001L, "engineering"));

        assertEquals(2001L, result.channelId());
        assertEquals(2001L, result.conversationId());
        assertEquals("private", result.type());
        assertEquals("", result.brief());
        assertEquals(false, result.defaultChannel());
        assertNotNull(context.channelRepository.savedChannel);
        ChannelMember ownerMember = context.channelMemberRepository.findByChannelIdAndAccountId(result.channelId(), 1001L).orElseThrow();
        assertEquals(ChannelMemberRole.OWNER, ownerMember.role());
        assertTrue(context.channelRealtimePublisher.channelsChangedAccountIds.contains(1001L));
    }

    /**
     * 验证事务失败时，已登记的频道变更广播不会提前发出。
     */
    @Test
    @DisplayName("create private channel rollback skips realtime publish")
    void createPrivateChannel_rollback_skipsRealtimePublish() {
        TestContext context = new TestContext();
        ChannelApplicationService service = context.createService(new RollbackingTransactionRunner());

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.createPrivateChannel(new CreatePrivateChannelCommand(1001L, "engineering"))
        );

        assertEquals("transaction rolled back", exception.getMessage());
        assertEquals(0, context.channelRealtimePublisher.channelsChangedAccountIds.size());
    }

    @Test
    @DisplayName("update channel profile owner updates channel name and brief")
    void updateChannelProfile_owner_updatesChannelNameAndBrief() {
        TestContext context = new TestContext();
        context.channelRepository.channels.put(9L, privateChannel(9L, "project-alpha"));
        context.channelMemberRepository.save(new ChannelMember(9L, 1001L, ChannelMemberRole.OWNER, BASE_TIME, null));
        ChannelApplicationService service = context.createService();

        ChannelResult result = service.updateChannelProfile(new UpdateChannelProfileCommand(1001L, 9L, "project-beta", "新的简介"));

        assertEquals("project-beta", result.name());
        assertEquals("新的简介", result.brief());
        assertEquals("project-beta", context.channelRepository.channels.get(9L).name());
        assertEquals("新的简介", context.channelRepository.channels.get(9L).brief());
        assertTrue(context.channelRealtimePublisher.channelChangedScopes.contains("profile"));
    }

    @Test
    @DisplayName("update channel read state advances anchor and returns result")
    void updateChannelReadState_advancesAnchorAndReturnsResult() {
        TestContext context = new TestContext();
        context.channelRepository.channels.put(9L, privateChannel(9L, "project-alpha"));
        context.channelMemberRepository.save(new ChannelMember(9L, 1001L, ChannelMemberRole.MEMBER, BASE_TIME, null));
        context.messageRepository.save(new ChannelMessage(5001L, "550e8400-e29b-41d4-a716-446655440000", 9L, 9L, 1002L, "text", "hello", "hello", "hello", null, null, "sent", BASE_TIME));
        ChannelApplicationService service = context.createService();

        var result = service.updateChannelReadState(new team.carrypigeon.backend.chat.domain.features.channel.application.command.UpdateChannelReadStateCommand(1001L, 9L, 5001L, BASE_TIME.plusSeconds(5).toEpochMilli()));

        assertEquals("9", result.cid());
        assertEquals("5001", result.lastReadMid());
        assertEquals(1, context.channelRealtimePublisher.readStateUpdates.size());
        assertEquals(5001L, context.channelRealtimePublisher.readStateUpdates.getFirst().lastReadMessageId());
    }

    @Test
    @DisplayName("list unreads returns unread projections")
    void listUnreads_returnsUnreadProjections() {
        TestContext context = new TestContext();
        context.channelReadStateRepository.unreadResults = List.of(new ChannelUnread(9L, 3L, BASE_TIME));
        ChannelApplicationService service = context.createService();

        List<team.carrypigeon.backend.chat.domain.features.channel.application.dto.ChannelUnreadResult> result = service.listUnreads(1001L);

        assertEquals(1, result.size());
        assertEquals("9", result.getFirst().cid());
        assertEquals(3L, result.getFirst().unreadCount());
    }

    @Test
    @DisplayName("list channel bans owner returns ban items")
    void listChannelBans_owner_returnsBanItems() {
        TestContext context = new TestContext();
        context.channelRepository.channels.put(9L, privateChannel(9L, "project-alpha"));
        context.channelMemberRepository.save(new ChannelMember(9L, 1001L, ChannelMemberRole.OWNER, BASE_TIME, null));
        context.channelBanRepository.channelBan = new ChannelBan(9L, 1002L, 1001L, "spam", BASE_TIME.plusSeconds(300), BASE_TIME, null);
        ChannelApplicationService service = context.createService();

        List<ChannelBanListItemResult> result = service.listChannelBans(new ListChannelBansQuery(1001L, 9L));

        assertEquals(1, result.size());
        assertEquals(1002L, result.getFirst().bannedAccountId());
        assertEquals("spam", result.getFirst().reason());
    }

    /**
     * 验证 OWNER 可以删除频道。
     */
    @Test
    @DisplayName("delete channel owner removes channel")
    void deleteChannel_owner_removesChannel() {
        TestContext context = new TestContext();
        context.channelRepository.channels.put(9L, privateChannel(9L, "project-alpha"));
        context.channelMemberRepository.save(new ChannelMember(9L, 1001L, ChannelMemberRole.OWNER, BASE_TIME, null));
        ChannelApplicationService service = context.createService();

        service.deleteChannel(new DeleteChannelCommand(1001L, 9L));

        assertEquals(false, context.channelRepository.channels.containsKey(9L));
    }

    /**
     * 验证 OWNER 可以向 private channel 发起成员邀请。
     */
    @Test
    @DisplayName("invite channel member owner invite creates pending invite")
    void inviteChannelMember_ownerInvite_createsPendingInvite() {
        TestContext context = new TestContext();
        context.channelRepository.channels.put(9L, privateChannel(9L, "project-alpha"));
        context.channelMemberRepository.save(new ChannelMember(9L, 1001L, ChannelMemberRole.OWNER, BASE_TIME, null));
        context.userProfileRepository.save(profile(1002L, "carry-guest"));
        ChannelApplicationService service = context.createService();

        ChannelInviteResult result = service.inviteChannelMember(new InviteChannelMemberCommand(1001L, 9L, 1002L));

        assertEquals(9L, result.channelId());
        assertEquals(1002L, result.inviteeAccountId());
        assertEquals("PENDING", result.status());
        assertEquals("PENDING", context.channelInviteRepository.savedInvite.status().name());
        assertTrue(context.channelRealtimePublisher.channelChangedScopes.contains("applications"));
        assertTrue(context.channelRealtimePublisher.channelsChangedAccountIds.contains(1002L));
    }

    @Test
    @DisplayName("create channel application non member creates pending application")
    void createChannelApplication_nonMember_createsPendingApplication() {
        TestContext context = new TestContext();
        context.channelRepository.channels.put(9L, privateChannel(9L, "project-alpha"));
        ChannelApplicationService service = context.createService();

        ChannelApplicationResult result = service.createChannelApplication(new CreateChannelApplicationCommand(1002L, 9L, "hi"));

        assertEquals(9L, result.channelId());
        assertEquals(1002L, result.accountId());
        assertEquals("PENDING", result.status());
        assertEquals(1002L, context.channelInviteRepository.savedInvite.inviteeAccountId());
        assertTrue(context.channelRealtimePublisher.channelChangedScopes.contains("applications"));
        assertTrue(context.channelRealtimePublisher.channelsChangedAccountIds.contains(1002L));
    }

    @Test
    @DisplayName("list channel applications owner returns pending items")
    void listChannelApplications_owner_returnsPendingItems() {
        TestContext context = new TestContext();
        context.channelRepository.channels.put(9L, privateChannel(9L, "project-alpha"));
        context.channelMemberRepository.save(new ChannelMember(9L, 1001L, ChannelMemberRole.OWNER, BASE_TIME, null));
        context.channelInviteRepository.savedInvite = new ChannelInvite(
                9L,
                3001L,
                1002L,
                0L,
                ChannelInviteStatus.PENDING,
                BASE_TIME,
                null
        );
        ChannelApplicationService service = context.createService();

        List<ChannelApplicationResult> result = service.listChannelApplications(new ListChannelApplicationsQuery(1001L, 9L));

        assertEquals(1, result.size());
        assertEquals(3001L, result.getFirst().applicationId());
        assertEquals("PENDING", result.getFirst().status());
    }

    @Test
    @DisplayName("list channel applications ignores direct invite records")
    void listChannelApplications_directInvite_returnsEmptyList() {
        TestContext context = new TestContext();
        context.channelRepository.channels.put(9L, privateChannel(9L, "project-alpha"));
        context.channelMemberRepository.save(new ChannelMember(9L, 1001L, ChannelMemberRole.OWNER, BASE_TIME, null));
        context.channelInviteRepository.savedInvite = new ChannelInvite(
                9L,
                3001L,
                1002L,
                1001L,
                ChannelInviteStatus.PENDING,
                BASE_TIME,
                null
        );
        ChannelApplicationService service = context.createService();

        List<ChannelApplicationResult> result = service.listChannelApplications(new ListChannelApplicationsQuery(1001L, 9L));

        assertEquals(0, result.size());
    }

    @Test
    @DisplayName("decide channel application approve creates membership and updates status")
    void decideChannelApplication_approve_createsMembershipAndUpdatesStatus() {
        TestContext context = new TestContext();
        context.channelRepository.channels.put(9L, privateChannel(9L, "project-alpha"));
        context.channelMemberRepository.save(new ChannelMember(9L, 1001L, ChannelMemberRole.OWNER, BASE_TIME, null));
        context.channelInviteRepository.savedInvite = new ChannelInvite(
                9L,
                3001L,
                1002L,
                0L,
                ChannelInviteStatus.PENDING,
                BASE_TIME,
                null
        );
        ChannelApplicationService service = context.createService();

        ChannelApplicationResult result = service.decideChannelApplication(new DecideChannelApplicationCommand(1001L, 9L, 3001L, "approve"));

        assertEquals("ACCEPTED", result.status());
        assertEquals(true, context.channelMemberRepository.exists(9L, 1002L));
        assertEquals(ChannelInviteStatus.ACCEPTED, context.channelInviteRepository.updatedInvite.status());
        assertTrue(context.channelRealtimePublisher.channelChangedScopes.contains("applications"));
        assertTrue(context.channelRealtimePublisher.channelChangedScopes.contains("members"));
        assertTrue(context.channelRealtimePublisher.channelsChangedAccountIds.contains(1002L));
    }

    @Test
    @DisplayName("decide channel application direct invite throws not found problem")
    void decideChannelApplication_directInvite_throwsNotFoundProblem() {
        TestContext context = new TestContext();
        context.channelRepository.channels.put(9L, privateChannel(9L, "project-alpha"));
        context.channelMemberRepository.save(new ChannelMember(9L, 1001L, ChannelMemberRole.OWNER, BASE_TIME, null));
        context.channelInviteRepository.savedInvite = new ChannelInvite(
                9L,
                3001L,
                1002L,
                1001L,
                ChannelInviteStatus.PENDING,
                BASE_TIME,
                null
        );
        ChannelApplicationService service = context.createService();

        ProblemException exception = assertThrows(
                ProblemException.class,
                () -> service.decideChannelApplication(new DecideChannelApplicationCommand(1001L, 9L, 3001L, "approve"))
        );

        assertEquals("channel application does not exist", exception.getMessage());
    }

    /**
     * 验证普通 MEMBER 无法发起 private channel 邀请。
     */
    @Test
    @DisplayName("invite channel member member role throws forbidden problem")
    void inviteChannelMember_memberRole_throwsForbiddenProblem() {
        TestContext context = new TestContext();
        context.channelRepository.channels.put(9L, privateChannel(9L, "project-alpha"));
        context.channelMemberRepository.save(new ChannelMember(9L, 1001L, ChannelMemberRole.MEMBER, BASE_TIME, null));
        context.userProfileRepository.save(profile(1002L, "carry-guest"));
        ChannelApplicationService service = context.createService();

        ProblemException exception = assertThrows(
                ProblemException.class,
                () -> service.inviteChannelMember(new InviteChannelMemberCommand(1001L, 9L, 1002L))
        );

        assertEquals("channel invite requires owner or admin role", exception.getMessage());
    }

    /**
     * 验证接受邀请时会新增 MEMBER 活跃成员，并把邀请状态更新为 ACCEPTED。
     */
    @Test
    @DisplayName("accept channel invite pending invite creates member and marks invite accepted")
    void acceptChannelInvite_pendingInvite_createsMemberAndMarksInviteAccepted() {
        TestContext context = new TestContext();
        context.channelRepository.channels.put(9L, privateChannel(9L, "project-alpha"));
        context.channelInviteRepository.savedInvite = new ChannelInvite(
                9L,
                3001L,
                1002L,
                1001L,
                ChannelInviteStatus.PENDING,
                BASE_TIME,
                null
        );
        ChannelApplicationService service = context.createService();

        ChannelInviteResult result = service.acceptChannelInvite(new AcceptChannelInviteCommand(1002L, 9L));

        assertEquals("ACCEPTED", result.status());
        ChannelMember acceptedMember = context.channelMemberRepository.findByChannelIdAndAccountId(9L, 1002L).orElseThrow();
        assertEquals(ChannelMemberRole.MEMBER, acceptedMember.role());
        assertEquals(ChannelInviteStatus.ACCEPTED, context.channelInviteRepository.updatedInvite.status());
        assertTrue(context.channelRealtimePublisher.channelChangedScopes.contains("applications"));
        assertTrue(context.channelRealtimePublisher.channelChangedScopes.contains("members"));
        assertTrue(context.channelRealtimePublisher.channelsChangedAccountIds.contains(1002L));
    }

    @Test
    @DisplayName("accept channel invite application record throws not found problem")
    void acceptChannelInvite_applicationRecord_throwsNotFoundProblem() {
        TestContext context = new TestContext();
        context.channelRepository.channels.put(9L, privateChannel(9L, "project-alpha"));
        context.channelInviteRepository.savedInvite = new ChannelInvite(
                9L,
                3001L,
                1002L,
                0L,
                ChannelInviteStatus.PENDING,
                BASE_TIME,
                null
        );
        ChannelApplicationService service = context.createService();

        ProblemException exception = assertThrows(
                ProblemException.class,
                () -> service.acceptChannelInvite(new AcceptChannelInviteCommand(1002L, 9L))
        );

        assertEquals("channel invite does not exist", exception.getMessage());
    }

    @Test
    @DisplayName("create channel application rollback skips realtime publish")
    void createChannelApplication_rollback_skipsRealtimePublish() {
        TestContext context = new TestContext();
        context.channelRepository.channels.put(9L, privateChannel(9L, "project-alpha"));
        ChannelApplicationService service = context.createService(new RollbackingTransactionRunner());

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.createChannelApplication(new CreateChannelApplicationCommand(1002L, 9L, "hi"))
        );

        assertEquals("transaction rolled back", exception.getMessage());
        assertEquals(0, context.channelRealtimePublisher.channelChangedScopes.size());
        assertEquals(0, context.channelRealtimePublisher.channelsChangedAccountIds.size());
    }

    @Test
    @DisplayName("create channel application existing invite rewrites record as application")
    void createChannelApplication_existingInvite_rewritesRecordAsApplication() {
        TestContext context = new TestContext();
        context.channelRepository.channels.put(9L, privateChannel(9L, "project-alpha"));
        context.channelInviteRepository.savedInvite = new ChannelInvite(
                9L,
                3001L,
                1002L,
                1001L,
                ChannelInviteStatus.DECLINED,
                BASE_TIME.minusSeconds(300),
                BASE_TIME.minusSeconds(60)
        );
        ChannelApplicationService service = context.createService();

        ChannelApplicationResult result = service.createChannelApplication(new CreateChannelApplicationCommand(1002L, 9L, "retry"));

        assertEquals(3001L, result.applicationId());
        assertEquals(0L, context.channelInviteRepository.updatedInvite.inviterAccountId());
        assertEquals(ChannelInviteStatus.PENDING, context.channelInviteRepository.updatedInvite.status());
    }

    /**
     * 验证活跃成员可以读取频道成员列表，并得到成员角色与资料字段。
     */
    @Test
    @DisplayName("list channel members active member returns member results")
    void listChannelMembers_activeMember_returnsMemberResults() {
        TestContext context = new TestContext();
        context.channelRepository.channels.put(9L, privateChannel(9L, "project-alpha"));
        context.channelMemberRepository.save(new ChannelMember(9L, 1001L, ChannelMemberRole.OWNER, BASE_TIME, null));
        context.channelMemberRepository.save(new ChannelMember(9L, 1002L, ChannelMemberRole.MEMBER, BASE_TIME.plusSeconds(60), null));
        context.userProfileRepository.save(profile(1001L, "carry-owner"));
        context.userProfileRepository.save(profile(1002L, "carry-member"));
        ChannelApplicationService service = context.createService();

        List<ChannelMemberResult> result = service.listChannelMembers(new ListChannelMembersQuery(1001L, 9L));

        assertEquals(2, result.size());
        assertEquals(1001L, result.get(0).accountId());
        assertEquals("carry-owner", result.get(0).nickname());
        assertEquals("OWNER", result.get(0).role());
        assertEquals(1002L, result.get(1).accountId());
        assertEquals("MEMBER", result.get(1).role());
    }

    /**
     * 验证默认频道缺失时会返回不存在问题语义。
     */
    @Test
    @DisplayName("get default channel missing channel throws not found problem")
    void getDefaultChannel_missingChannel_throwsNotFoundProblem() {
        TestContext context = new TestContext();
        context.channelRepository.defaultChannel = null;
        ChannelApplicationService service = context.createService();

        ProblemException exception = assertThrows(
                ProblemException.class,
                () -> service.getDefaultChannel(new GetDefaultChannelCommand(1001L))
        );

        assertEquals("default channel does not exist", exception.getMessage());
    }

    /**
     * 验证 OWNER 可以把 MEMBER 提升为 ADMIN，并写入审计日志。
     */
    @Test
    @DisplayName("promote channel member owner promotes member to admin")
    void promoteChannelMember_owner_promotesMemberToAdmin() {
        TestContext context = new TestContext();
        context.channelRepository.channels.put(9L, privateChannel(9L, "project-alpha"));
        context.channelMemberRepository.save(new ChannelMember(9L, 1001L, ChannelMemberRole.OWNER, BASE_TIME, null));
        context.channelMemberRepository.save(new ChannelMember(9L, 1002L, ChannelMemberRole.MEMBER, BASE_TIME.plusSeconds(60), null));
        context.userProfileRepository.save(profile(1002L, "carry-admin"));
        ChannelApplicationService service = context.createService();

        ChannelMemberResult result = service.promoteChannelMember(new PromoteChannelMemberCommand(1001L, 9L, 1002L));

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
        TestContext context = new TestContext();
        context.channelRepository.channels.put(9L, privateChannel(9L, "project-alpha"));
        context.channelMemberRepository.save(new ChannelMember(9L, 1001L, ChannelMemberRole.OWNER, BASE_TIME, null));
        context.channelMemberRepository.save(new ChannelMember(9L, 1002L, ChannelMemberRole.ADMIN, BASE_TIME.plusSeconds(60), null));
        context.userProfileRepository.save(profile(1002L, "carry-member"));
        ChannelApplicationService service = context.createService();

        ChannelMemberResult result = service.demoteChannelAdmin(new DemoteChannelAdminCommand(1001L, 9L, 1002L));

        assertEquals("MEMBER", result.role());
        assertEquals(ChannelMemberRole.MEMBER, context.channelMemberRepository.findByChannelIdAndAccountId(9L, 1002L).orElseThrow().role());
    }

    /**
     * 验证 OWNER 转移所有权后自己会退回 ADMIN，目标成为 OWNER。
     */
    @Test
    @DisplayName("transfer channel ownership owner transfers and keeps previous owner as admin")
    void transferChannelOwnership_owner_transfersAndKeepsPreviousOwnerAsAdmin() {
        TestContext context = new TestContext();
        context.channelRepository.channels.put(9L, privateChannel(9L, "project-alpha"));
        context.channelMemberRepository.save(new ChannelMember(9L, 1001L, ChannelMemberRole.OWNER, BASE_TIME, null));
        context.channelMemberRepository.save(new ChannelMember(9L, 1002L, ChannelMemberRole.ADMIN, BASE_TIME.plusSeconds(60), null));
        ChannelApplicationService service = context.createService();

        ChannelOwnershipTransferResult result = service.transferChannelOwnership(new TransferChannelOwnershipCommand(1001L, 9L, 1002L));

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
        TestContext context = new TestContext();
        context.channelRepository.channels.put(9L, privateChannel(9L, "project-alpha"));
        context.channelMemberRepository.save(new ChannelMember(9L, 1001L, ChannelMemberRole.OWNER, BASE_TIME, null));
        context.channelMemberRepository.save(new ChannelMember(9L, 1002L, ChannelMemberRole.MEMBER, BASE_TIME.plusSeconds(60), null));
        context.userProfileRepository.save(profile(1002L, "carry-member"));
        ChannelApplicationService service = context.createService();

        ChannelMemberResult result = service.muteChannelMember(new MuteChannelMemberCommand(1001L, 9L, 1002L, 300));

        assertEquals(BASE_TIME.plusSeconds(300), result.mutedUntil());
        assertEquals(BASE_TIME.plusSeconds(300), context.channelMemberRepository.findByChannelIdAndAccountId(9L, 1002L).orElseThrow().mutedUntil());
    }

    /**
     * 验证解除禁言会把 mutedUntil 清空。
     */
    @Test
    @DisplayName("unmute channel member owner clears muted until")
    void unmuteChannelMember_owner_clearsMutedUntil() {
        TestContext context = new TestContext();
        context.channelRepository.channels.put(9L, privateChannel(9L, "project-alpha"));
        context.channelMemberRepository.save(new ChannelMember(9L, 1001L, ChannelMemberRole.OWNER, BASE_TIME, null));
        context.channelMemberRepository.save(new ChannelMember(9L, 1002L, ChannelMemberRole.MEMBER, BASE_TIME.plusSeconds(60), BASE_TIME.plusSeconds(300)));
        context.userProfileRepository.save(profile(1002L, "carry-member"));
        ChannelApplicationService service = context.createService();

        ChannelMemberResult result = service.unmuteChannelMember(new UnmuteChannelMemberCommand(1001L, 9L, 1002L));

        assertEquals(null, result.mutedUntil());
        assertEquals(null, context.channelMemberRepository.findByChannelIdAndAccountId(9L, 1002L).orElseThrow().mutedUntil());
    }

    /**
     * 验证踢人会删除活跃成员投影。
     */
    @Test
    @DisplayName("kick channel member owner removes active membership")
    void kickChannelMember_owner_removesActiveMembership() {
        TestContext context = new TestContext();
        context.channelRepository.channels.put(9L, privateChannel(9L, "project-alpha"));
        context.channelMemberRepository.save(new ChannelMember(9L, 1001L, ChannelMemberRole.OWNER, BASE_TIME, null));
        context.channelMemberRepository.save(new ChannelMember(9L, 1002L, ChannelMemberRole.MEMBER, BASE_TIME.plusSeconds(60), null));
        ChannelApplicationService service = context.createService();

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
        TestContext context = new TestContext();
        context.channelRepository.channels.put(9L, privateChannel(9L, "project-alpha"));
        context.channelMemberRepository.save(new ChannelMember(9L, 1001L, ChannelMemberRole.OWNER, BASE_TIME, null));
        context.channelMemberRepository.save(new ChannelMember(9L, 1002L, ChannelMemberRole.MEMBER, BASE_TIME.plusSeconds(60), null));
        ChannelApplicationService service = context.createService();

        ChannelBanResult result = service.banChannelMember(new BanChannelMemberCommand(1001L, 9L, 1002L, "spam", 600L));

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
        TestContext context = new TestContext();
        context.channelRepository.channels.put(9L, privateChannel(9L, "project-alpha"));
        context.channelMemberRepository.save(new ChannelMember(9L, 1001L, ChannelMemberRole.OWNER, BASE_TIME, null));
        context.channelBanRepository.channelBan = new ChannelBan(9L, 1002L, 1001L, "spam", null, BASE_TIME, null);
        ChannelApplicationService service = context.createService();

        ChannelBanResult result = service.unbanChannelMember(new UnbanChannelMemberCommand(1001L, 9L, 1002L));

        assertEquals(BASE_TIME, result.revokedAt());
        assertEquals(BASE_TIME, context.channelBanRepository.channelBan.revokedAt());
    }

    /**
     * 验证 ADMIN 不能直接禁言 OWNER。
     */
    @Test
    @DisplayName("mute channel member admin target owner throws forbidden problem")
    void muteChannelMember_adminTargetOwner_throwsForbiddenProblem() {
        TestContext context = new TestContext();
        context.channelRepository.channels.put(9L, privateChannel(9L, "project-alpha"));
        context.channelMemberRepository.save(new ChannelMember(9L, 1001L, ChannelMemberRole.ADMIN, BASE_TIME, null));
        context.channelMemberRepository.save(new ChannelMember(9L, 1002L, ChannelMemberRole.OWNER, BASE_TIME.plusSeconds(60), null));
        ChannelApplicationService service = context.createService();

        ProblemException exception = assertThrows(
                ProblemException.class,
                () -> service.muteChannelMember(new MuteChannelMemberCommand(1001L, 9L, 1002L, 300))
        );

        assertEquals("target member with OWNER role cannot be moderated", exception.getMessage());
    }

    private static Channel privateChannel(long id, String name) {
        return new Channel(id, id, name, "", "", "", "private", false, BASE_TIME, BASE_TIME);
    }

    private static Channel publicChannel() {
        return new Channel(1L, 1L, "public", "", "", "", "public", true, BASE_TIME, BASE_TIME);
    }

    private static Channel systemChannel() {
        return new Channel(2L, 2L, "system", "", "", "", "system", false, BASE_TIME, BASE_TIME);
    }

    private static UserProfile profile(long accountId, String nickname) {
        return new UserProfile(accountId, nickname, "", "", BASE_TIME, BASE_TIME);
    }

    private static final class TestContext {

        private final InMemoryChannelRepository channelRepository = new InMemoryChannelRepository();
        private final InMemoryChannelMemberRepository channelMemberRepository = new InMemoryChannelMemberRepository();
        private final InMemoryChannelInviteRepository channelInviteRepository = new InMemoryChannelInviteRepository();
        private final InMemoryChannelBanRepository channelBanRepository = new InMemoryChannelBanRepository();
        private final InMemoryChannelAuditLogRepository channelAuditLogRepository = new InMemoryChannelAuditLogRepository();
        private final InMemoryChannelReadStateRepository channelReadStateRepository = new InMemoryChannelReadStateRepository();
        private final InMemoryMessageRepository messageRepository = new InMemoryMessageRepository();
        private final InMemoryUserProfileRepository userProfileRepository = new InMemoryUserProfileRepository();
        private final RecordingChannelRealtimePublisher channelRealtimePublisher = new RecordingChannelRealtimePublisher();

        private TestContext() {
            channelRepository.defaultChannel = publicChannel();
            channelRepository.channels.put(1L, channelRepository.defaultChannel);
            channelMemberRepository.save(new ChannelMember(1L, 1001L, ChannelMemberRole.OWNER, BASE_TIME, null));
        }

        private ChannelApplicationService createService() {
            return createService(new NoopTransactionRunner());
        }

        private ChannelApplicationService createService(TransactionRunner transactionRunner) {
            return new ChannelApplicationService(
                    channelRepository,
                    channelMemberRepository,
                    channelInviteRepository,
                    channelBanRepository,
                    channelAuditLogRepository,
                    channelReadStateRepository,
                    messageRepository,
                    userProfileRepository,
                    new ChannelGovernancePolicy(),
                    channelRealtimePublisher,
                    new FixedIdGenerator(),
                    new TimeProvider(Clock.fixed(BASE_TIME, ZoneOffset.UTC)),
                    transactionRunner
            );
        }
    }

    private static final class InMemoryChannelReadStateRepository implements ChannelReadStateRepository {

        private final Map<String, ChannelReadState> states = new HashMap<>();
        private List<ChannelUnread> unreadResults = List.of();

        @Override
        public Optional<ChannelReadState> findByChannelIdAndAccountId(long channelId, long accountId) {
            return Optional.ofNullable(states.get(key(channelId, accountId)));
        }

        @Override
        public ChannelReadState upsert(ChannelReadState readState) {
            states.put(key(readState.channelId(), readState.accountId()), readState);
            return readState;
        }

        @Override
        public List<ChannelUnread> listUnreadsByAccountId(long accountId) {
            return unreadResults;
        }

        private String key(long channelId, long accountId) {
            return channelId + ":" + accountId;
        }
    }

    private static final class InMemoryMessageRepository implements MessageRepository {

        private final Map<Long, ChannelMessage> messagesById = new HashMap<>();

        @Override
        public ChannelMessage save(ChannelMessage message) {
            messagesById.put(message.messageId(), message);
            return message;
        }

        @Override
        public Optional<ChannelMessage> findById(long messageId) {
            return Optional.ofNullable(messagesById.get(messageId));
        }

        @Override
        public ChannelMessage update(ChannelMessage message) {
            messagesById.put(message.messageId(), message);
            return message;
        }

        @Override
        public List<ChannelMessage> findByChannelIdBefore(long channelId, Long cursorMessageId, int limit) {
            return List.of();
        }

        @Override
        public List<ChannelMessage> searchByChannelId(long channelId, String keyword, int limit) {
            return List.of();
        }
    }

    private static final class RecordingChannelRealtimePublisher implements ChannelRealtimePublisher {
        private final List<ChannelReadState> readStateUpdates = new ArrayList<>();
        private final List<String> channelChangedScopes = new ArrayList<>();
        private final List<Long> channelsChangedAccountIds = new ArrayList<>();

        @Override
        public void publishReadStateUpdated(ChannelReadState readState) {
            readStateUpdates.add(readState);
        }

        @Override
        public void publishChannelChanged(Channel channel, String scope, java.util.Collection<Long> recipientAccountIds) {
            channelChangedScopes.add(scope);
        }

        @Override
        public void publishChannelsChanged(long accountId) {
            channelsChangedAccountIds.add(accountId);
        }
    }

    private static final class InMemoryChannelRepository implements ChannelRepository {

        private final Map<Long, Channel> channels = new HashMap<>();
        private Channel defaultChannel;
        private Channel savedChannel;

        @Override
        public Optional<Channel> findDefaultChannel() {
            return Optional.ofNullable(defaultChannel);
        }

        @Override
        public Optional<Channel> findSystemChannel() {
            return channels.values().stream().filter(channel -> "system".equals(channel.type())).findFirst();
        }

        @Override
        public Optional<Channel> findById(long channelId) {
            return Optional.ofNullable(channels.get(channelId));
        }

        @Override
        public Channel save(Channel channel) {
            this.savedChannel = channel;
            this.channels.put(channel.id(), channel);
            return channel;
        }

        @Override
        public Channel update(Channel channel) {
            this.channels.put(channel.id(), channel);
            return channel;
        }

        @Override
        public void delete(long channelId) {
            this.channels.remove(channelId);
        }
    }

    private static final class InMemoryChannelMemberRepository implements ChannelMemberRepository {

        private final Map<Long, List<ChannelMember>> membersByChannelId = new HashMap<>();

        @Override
        public boolean exists(long channelId, long accountId) {
            return membersByChannelId.getOrDefault(channelId, List.of()).stream()
                    .anyMatch(member -> member.accountId() == accountId);
        }

        @Override
        public void save(ChannelMember channelMember) {
            membersByChannelId.computeIfAbsent(channelMember.channelId(), ignored -> new ArrayList<>()).add(channelMember);
        }

        @Override
        public void update(ChannelMember channelMember) {
            List<ChannelMember> members = new ArrayList<>(membersByChannelId.getOrDefault(channelMember.channelId(), List.of()));
            members.removeIf(existing -> existing.accountId() == channelMember.accountId());
            members.add(channelMember);
            membersByChannelId.put(channelMember.channelId(), members);
        }

        @Override
        public void delete(long channelId, long accountId) {
            List<ChannelMember> members = new ArrayList<>(membersByChannelId.getOrDefault(channelId, List.of()));
            members.removeIf(member -> member.accountId() == accountId);
            membersByChannelId.put(channelId, members);
        }

        @Override
        public Optional<ChannelMember> findByChannelIdAndAccountId(long channelId, long accountId) {
            return membersByChannelId.getOrDefault(channelId, List.of()).stream()
                    .filter(member -> member.accountId() == accountId)
                    .findFirst();
        }

        @Override
        public List<ChannelMember> findByChannelId(long channelId) {
            return membersByChannelId.getOrDefault(channelId, List.of()).stream().toList();
        }

        @Override
        public List<Long> findAccountIdsByChannelId(long channelId) {
            return membersByChannelId.getOrDefault(channelId, List.of()).stream()
                    .map(ChannelMember::accountId)
                    .toList();
        }
    }

    private static final class InMemoryChannelInviteRepository implements ChannelInviteRepository {

        private ChannelInvite savedInvite;
        private ChannelInvite updatedInvite;

        @Override
        public Optional<ChannelInvite> findByChannelIdAndInviteeAccountId(long channelId, long inviteeAccountId) {
            ChannelInvite invite = updatedInvite != null ? updatedInvite : savedInvite;
            if (invite == null || invite.channelId() != channelId || invite.inviteeAccountId() != inviteeAccountId) {
                return Optional.empty();
            }
            return Optional.of(invite);
        }

        @Override
        public Optional<ChannelInvite> findByChannelIdAndApplicationId(long channelId, long applicationId) {
            ChannelInvite invite = updatedInvite != null ? updatedInvite : savedInvite;
            if (invite == null || invite.channelId() != channelId || invite.applicationId() != applicationId) {
                return Optional.empty();
            }
            return Optional.of(invite);
        }

        @Override
        public List<ChannelInvite> findByChannelId(long channelId) {
            ChannelInvite invite = updatedInvite != null ? updatedInvite : savedInvite;
            if (invite == null || invite.channelId() != channelId) {
                return List.of();
            }
            return List.of(invite);
        }

        @Override
        public void save(ChannelInvite channelInvite) {
            this.savedInvite = channelInvite;
        }

        @Override
        public void update(ChannelInvite channelInvite) {
            this.updatedInvite = channelInvite;
            this.savedInvite = channelInvite;
        }
    }

    private static final class InMemoryChannelBanRepository implements ChannelBanRepository {

        private ChannelBan channelBan;

        @Override
        public Optional<ChannelBan> findByChannelIdAndBannedAccountId(long channelId, long bannedAccountId) {
            if (channelBan == null || channelBan.channelId() != channelId || channelBan.bannedAccountId() != bannedAccountId) {
                return Optional.empty();
            }
            return Optional.of(channelBan);
        }

        @Override
        public List<ChannelBan> findByChannelId(long channelId) {
            if (channelBan == null || channelBan.channelId() != channelId) {
                return List.of();
            }
            return List.of(channelBan);
        }

        @Override
        public void save(ChannelBan channelBan) {
            this.channelBan = channelBan;
        }

        @Override
        public void update(ChannelBan channelBan) {
            this.channelBan = channelBan;
        }
    }

    private static final class InMemoryUserProfileRepository implements UserProfileRepository {

        private final Map<Long, UserProfile> profiles = new HashMap<>();

        @Override
        public Optional<UserProfile> findByAccountId(long accountId) {
            return Optional.ofNullable(profiles.get(accountId));
        }

        @Override
        public List<UserProfile> findAll() {
            return new ArrayList<>(profiles.values());
        }

        @Override
        public List<UserProfile> findByAccountIdBefore(Long cursorAccountId, int limit) {
            return profiles.values().stream()
                    .filter(profile -> cursorAccountId == null || profile.accountId() < cursorAccountId)
                    .sorted(java.util.Comparator.comparingLong(UserProfile::accountId).reversed())
                    .limit(limit)
                    .toList();
        }

        @Override
        public List<UserProfile> searchByKeyword(String keyword, Long cursorAccountId, int limit) {
            String normalizedKeyword = keyword == null ? "" : keyword.trim();
            return profiles.values().stream()
                    .filter(profile -> cursorAccountId == null || profile.accountId() < cursorAccountId)
                    .filter(profile -> profile.nickname().contains(normalizedKeyword) || profile.bio().contains(normalizedKeyword))
                    .sorted(java.util.Comparator.comparingLong(UserProfile::accountId).reversed())
                    .limit(limit)
                    .toList();
        }

        @Override
        public UserProfile save(UserProfile userProfile) {
            profiles.put(userProfile.accountId(), userProfile);
            return userProfile;
        }

        @Override
        public UserProfile update(UserProfile userProfile) {
            profiles.put(userProfile.accountId(), userProfile);
            return userProfile;
        }
    }

    private static final class InMemoryChannelAuditLogRepository implements ChannelAuditLogRepository {

        private final List<ChannelAuditLog> logs = new ArrayList<>();

        @Override
        public void append(ChannelAuditLog channelAuditLog) {
            logs.add(channelAuditLog);
        }
    }

    private static final class FixedIdGenerator implements IdGenerator {

        @Override
        public long nextLongId() {
            return 2001L;
        }
    }

    private static final class NoopTransactionRunner implements TransactionRunner {

        @Override
        public <T> T runInTransaction(java.util.function.Supplier<T> action) {
            return action.get();
        }

        @Override
        public void runInTransaction(Runnable action) {
            action.run();
        }
    }

    private static final class RollbackingTransactionRunner implements TransactionRunner {

        @Override
        public <T> T runInTransaction(java.util.function.Supplier<T> action) {
            action.get();
            throw new IllegalStateException("transaction rolled back");
        }

        @Override
        public void runInTransaction(Runnable action) {
            action.run();
            throw new IllegalStateException("transaction rolled back");
        }
    }
}
