package team.carrypigeon.backend.chat.domain.features.channel.application.service;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.chat.domain.features.channel.application.query.ListAuditLogsQuery;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.Channel;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelAuditLog;
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
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MessageRepository;
import team.carrypigeon.backend.chat.domain.features.user.domain.repository.UserProfileRepository;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.id.IdGenerator;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;
import team.carrypigeon.backend.infrastructure.service.database.api.transaction.TransactionRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("contract")
class AuditLogApplicationServiceTests {

    @Test
    @DisplayName("list audit logs maps results")
    void listAuditLogs_mapsResults() {
        StubChannelAuditLogRepository auditLogRepository = new StubChannelAuditLogRepository();
        auditLogRepository.logs = List.of(new ChannelAuditLog(7001L, 9L, 1001L, "MEMBER_BANNED", 1002L, "{}", Instant.parse("2026-04-24T12:00:00Z")));
        ChannelApplicationService service = createService(auditLogRepository);

        var result = service.listAuditLogs(new ListAuditLogsQuery(1001L, null, 50, null, null, null, null, null));

        assertEquals("7001", result.getFirst().auditId());
        assertEquals("channel.ban.create", result.getFirst().action());
    }

    @Test
    @DisplayName("list audit logs invalid cursor throws cursor invalid")
    void listAuditLogs_invalidCursor_throwsCursorInvalid() {
        ChannelApplicationService service = createService(new StubChannelAuditLogRepository());

        ProblemException exception = assertThrows(ProblemException.class, () -> service.listAuditLogs(new ListAuditLogsQuery(1001L, 0L, 50, null, null, null, null, null)));

        assertEquals("cursor_invalid", exception.reason());
    }

    private ChannelApplicationService createService(ChannelAuditLogRepository auditLogRepository) {
        return new ChannelApplicationService(
                new StubChannelRepository(),
                new StubChannelMemberRepository(),
                new StubChannelInviteRepository(),
                new StubChannelBanRepository(),
                auditLogRepository,
                new StubChannelReadStateRepository(),
                new StubMessageRepository(),
                new StubUserProfileRepository(),
                new ChannelGovernancePolicy(),
                new ChannelRealtimePublisher() {
                },
                new FixedIdGenerator(),
                new TimeProvider(Clock.fixed(Instant.parse("2026-04-24T12:00:00Z"), ZoneOffset.UTC)),
                new NoopTransactionRunner()
        );
    }

    private static final class StubChannelRepository implements ChannelRepository {
        @Override public Optional<Channel> findDefaultChannel() { return Optional.empty(); }
        @Override public Optional<Channel> findSystemChannel() { return Optional.empty(); }
        @Override public Optional<Channel> findById(long channelId) { return Optional.of(new Channel(channelId, channelId, "general", "", "", "1001", "private", false, Instant.parse("2026-04-24T12:00:00Z"), Instant.parse("2026-04-24T12:00:00Z"))); }
    }

    private static final class StubChannelMemberRepository implements ChannelMemberRepository {
        @Override public boolean exists(long channelId, long accountId) { return true; }
        @Override public void save(ChannelMember channelMember) { }
        @Override public Optional<ChannelMember> findByChannelIdAndAccountId(long channelId, long accountId) { return Optional.of(new ChannelMember(channelId, accountId, ChannelMemberRole.OWNER, Instant.parse("2026-04-24T12:00:00Z"), null)); }
        @Override public List<ChannelMember> findByChannelId(long channelId) { return List.of(); }
        @Override public List<Long> findAccountIdsByChannelId(long channelId) { return List.of(); }
    }

    private static final class StubChannelInviteRepository implements ChannelInviteRepository {
        @Override public Optional<team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelInvite> findByChannelIdAndInviteeAccountId(long channelId, long inviteeAccountId) { return Optional.empty(); }
        @Override public void save(team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelInvite channelInvite) { }
        @Override public void update(team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelInvite channelInvite) { }
        @Override public Optional<team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelInvite> findByChannelIdAndApplicationId(long channelId, long applicationId) { return Optional.empty(); }
        @Override public List<team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelInvite> findByChannelId(long channelId) { return List.of(); }
    }

    private static final class StubChannelBanRepository implements ChannelBanRepository {
        @Override public Optional<team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelBan> findByChannelIdAndBannedAccountId(long channelId, long bannedAccountId) { return Optional.empty(); }
        @Override public void save(team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelBan channelBan) { }
        @Override public void update(team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelBan channelBan) { }
        @Override public List<team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelBan> findByChannelId(long channelId) { return List.of(); }
    }

    private static final class StubChannelReadStateRepository implements ChannelReadStateRepository {
        @Override public Optional<team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelReadState> findByChannelIdAndAccountId(long channelId, long accountId) { return Optional.empty(); }
        @Override public team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelReadState upsert(team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelReadState readState) { return readState; }
        @Override public List<team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelUnread> listUnreadsByAccountId(long accountId) { return List.of(); }
    }

    private static final class StubMessageRepository implements MessageRepository {
        @Override public team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage save(team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage message) { return message; }
        @Override public Optional<team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage> findById(long messageId) { return Optional.empty(); }
        @Override public team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage update(team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage message) { return message; }
        @Override public List<team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage> findByChannelIdBefore(long channelId, Long cursorMessageId, int limit) { return List.of(); }
        @Override public List<team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage> searchByChannelId(long channelId, String keyword, int limit) { return List.of(); }
    }

    private static final class StubUserProfileRepository implements UserProfileRepository {
        @Override public Optional<team.carrypigeon.backend.chat.domain.features.user.domain.model.UserProfile> findByAccountId(long accountId) { return Optional.empty(); }
        @Override public List<team.carrypigeon.backend.chat.domain.features.user.domain.model.UserProfile> findAll() { return List.of(); }
        @Override public List<team.carrypigeon.backend.chat.domain.features.user.domain.model.UserProfile> findByAccountIdBefore(Long cursorAccountId, int limit) { return List.of(); }
        @Override public List<team.carrypigeon.backend.chat.domain.features.user.domain.model.UserProfile> searchByKeyword(String keyword, Long cursorAccountId, int limit) { return List.of(); }
        @Override public team.carrypigeon.backend.chat.domain.features.user.domain.model.UserProfile save(team.carrypigeon.backend.chat.domain.features.user.domain.model.UserProfile userProfile) { return userProfile; }
        @Override public team.carrypigeon.backend.chat.domain.features.user.domain.model.UserProfile update(team.carrypigeon.backend.chat.domain.features.user.domain.model.UserProfile userProfile) { return userProfile; }
    }

    private static final class StubChannelAuditLogRepository implements ChannelAuditLogRepository {
        private List<ChannelAuditLog> logs = List.of();
        @Override public void append(ChannelAuditLog channelAuditLog) { }
        @Override public List<ChannelAuditLog> list(Long cursorAuditId, int limit, Long channelId, Long actorAccountId, String actionType, Instant fromTime, Instant toTime) { return logs; }
    }

    private static final class FixedIdGenerator implements IdGenerator {
        @Override public long nextLongId() { return 9001L; }
    }

    private static final class NoopTransactionRunner implements TransactionRunner {
        @Override public <T> T runInTransaction(java.util.function.Supplier<T> action) { return action.get(); }
        @Override public void runInTransaction(Runnable action) { action.run(); }
    }
}
