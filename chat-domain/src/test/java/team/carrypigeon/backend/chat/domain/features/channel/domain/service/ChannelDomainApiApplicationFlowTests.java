package team.carrypigeon.backend.chat.domain.features.channel.domain.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.AcceptChannelInviteCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.CreateChannelApplicationCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.DecideChannelApplicationCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.InviteChannelMemberCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.query.ListChannelApplicationsQuery;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelBan;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelInvite;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelInviteStatus;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMember;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMemberRole;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.chat.domain.features.channel.domain.service.ChannelDomainApiTestSupport.RollbackingTransactionRunner;
import team.carrypigeon.backend.chat.domain.features.channel.domain.service.ChannelDomainApiTestSupport.TestContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static team.carrypigeon.backend.chat.domain.features.channel.domain.service.ChannelDomainApiTestSupport.BASE_TIME;
import static team.carrypigeon.backend.chat.domain.features.channel.domain.service.ChannelDomainApiTestSupport.newContext;
import static team.carrypigeon.backend.chat.domain.features.channel.domain.service.ChannelDomainApiTestSupport.privateChannel;
import static team.carrypigeon.backend.chat.domain.features.channel.domain.service.ChannelDomainApiTestSupport.profile;

/**
 * 频道申请与邀请流契约测试。
 * 职责：验证邀请、申请、审批和接受邀请相关的应用层编排语义。
 * 边界：不验证 HTTP 协议层与真实数据库访问，只使用内存替身验证业务语义。
 */
@Tag("contract")
class ChannelDomainApiApplicationFlowTests {

    /**
     * 验证 OWNER 可以向 private channel 发起成员邀请。
     */
    @Test
    @DisplayName("invite channel member owner invite creates pending invite")
    void inviteChannelMember_ownerInvite_createsPendingInvite() {
        TestContext context = newContext();
        context.channelRepository.channels.put(9L, privateChannel(9L, "project-alpha"));
        context.channelMemberRepository.save(new ChannelMember(9L, 1001L, ChannelMemberRole.OWNER, BASE_TIME, null));
        context.userProfileRepository.save(profile(1002L, "carry-guest"));
        ChannelApplicationFlowDomainApi service = context.createApplicationFlowService();

        var result = service.inviteChannelMember(new InviteChannelMemberCommand(1001L, 9L, 1002L));

        assertEquals(9L, result.channelId());
        assertEquals(1002L, result.inviteeAccountId());
        assertEquals("PENDING", result.status());
        assertEquals("PENDING", context.channelInviteRepository.savedInvite.status().name());
        assertTrue(context.realtimeEventApi.channelChangedScopes.contains("applications"));
        assertTrue(context.realtimeEventApi.channelsChangedAccountIds.contains(1002L));
    }

    /**
     * 验证非成员可以创建待审批的入群申请。
     */
    @Test
    @DisplayName("create channel application non member creates pending application")
    void createChannelApplication_nonMember_createsPendingApplication() {
        TestContext context = newContext();
        context.channelRepository.channels.put(9L, privateChannel(9L, "project-alpha"));
        ChannelApplicationFlowDomainApi service = context.createApplicationFlowService();

        var result = service.createChannelApplication(new CreateChannelApplicationCommand(1002L, 9L, "hi"));

        assertEquals(9L, result.channelId());
        assertEquals(1002L, result.accountId());
        assertEquals("PENDING", result.status());
        assertEquals(1002L, context.channelInviteRepository.savedInvite.inviteeAccountId());
        assertTrue(context.realtimeEventApi.channelChangedScopes.contains("applications"));
        assertTrue(context.realtimeEventApi.channelsChangedAccountIds.contains(1002L));
    }

