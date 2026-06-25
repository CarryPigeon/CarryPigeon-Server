package team.carrypigeon.backend.chat.domain.features.channel.application.service;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.Channel;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelAuditLog;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelBan;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelInvite;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMember;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelReadState;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelUnread;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelAuditLogRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelBanRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelInviteRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelMemberRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelReadStateRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.service.ChannelGovernancePolicy;
import team.carrypigeon.backend.chat.domain.features.channel.domain.service.ChannelRealtimePublisher;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMemberRole;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MessageRepository;
import team.carrypigeon.backend.chat.domain.features.user.domain.model.UserProfile;
import team.carrypigeon.backend.chat.domain.features.user.domain.repository.UserProfileRepository;
import team.carrypigeon.backend.infrastructure.basic.id.IdGenerator;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;
import team.carrypigeon.backend.infrastructure.service.database.api.transaction.TransactionRunner;

/**
 * 频道应用测试支撑。
 * 职责：为频道应用服务契约测试提供固定时间、内存仓储替身和最小装配入口。
 * 边界：仅供当前测试包使用，不承载正式业务规则。
 */
final class ChannelApplicationServiceTestSupport {

    static final Instant BASE_TIME = Instant.parse("2026-04-24T12:00:00Z");

    private ChannelApplicationServiceTestSupport() {
    }

    static TestContext newContext() {
        return new TestContext();
    }

    static Channel privateChannel(long id, String name) {
        return new Channel(id, id, name, "", "", "", "private", false, BASE_TIME, BASE_TIME);
    }

    static Channel publicChannel() {
        return new Channel(1L, 1L, "public", "", "", "", "public", true, BASE_TIME, BASE_TIME);
    }

    static Channel systemChannel() {
        return new Channel(2L, 2L, "system", "", "", "", "system", false, BASE_TIME, BASE_TIME);
    }

    static UserProfile profile(long accountId, String nickname) {
        return new UserProfile(accountId, nickname, "", "", BASE_TIME, BASE_TIME);
    }

    static final class TestContext {

        final InMemoryChannelRepository channelRepository = new InMemoryChannelRepository();
        final InMemoryChannelMemberRepository channelMemberRepository = new InMemoryChannelMemberRepository();
        final InMemoryChannelInviteRepository channelInviteRepository = new InMemoryChannelInviteRepository();
        final InMemoryChannelBanRepository channelBanRepository = new InMemoryChannelBanRepository();
        final InMemoryChannelAuditLogRepository channelAuditLogRepository = new InMemoryChannelAuditLogRepository();
        final InMemoryChannelReadStateRepository channelReadStateRepository = new InMemoryChannelReadStateRepository();
        final InMemoryMessageRepository messageRepository = new InMemoryMessageRepository();
        final InMemoryUserProfileRepository userProfileRepository = new InMemoryUserProfileRepository();
        final RecordingChannelRealtimePublisher channelRealtimePublisher = new RecordingChannelRealtimePublisher();

        private TestContext() {
            channelRepository.defaultChannel = publicChannel();
            channelRepository.channels.put(1L, channelRepository.defaultChannel);
            channelMemberRepository.save(new ChannelMember(1L, 1001L, ChannelMemberRole.OWNER, BASE_TIME, null));
        }

        ChannelQueryApplicationService createQueryService() {
            return new ChannelQueryApplicationService(
                    channelRepository,
                    channelMemberRepository,
                    channelBanRepository,
                    channelAuditLogRepository,
                    channelReadStateRepository,
                    userProfileRepository,
                    governancePolicy()
            );
        }

        ChannelAccessApplicationService createAccessService() {
            return createAccessService(new NoopTransactionRunner());
        }

        ChannelAccessApplicationService createAccessService(TransactionRunner transactionRunner) {
            return new ChannelAccessApplicationService(
                    channelRepository,
                    channelMemberRepository,
                    channelInviteRepository,
                    channelBanRepository,
                    channelAuditLogRepository,
                    channelReadStateRepository,
                    messageRepository,
                    userProfileRepository,
                    governancePolicy(),
                    channelRealtimePublisher,
                    new FixedIdGenerator(),
                    timeProvider(),
                    transactionRunner
            );
        }

        ChannelLifecycleApplicationService createLifecycleService() {
            return createLifecycleService(new NoopTransactionRunner());
        }

        ChannelLifecycleApplicationService createLifecycleService(TransactionRunner transactionRunner) {
            return new ChannelLifecycleApplicationService(
                    channelRepository,
                    channelMemberRepository,
                    channelInviteRepository,
                    channelBanRepository,
                    channelAuditLogRepository,
                    channelReadStateRepository,
                    messageRepository,
                    userProfileRepository,
                    governancePolicy(),
                    channelRealtimePublisher,
                    new FixedIdGenerator(),
                    timeProvider(),
                    transactionRunner
            );
        }

        ChannelGovernanceApplicationService createGovernanceService() {
            return createGovernanceService(new NoopTransactionRunner());
        }

