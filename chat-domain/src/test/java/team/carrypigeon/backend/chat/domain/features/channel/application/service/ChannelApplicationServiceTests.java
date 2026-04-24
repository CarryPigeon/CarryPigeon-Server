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
import team.carrypigeon.backend.chat.domain.features.channel.application.command.DemoteChannelAdminCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.GetDefaultChannelCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.InviteChannelMemberCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.KickChannelMemberCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.MuteChannelMemberCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.PromoteChannelMemberCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.TransferChannelOwnershipCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.UnbanChannelMemberCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.UnmuteChannelMemberCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.dto.ChannelBanResult;
import team.carrypigeon.backend.chat.domain.features.channel.application.dto.ChannelInviteResult;
import team.carrypigeon.backend.chat.domain.features.channel.application.dto.ChannelMemberResult;
import team.carrypigeon.backend.chat.domain.features.channel.application.dto.ChannelOwnershipTransferResult;
import team.carrypigeon.backend.chat.domain.features.channel.application.dto.ChannelResult;
import team.carrypigeon.backend.chat.domain.features.channel.application.query.ListChannelMembersQuery;
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
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.service.ChannelGovernancePolicy;
import team.carrypigeon.backend.chat.domain.features.user.domain.model.UserProfile;
import team.carrypigeon.backend.chat.domain.features.user.domain.repository.UserProfileRepository;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.id.IdGenerator;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;
import team.carrypigeon.backend.infrastructure.service.database.api.transaction.TransactionRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
        assertEquals("public", result.type());
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
        assertEquals(false, result.defaultChannel());
        assertNotNull(context.channelRepository.savedChannel);
        ChannelMember ownerMember = context.channelMemberRepository.findByChannelIdAndAccountId(result.channelId(), 1001L).orElseThrow();
        assertEquals(ChannelMemberRole.OWNER, ownerMember.role());
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
        return new Channel(id, id, name, "private", false, BASE_TIME, BASE_TIME);
    }

    private static Channel publicChannel() {
        return new Channel(1L, 1L, "public", "public", true, BASE_TIME, BASE_TIME);
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
        private final InMemoryUserProfileRepository userProfileRepository = new InMemoryUserProfileRepository();

        private TestContext() {
            channelRepository.defaultChannel = publicChannel();
            channelRepository.channels.put(1L, channelRepository.defaultChannel);
        }

        private ChannelApplicationService createService() {
            return new ChannelApplicationService(
                    channelRepository,
                    channelMemberRepository,
                    channelInviteRepository,
                    channelBanRepository,
                    channelAuditLogRepository,
                    userProfileRepository,
                    new ChannelGovernancePolicy(),
                    new FixedIdGenerator(),
                    new TimeProvider(Clock.fixed(BASE_TIME, ZoneOffset.UTC)),
                    new NoopTransactionRunner()
            );
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
        public Optional<Channel> findById(long channelId) {
            return Optional.ofNullable(channels.get(channelId));
        }

        @Override
        public Channel save(Channel channel) {
            this.savedChannel = channel;
            this.channels.put(channel.id(), channel);
            return channel;
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
}
