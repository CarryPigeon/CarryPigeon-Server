package team.carrypigeon.backend.chat.domain.features.channel.domain.service;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.chat.domain.features.channel.domain.query.ListAuditLogsQuery;
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
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MessageRepository;
import team.carrypigeon.backend.chat.domain.features.user.domain.repository.UserProfileRepository;
import team.carrypigeon.backend.chat.domain.support.TestFeatureApis;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.id.IdGenerator;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;
import team.carrypigeon.backend.infrastructure.service.database.api.transaction.TransactionRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
/**
 * `AuditLogDomainApi` 契约测试。
 * 职责：验证当前测试类覆盖对象的关键成功路径、失败路径或边界行为。
 */

@Tag("contract")
class AuditLogDomainApiTests {

    /**
     * 验证 `listAuditLogs` 在 `mapsResults` 场景下的测试契约。
     */
    @Test
    @DisplayName("list audit logs maps results")
    void listAuditLogs_mapsResults() {
        StubChannelAuditLogRepository auditLogRepository = new StubChannelAuditLogRepository();
        auditLogRepository.logs = List.of(new ChannelAuditLog(7001L, 9L, 1001L, "MEMBER_BANNED", 1002L, "{}", Instant.parse("2026-04-24T12:00:00Z")));
        ChannelQueryDomainApi service = createService(auditLogRepository);

        var result = service.listAuditLogs(new ListAuditLogsQuery(1001L, null, 50, 9L, null, null, null, null));

        assertEquals("7001", result.getFirst().auditId());
        assertEquals("channel.ban.create", result.getFirst().action());
    }

    /**
     * 验证审计查询外部 action 过滤值会转换为仓储内部 actionType。
     */
    @Test
    @DisplayName("list audit logs normalizes action filter")
    void listAuditLogs_actionFilter_normalizesActionType() {
        StubChannelAuditLogRepository auditLogRepository = new StubChannelAuditLogRepository();
        ChannelQueryDomainApi service = createService(auditLogRepository);

        service.listAuditLogs(new ListAuditLogsQuery(1001L, null, 50, 9L, null, "message.pin", null, null));

        assertEquals("MESSAGE_PINNED", auditLogRepository.lastActionType);
    }

    /**
     * 验证未指定频道时只查询当前账号已加入频道，并按审计 ID 倒序合并结果。
     */
    @Test
    @DisplayName("list audit logs without channel id merges member channels")
    void listAuditLogs_withoutChannelId_mergesMemberChannels() {
        StubChannelAuditLogRepository auditLogRepository = new StubChannelAuditLogRepository();
        auditLogRepository.logs = List.of(
                new ChannelAuditLog(7001L, 9L, 1001L, "MEMBER_BANNED", 1002L, "{}", Instant.parse("2026-04-24T12:00:00Z")),
                new ChannelAuditLog(7003L, 10L, 1001L, "MEMBER_MUTED", 1003L, "{}", Instant.parse("2026-04-24T12:01:00Z")),
                new ChannelAuditLog(7004L, 11L, 1001L, "MEMBER_MUTED", 1004L, "{}", Instant.parse("2026-04-24T12:02:00Z"))
        );
        ChannelQueryDomainApi service = createService(auditLogRepository);

        var result = service.listAuditLogs(new ListAuditLogsQuery(1001L, null, 50, null, null, null, null, null));

        assertEquals(2, result.size());
        assertEquals("7003", result.get(0).auditId());
        assertEquals("7001", result.get(1).auditId());
        assertEquals(List.of(9L, 10L), auditLogRepository.queriedChannelIds);
    }

    /**
     * 验证 `listAuditLogs` 在 `invalidCursor` 条件下满足 `throwsCursorInvalid` 的测试契约。
     */
    @Test
    @DisplayName("list audit logs invalid cursor throws cursor invalid")
    void listAuditLogs_invalidCursor_throwsCursorInvalid() {
        ChannelQueryDomainApi service = createService(new StubChannelAuditLogRepository());

        ProblemException exception = assertThrows(ProblemException.class, () -> service.listAuditLogs(new ListAuditLogsQuery(1001L, 0L, 50, null, null, null, null, null)));

        assertEquals("cursor_invalid", exception.reason());
    }