        ChannelGovernanceApplicationService createGovernanceService(TransactionRunner transactionRunner) {
            return new ChannelGovernanceApplicationService(
                    channelRepository,
                    channelMemberRepository,
                    channelInviteRepository,
                    channelBanRepository,
                    channelAuditLogRepository,
                    channelReadStateRepository,
                    messageRepository,
                    userProfileRepository,
                    governancePolicy(),
                    channelRealtimePublisher,
                    new FixedIdGenerator(),
                    timeProvider(),
                    transactionRunner
            );
        }

        ChannelApplicationFlowApplicationService createApplicationFlowService() {
            return createApplicationFlowService(new NoopTransactionRunner());
        }

        ChannelApplicationFlowApplicationService createApplicationFlowService(TransactionRunner transactionRunner) {
            return new ChannelApplicationFlowApplicationService(
                    channelRepository,
                    channelMemberRepository,
                    channelInviteRepository,
                    channelBanRepository,
                    channelAuditLogRepository,
                    channelReadStateRepository,
                    messageRepository,
                    userProfileRepository,
                    governancePolicy(),
                    channelRealtimePublisher,
                    new FixedIdGenerator(),
                    timeProvider(),
                    transactionRunner
            );
        }

        private ChannelGovernancePolicy governancePolicy() {
            return new ChannelGovernancePolicy();
        }

        private TimeProvider timeProvider() {
            return new TimeProvider(Clock.fixed(BASE_TIME, ZoneOffset.UTC));
        }
    }

    static final class InMemoryChannelReadStateRepository implements ChannelReadStateRepository {

        final Map<String, ChannelReadState> states = new HashMap<>();
        List<ChannelUnread> unreadResults = List.of();

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

    static final class InMemoryMessageRepository implements MessageRepository {

        final Map<Long, ChannelMessage> messagesById = new HashMap<>();

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
            return messagesById.values().stream()
                    .filter(message -> message.channelId() == channelId)
                    .sorted(java.util.Comparator.comparingLong(ChannelMessage::messageId).reversed())
                    .limit(limit)
                    .toList();
        }

        @Override
        public List<ChannelMessage> searchByChannelId(long channelId, String keyword, int limit) {
            return List.of();
        }
    }

    static final class RecordingChannelRealtimePublisher implements ChannelRealtimePublisher {

        final List<ChannelReadState> readStateUpdates = new ArrayList<>();
        final List<String> channelChangedScopes = new ArrayList<>();
        final List<Long> channelsChangedAccountIds = new ArrayList<>();

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

    static final class InMemoryChannelRepository implements ChannelRepository {

        final Map<Long, Channel> channels = new HashMap<>();
        Channel defaultChannel;
        Channel savedChannel;
        RuntimeException deleteFailure;

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
            savedChannel = channel;
            channels.put(channel.id(), channel);
            return channel;
        }

        @Override
        public Channel update(Channel channel) {
            channels.put(channel.id(), channel);
            return channel;
        }

        @Override
        public void delete(long channelId) {
            if (deleteFailure != null) {
                throw deleteFailure;
            }
            channels.remove(channelId);
        }
    }

    static final class InMemoryChannelMemberRepository implements ChannelMemberRepository {

        final Map<Long, List<ChannelMember>> membersByChannelId = new HashMap<>();

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

    static final class InMemoryChannelInviteRepository implements ChannelInviteRepository {

        ChannelInvite savedInvite;
        ChannelInvite updatedInvite;

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
            savedInvite = channelInvite;
        }

        @Override
        public void update(ChannelInvite channelInvite) {
            updatedInvite = channelInvite;
            savedInvite = channelInvite;
        }
    }

    static final class InMemoryChannelBanRepository implements ChannelBanRepository {

        ChannelBan channelBan;

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

    static final class InMemoryUserProfileRepository implements UserProfileRepository {

        final Map<Long, UserProfile> profiles = new HashMap<>();

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

    static final class InMemoryChannelAuditLogRepository implements ChannelAuditLogRepository {

        final List<ChannelAuditLog> logs = new ArrayList<>();

        @Override
        public void append(ChannelAuditLog channelAuditLog) {
            logs.add(channelAuditLog);
        }

        @Override
        public List<ChannelAuditLog> list(
                Long cursorAuditId,
                int limit,
                Long channelId,
                Long actorAccountId,
                String actionType,
                Instant fromTime,
                Instant toTime
        ) {
            return logs.stream()
                    .filter(log -> channelId == null || log.channelId() == channelId)
                    .filter(log -> actorAccountId == null || actorAccountId.equals(log.actorAccountId()))
                    .filter(log -> actionType == null || actionType.equals(log.actionType()))
                    .filter(log -> fromTime == null || !log.createdAt().isBefore(fromTime))
                    .filter(log -> toTime == null || !log.createdAt().isAfter(toTime))
                    .filter(log -> cursorAuditId == null || log.auditId() < cursorAuditId)
                    .limit(limit)
                    .toList();
        }
    }

    static final class FixedIdGenerator implements IdGenerator {

        @Override
        public long nextLongId() {
            return 2001L;
        }
    }

    static final class NoopTransactionRunner implements TransactionRunner {

        @Override
        public <T> T runInTransaction(java.util.function.Supplier<T> action) {
            return action.get();
        }

        @Override
        public void runInTransaction(Runnable action) {
            action.run();
        }
    }

    static final class RollbackingTransactionRunner implements TransactionRunner {

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
