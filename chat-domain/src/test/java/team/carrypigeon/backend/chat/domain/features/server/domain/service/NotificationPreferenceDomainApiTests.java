package team.carrypigeon.backend.chat.domain.features.server.domain.service;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.Channel;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelMemberRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelRepository;
import team.carrypigeon.backend.chat.domain.features.server.domain.command.UpdateNotificationChannelPreferenceCommand;
import team.carrypigeon.backend.chat.domain.features.server.domain.command.UpdateNotificationServerPreferenceCommand;
import team.carrypigeon.backend.chat.domain.features.server.domain.model.NotificationChannelPreference;
import team.carrypigeon.backend.chat.domain.features.server.domain.model.NotificationServerPreference;
import team.carrypigeon.backend.chat.domain.features.server.domain.repository.NotificationPreferenceRepository;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
/**
 * NotificationPreferenceDomainApi 契约测试。
 * 职责：验证服务端与频道级通知偏好的默认值、持久化归一化和成员权限边界。
 * 边界：仓储使用测试替身，不验证数据库适配器。
 */
@Tag("contract")
class NotificationPreferenceDomainApiTests {

    /**
     * 验证缺失偏好记录时会返回服务端默认配置和空频道列表。
     */
    @Test
    @DisplayName("get notification preferences returns defaults when missing")
    void getNotificationPreferences_missingPreferences_returnsDefaults() {
        RecordingNotificationPreferenceRepository repository = new RecordingNotificationPreferenceRepository();
        NotificationPreferenceDomainApi service = new NotificationPreferenceDomainApi(repository, new StubChannelRepository(), new StubChannelMemberRepository(), timeProvider());

        var result = service.getNotificationPreferences(1001L);

        assertEquals("all", result.server().mode());
        assertEquals(0L, result.server().mutedUntil());
        assertEquals(0, result.channels().size());
    }

    /**
     * 验证更新服务端偏好时会持久化归一化后的模式和静音时间。
     */
    @Test
    @DisplayName("update server preference stores normalized preference")
    void updateServerPreference_storesNormalizedPreference() {
        RecordingNotificationPreferenceRepository repository = new RecordingNotificationPreferenceRepository();
        NotificationPreferenceDomainApi service = new NotificationPreferenceDomainApi(repository, new StubChannelRepository(), new StubChannelMemberRepository(), timeProvider());

        service.updateServerPreference(new UpdateNotificationServerPreferenceCommand(1001L, "muted", 0L));

        assertEquals("muted", repository.serverPreference.mode());
        assertEquals(0L, repository.serverPreference.mutedUntil());
    }

    /**
     * 验证非频道成员不能更新该频道的通知偏好。
     */
    @Test
    @DisplayName("update channel preference non member throws forbidden")
    void updateChannelPreference_nonMember_throwsForbidden() {
        RecordingNotificationPreferenceRepository repository = new RecordingNotificationPreferenceRepository();
        NotificationPreferenceDomainApi service = new NotificationPreferenceDomainApi(repository, new StubChannelRepository(), new NonMemberChannelMemberRepository(), timeProvider());

        ProblemException exception = assertThrows(ProblemException.class, () -> service.updateChannelPreference(new UpdateNotificationChannelPreferenceCommand(1001L, 9L, "inherit", 0L)));

        assertEquals("not_channel_member", exception.reason());
    }

    private static TimeProvider timeProvider() {
        return new TimeProvider(Clock.fixed(Instant.parse("2026-04-24T12:00:00Z"), ZoneOffset.UTC));
    }

    /**
     * `RecordingNotificationPreferenceRepository` 测试替身。
     * 职责：隔离外部依赖，使测试只验证当前契约边界。
     */
    private static final class RecordingNotificationPreferenceRepository implements NotificationPreferenceRepository {
        private NotificationServerPreference serverPreference;
        private NotificationChannelPreference channelPreference;

        @Override
        public Optional<NotificationServerPreference> findServerPreferenceByAccountId(long accountId) {
            return Optional.ofNullable(serverPreference);
        }

        @Override
        public List<NotificationChannelPreference> listChannelPreferencesByAccountId(long accountId) {
            return channelPreference == null ? List.of() : List.of(channelPreference);
        }

        @Override
        public NotificationServerPreference upsertServerPreference(NotificationServerPreference preference) {
            this.serverPreference = preference;
            return preference;
        }

        @Override
        public NotificationChannelPreference upsertChannelPreference(NotificationChannelPreference preference) {
            this.channelPreference = preference;
            return preference;
        }
    }

    /**
     * `StubChannelRepository` 测试替身。
     * 职责：隔离外部依赖，使测试只验证当前契约边界。
     */
    private static final class StubChannelRepository implements ChannelRepository {
        @Override public Optional<Channel> findDefaultChannel() { return Optional.empty(); }
        @Override public Optional<Channel> findSystemChannel() { return Optional.empty(); }
        @Override public Optional<Channel> findById(long channelId) { return Optional.of(new Channel(channelId, channelId, "general", "", "", "1001", "public", false, Instant.parse("2026-04-24T12:00:00Z"), Instant.parse("2026-04-24T12:00:00Z"))); }
    }

    /**
     * `StubChannelMemberRepository` 测试替身。
     * 职责：隔离外部依赖，使测试只验证当前契约边界。
     */
    private static final class StubChannelMemberRepository implements ChannelMemberRepository {
        @Override public boolean exists(long channelId, long accountId) { return true; }
        @Override public void save(team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMember channelMember) { }
        @Override public Optional<team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMember> findByChannelIdAndAccountId(long channelId, long accountId) { return Optional.empty(); }
        @Override public List<team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMember> findByChannelId(long channelId) { return List.of(); }
        @Override public List<Long> findAccountIdsByChannelId(long channelId) { return List.of(); }
    }

    /**
     * `NonMemberChannelMemberRepository` 测试辅助类型。
     * 职责：隔离外部依赖，使测试只验证当前契约边界。
     */
    private static final class NonMemberChannelMemberRepository implements ChannelMemberRepository {
        @Override public boolean exists(long channelId, long accountId) { return false; }
        @Override public void save(team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMember channelMember) { }
        @Override public Optional<team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMember> findByChannelIdAndAccountId(long channelId, long accountId) { return Optional.empty(); }
        @Override public List<team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMember> findByChannelId(long channelId) { return List.of(); }
        @Override public List<Long> findAccountIdsByChannelId(long channelId) { return List.of(); }
    }
}