    private ChannelQueryDomainApi createService(ChannelAuditLogRepository auditLogRepository) {
        ChannelRepository channelRepository = new StubChannelRepository();
        ChannelMemberRepository channelMemberRepository = new StubChannelMemberRepository();
        ChannelBanRepository channelBanRepository = new StubChannelBanRepository();
        ChannelReadStateRepository channelReadStateRepository = new StubChannelReadStateRepository();
        UserProfileRepository userProfileRepository = new StubUserProfileRepository();
        return new ChannelQueryDomainApi(
                channelRepository,
                channelMemberRepository,
                channelBanRepository,
                auditLogRepository,
                channelReadStateRepository,
                TestFeatureApis.userProfiles(userProfileRepository),
                new ChannelGovernancePolicy()
        );
    }

    /**
     * `StubChannelRepository` 测试替身。
     * 职责：隔离外部依赖，使测试只验证当前契约边界。
     */
    private static final class StubChannelRepository implements ChannelRepository {
        @Override public Optional<Channel> findDefaultChannel() { return Optional.empty(); }
        @Override public Optional<Channel> findSystemChannel() { return Optional.empty(); }
        @Override public Optional<Channel> findById(long channelId) { return Optional.of(new Channel(channelId, channelId, "general", "", "", "1001", "private", false, Instant.parse("2026-04-24T12:00:00Z"), Instant.parse("2026-04-24T12:00:00Z"))); }
    }

    /**
     * `StubChannelMemberRepository` 测试替身。
     * 职责：隔离外部依赖，使测试只验证当前契约边界。
     */
    private static final class StubChannelMemberRepository implements ChannelMemberRepository {
        @Override public boolean exists(long channelId, long accountId) { return true; }
        @Override public void save(ChannelMember channelMember) { }
        @Override public Optional<ChannelMember> findByChannelIdAndAccountId(long channelId, long accountId) { return Optional.of(new ChannelMember(channelId, accountId, ChannelMemberRole.OWNER, Instant.parse("2026-04-24T12:00:00Z"), null)); }
        @Override public List<ChannelMember> findByChannelId(long channelId) { return List.of(); }
        @Override public List<Long> findAccountIdsByChannelId(long channelId) { return List.of(); }
        @Override public List<Long> findChannelIdsByAccountId(long accountId) { return List.of(9L, 10L); }
    }

    /**
     * `StubChannelInviteRepository` 测试替身。
     * 职责：隔离外部依赖，使测试只验证当前契约边界。
     */
    private static final class StubChannelInviteRepository implements ChannelInviteRepository {
        @Override public Optional<team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelInvite> findByChannelIdAndInviteeAccountId(long channelId, long inviteeAccountId) { return Optional.empty(); }
        @Override public void save(team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelInvite channelInvite) { }
        @Override public void update(team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelInvite channelInvite) { }
        @Override public Optional<team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelInvite> findByChannelIdAndApplicationId(long channelId, long applicationId) { return Optional.empty(); }
        @Override public List<team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelInvite> findByChannelId(long channelId) { return List.of(); }
    }

    /**
     * `StubChannelBanRepository` 测试替身。
     * 职责：隔离外部依赖，使测试只验证当前契约边界。
     */
    private static final class StubChannelBanRepository implements ChannelBanRepository {
        @Override public Optional<team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelBan> findByChannelIdAndBannedAccountId(long channelId, long bannedAccountId) { return Optional.empty(); }
        @Override public void save(team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelBan channelBan) { }
        @Override public void update(team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelBan channelBan) { }
        @Override public List<team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelBan> findByChannelId(long channelId) { return List.of(); }
    }