    /**
     * 验证 OWNER 可以读取待处理的入群申请列表。
     */
    @Test
    @DisplayName("list channel applications owner returns pending items")
    void listChannelApplications_owner_returnsPendingItems() {
        TestContext context = newContext();
        context.channelRepository.channels.put(9L, privateChannel(9L, "project-alpha"));
        context.channelMemberRepository.save(new ChannelMember(9L, 1001L, ChannelMemberRole.OWNER, BASE_TIME, null));
        context.channelInviteRepository.savedInvite = new ChannelInvite(
                9L,
                3001L,
                1002L,
                1002L,
                ChannelInviteStatus.PENDING,
                BASE_TIME,
                null
        );
        ChannelApplicationFlowDomainApi service = context.createApplicationFlowService();

        var result = service.listChannelApplications(new ListChannelApplicationsQuery(1001L, 9L));

        assertEquals(1, result.size());
        assertEquals(3001L, result.getFirst().applicationId());
        assertEquals("PENDING", result.getFirst().status());
    }

    /**
     * 验证直接邀请记录不会混入申请列表。
     */
    @Test
    @DisplayName("list channel applications ignores direct invite records")
    void listChannelApplications_directInvite_returnsEmptyList() {
        TestContext context = newContext();
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
        ChannelApplicationFlowDomainApi service = context.createApplicationFlowService();

        var result = service.listChannelApplications(new ListChannelApplicationsQuery(1001L, 9L));

        assertEquals(0, result.size());
    }

    /**
     * 验证审批通过申请时会创建成员并更新申请状态。
     */
    @Test
    @DisplayName("decide channel application approve creates membership and updates status")
    void decideChannelApplication_approve_createsMembershipAndUpdatesStatus() {
        TestContext context = newContext();
        context.channelRepository.channels.put(9L, privateChannel(9L, "project-alpha"));
        context.channelMemberRepository.save(new ChannelMember(9L, 1001L, ChannelMemberRole.OWNER, BASE_TIME, null));
        context.channelInviteRepository.savedInvite = new ChannelInvite(
                9L,
                3001L,
                1002L,
                1002L,
                ChannelInviteStatus.PENDING,
                BASE_TIME,
                null
        );
        ChannelApplicationFlowDomainApi service = context.createApplicationFlowService();

        var result = service.decideChannelApplication(new DecideChannelApplicationCommand(1001L, 9L, 3001L, "approve"));

        assertEquals("ACCEPTED", result.status());
        assertEquals(true, context.channelMemberRepository.exists(9L, 1002L));
        assertEquals(ChannelInviteStatus.ACCEPTED, context.channelInviteRepository.updatedInvite.status());
        assertTrue(context.realtimeEventApi.channelChangedScopes.contains("applications"));
        assertTrue(context.realtimeEventApi.channelChangedScopes.contains("members"));
        assertTrue(context.realtimeEventApi.channelsChangedAccountIds.contains(1002L));
    }

    /**
     * 验证申请人在提交申请后被封禁时，审批通过会被封禁策略拦截。
     */
    @Test
    @DisplayName("decide channel application approve active ban throws forbidden problem")
    void decideChannelApplication_approveActiveBan_throwsForbiddenProblem() {
        TestContext context = newContext();
        context.channelRepository.channels.put(9L, privateChannel(9L, "project-alpha"));
        context.channelMemberRepository.save(new ChannelMember(9L, 1001L, ChannelMemberRole.OWNER, BASE_TIME, null));
        context.channelInviteRepository.savedInvite = new ChannelInvite(
                9L,
                3001L,
                1002L,
                1002L,
                ChannelInviteStatus.PENDING,
                BASE_TIME.minusSeconds(60),
                null
        );
        context.channelBanRepository.save(new ChannelBan(9L, 1002L, 1001L, "spam", null, BASE_TIME, null));
        ChannelApplicationFlowDomainApi service = context.createApplicationFlowService();

        ProblemException exception = assertThrows(
                ProblemException.class,
                () -> service.decideChannelApplication(new DecideChannelApplicationCommand(1001L, 9L, 3001L, "approve"))
        );

        assertEquals("channel ban is still active", exception.getMessage());
        assertEquals(false, context.channelMemberRepository.exists(9L, 1002L));
        assertEquals(ChannelInviteStatus.PENDING, context.channelInviteRepository.savedInvite.status());
    }