    /**
     * `StubChannelReadStateRepository` 测试替身。
     * 职责：隔离外部依赖，使测试只验证当前契约边界。
     */
    private static final class StubChannelReadStateRepository implements ChannelReadStateRepository {
        @Override public Optional<team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelReadState> findByChannelIdAndAccountId(long channelId, long accountId) { return Optional.empty(); }
        @Override public team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelReadState upsert(team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelReadState readState) { return readState; }
        @Override public List<team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelUnread> listUnreadsByAccountId(long accountId) { return List.of(); }
    }

    /**
     * `StubMessageRepository` 测试替身。
     * 职责：隔离外部依赖，使测试只验证当前契约边界。
     */
    private static final class StubMessageRepository implements MessageRepository {
        @Override public team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage save(team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage message) { return message; }
        @Override public Optional<team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage> findById(long messageId) { return Optional.empty(); }
        @Override public team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage update(team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage message) { return message; }
        @Override public List<team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage> findByChannelIdBefore(long channelId, Long cursorMessageId, int limit) { return List.of(); }
        @Override public List<team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage> searchByChannelId(long channelId, String keyword, int limit) { return List.of(); }
    }

    /**
     * `StubUserProfileRepository` 测试替身。
     * 职责：隔离外部依赖，使测试只验证当前契约边界。
     */
    private static final class StubUserProfileRepository implements UserProfileRepository {
        @Override public Optional<team.carrypigeon.backend.chat.domain.features.user.domain.model.UserProfile> findByAccountId(long accountId) { return Optional.empty(); }
        @Override public List<team.carrypigeon.backend.chat.domain.features.user.domain.model.UserProfile> findAll() { return List.of(); }
        @Override public List<team.carrypigeon.backend.chat.domain.features.user.domain.model.UserProfile> findByAccountIdBefore(Long cursorAccountId, int limit) { return List.of(); }
        @Override public List<team.carrypigeon.backend.chat.domain.features.user.domain.model.UserProfile> searchByKeyword(String keyword, Long cursorAccountId, int limit) { return List.of(); }
        @Override public team.carrypigeon.backend.chat.domain.features.user.domain.model.UserProfile save(team.carrypigeon.backend.chat.domain.features.user.domain.model.UserProfile userProfile) { return userProfile; }
        @Override public team.carrypigeon.backend.chat.domain.features.user.domain.model.UserProfile update(team.carrypigeon.backend.chat.domain.features.user.domain.model.UserProfile userProfile) { return userProfile; }
    }

    /**
     * `StubChannelAuditLogRepository` 测试替身。
     * 职责：隔离外部依赖，使测试只验证当前契约边界。
     */
    private static final class StubChannelAuditLogRepository implements ChannelAuditLogRepository {
        private List<ChannelAuditLog> logs = List.of();
        private List<Long> queriedChannelIds = new java.util.ArrayList<>();
        private String lastActionType;
        @Override public void append(ChannelAuditLog channelAuditLog) { }
        @Override public List<ChannelAuditLog> list(Long cursorAuditId, int limit, Long channelId, Long actorAccountId, String actionType, Instant fromTime, Instant toTime) {
            queriedChannelIds.add(channelId);
            this.lastActionType = actionType;
            return logs.stream()
                    .filter(log -> channelId == null || log.channelId() == channelId)
                    .limit(limit)
                    .toList();
        }
    }

    /**
     * `FixedIdGenerator` 测试替身。
     * 职责：隔离外部依赖，使测试只验证当前契约边界。
     */
    private static final class FixedIdGenerator implements IdGenerator {
        @Override public long nextLongId() { return 9001L; }
    }

    /**
     * `NoopTransactionRunner` 测试替身。
     * 职责：隔离外部依赖，使测试只验证当前契约边界。
     */
    private static final class NoopTransactionRunner implements TransactionRunner {
        @Override public <T> T runInTransaction(java.util.function.Supplier<T> action) { return action.get(); }
        @Override public void runInTransaction(Runnable action) { action.run(); }
    }
}