    /**
     * 验证审批时若记录其实是直接邀请，会返回 not found 语义。
     */
    @Test
    @DisplayName("decide channel application direct invite throws not found problem")
    void decideChannelApplication_directInvite_throwsNotFoundProblem() {
        TestContext context = newContext();
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
        ChannelApplicationFlowDomainApi service = context.createApplicationFlowService();

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
        TestContext context = newContext();
        context.channelRepository.channels.put(9L, privateChannel(9L, "project-alpha"));
        context.channelMemberRepository.save(new ChannelMember(9L, 1001L, ChannelMemberRole.MEMBER, BASE_TIME, null));
        context.userProfileRepository.save(profile(1002L, "carry-guest"));
        ChannelApplicationFlowDomainApi service = context.createApplicationFlowService();

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
        TestContext context = newContext();
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
        ChannelApplicationFlowDomainApi service = context.createApplicationFlowService();

        var result = service.acceptChannelInvite(new AcceptChannelInviteCommand(1002L, 9L));

        assertEquals("ACCEPTED", result.status());
        ChannelMember acceptedMember = context.channelMemberRepository.findByChannelIdAndAccountId(9L, 1002L).orElseThrow();
        assertEquals(ChannelMemberRole.MEMBER, acceptedMember.role());
        assertEquals(ChannelInviteStatus.ACCEPTED, context.channelInviteRepository.updatedInvite.status());
        assertTrue(context.realtimeEventApi.channelChangedScopes.contains("applications"));
        assertTrue(context.realtimeEventApi.channelChangedScopes.contains("members"));
        assertTrue(context.realtimeEventApi.channelsChangedAccountIds.contains(1002L));
    }

    /**
     * 验证申请记录不能被当作直接邀请接受。
     */
    @Test
    @DisplayName("accept channel invite application record throws not found problem")
    void acceptChannelInvite_applicationRecord_throwsNotFoundProblem() {
        TestContext context = newContext();
        context.channelRepository.channels.put(9L, privateChannel(9L, "project-alpha"));
        context.channelInviteRepository.savedInvite = new ChannelInvite(
                9L,
                3001L,
                1002L,
                1002L,
                ChannelInviteStatus.PENDING,
                BASE_TIME,
                null
        );
        ChannelApplicationFlowDomainApi service = context.createApplicationFlowService();

        ProblemException exception = assertThrows(
                ProblemException.class,
                () -> service.acceptChannelInvite(new AcceptChannelInviteCommand(1002L, 9L))
        );

        assertEquals("channel invite does not exist", exception.getMessage());
    }

    /**
     * 验证事务回滚时申请链路不会提前广播变更。
     */
    @Test
    @DisplayName("create channel application rollback skips realtime publish")
    void createChannelApplication_rollback_skipsRealtimePublish() {
        TestContext context = newContext();
        context.channelRepository.channels.put(9L, privateChannel(9L, "project-alpha"));
        ChannelApplicationFlowDomainApi service = context.createApplicationFlowService(new RollbackingTransactionRunner());

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.createChannelApplication(new CreateChannelApplicationCommand(1002L, 9L, "hi"))
        );

        assertEquals("transaction rolled back", exception.getMessage());
        assertEquals(0, context.realtimeEventApi.channelChangedScopes.size());
        assertEquals(0, context.realtimeEventApi.channelsChangedAccountIds.size());
    }

    /**
     * 验证重复申请时会改写旧记录为新的申请，而不是新增无序记录。
     */
    @Test
    @DisplayName("create channel application existing invite rewrites record as application")
    void createChannelApplication_existingInvite_rewritesRecordAsApplication() {
        TestContext context = newContext();
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
        ChannelApplicationFlowDomainApi service = context.createApplicationFlowService();

        var result = service.createChannelApplication(new CreateChannelApplicationCommand(1002L, 9L, "retry"));

        assertEquals(3001L, result.applicationId());
        assertEquals(1002L, context.channelInviteRepository.updatedInvite.inviterAccountId());
        assertEquals(ChannelInviteStatus.PENDING, context.channelInviteRepository.updatedInvite.status());
    }
}
