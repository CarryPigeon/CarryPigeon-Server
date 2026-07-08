package team.carrypigeon.backend.chat.domain.features.server.support.realtime;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.chat.domain.features.server.domain.model.NotificationChannelPreference;
import team.carrypigeon.backend.chat.domain.features.server.domain.model.NotificationServerPreference;
import team.carrypigeon.backend.chat.domain.features.server.domain.repository.NotificationPreferenceRepository;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * RealtimeNotificationPreferenceFilter 契约测试。
 * 职责：验证 realtime 通知类事件在写入缓存与推送前会按通知偏好筛选接收账号。
 * 边界：不验证 Netty 帧序列化，只验证通知偏好到接收人集合的领域语义。
 */
@Tag("unit")
class RealtimeNotificationPreferenceFilterTests {

    /**
     * 验证服务端永久静音会阻止普通消息与 mention 通知事件，但不阻止状态同步事件。
     */
    @Test
    @DisplayName("filter recipients server muted blocks notification events")
    void filterRecipients_serverMuted_blocksNotificationEvents() {
        RecordingNotificationPreferenceRepository repository = new RecordingNotificationPreferenceRepository();
        repository.serverPreference = serverPreference(1001L, "muted", 0L);
        RealtimeNotificationPreferenceFilter filter = new RealtimeNotificationPreferenceFilter(repository, timeProvider());

        assertEquals(List.of(), filter.filterRecipients(9L, "message.created", List.of(1001L)));
        assertEquals(List.of(), filter.filterRecipients(9L, "mention.created", List.of(1001L)));
        assertEquals(List.of(1001L), filter.filterRecipients(9L, "read_state.updated", List.of(1001L)));
    }

    /**
     * 验证频道级静音只阻止对应频道事件，不影响其它频道。
     */
    @Test
    @DisplayName("filter recipients channel muted blocks matching channel")
    void filterRecipients_channelMuted_blocksMatchingChannel() {
        RecordingNotificationPreferenceRepository repository = new RecordingNotificationPreferenceRepository();
        repository.channelPreferences.add(channelPreference(1001L, 9L, "muted", 0L));
        RealtimeNotificationPreferenceFilter filter = new RealtimeNotificationPreferenceFilter(repository, timeProvider());

        assertEquals(List.of(), filter.filterRecipients(9L, "message.created", List.of(1001L)));
        assertEquals(List.of(1001L), filter.filterRecipients(10L, "message.created", List.of(1001L)));
    }

    /**
     * 验证仅提及模式会阻止普通消息但允许 mention 事件。
     */
    @Test
    @DisplayName("filter recipients mentions only allows mention event")
    void filterRecipients_mentionsOnly_allowsMentionEvent() {
        RecordingNotificationPreferenceRepository repository = new RecordingNotificationPreferenceRepository();
        repository.serverPreference = serverPreference(1001L, "mentions_only", 0L);
        RealtimeNotificationPreferenceFilter filter = new RealtimeNotificationPreferenceFilter(repository, timeProvider());

        assertEquals(List.of(), filter.filterRecipients(9L, "message.created", List.of(1001L)));
        assertEquals(List.of(1001L), filter.filterRecipients(9L, "mention.created", List.of(1001L)));
    }

    /**
     * 验证频道级显式 `all` 会覆盖账号级仅提及默认值。
     */
    @Test
    @DisplayName("filter recipients channel all overrides server mentions only")
    void filterRecipients_channelAll_overridesServerMentionsOnly() {
        RecordingNotificationPreferenceRepository repository = new RecordingNotificationPreferenceRepository();
        repository.serverPreference = serverPreference(1001L, "mentions_only", 0L);
        repository.channelPreferences.add(channelPreference(1001L, 9L, "all", 0L));
        RealtimeNotificationPreferenceFilter filter = new RealtimeNotificationPreferenceFilter(repository, timeProvider());

        assertEquals(List.of(1001L), filter.filterRecipients(9L, "message.created", List.of(1001L)));
    }

    /**
     * 验证过期静音不会继续阻止事件。
     */
    @Test
    @DisplayName("filter recipients expired mute allows notification event")
    void filterRecipients_expiredMute_allowsNotificationEvent() {
        RecordingNotificationPreferenceRepository repository = new RecordingNotificationPreferenceRepository();
        repository.serverPreference = serverPreference(1001L, "muted", Instant.parse("2026-04-24T11:59:59Z").toEpochMilli());
        RealtimeNotificationPreferenceFilter filter = new RealtimeNotificationPreferenceFilter(repository, timeProvider());

        assertEquals(List.of(1001L), filter.filterRecipients(9L, "message.created", List.of(1001L)));
    }

    /**
     * 验证结构同步事件不受通知偏好影响。
     */
    @Test
    @DisplayName("filter recipients structural event bypasses notification preference")
    void filterRecipients_structuralEvent_bypassesNotificationPreference() {
        RecordingNotificationPreferenceRepository repository = new RecordingNotificationPreferenceRepository();
        repository.serverPreference = serverPreference(1001L, "muted", 0L);
        RealtimeNotificationPreferenceFilter filter = new RealtimeNotificationPreferenceFilter(repository, timeProvider());

        assertEquals(List.of(1001L), filter.filterRecipients(null, "channels.changed", List.of(1001L)));
        assertEquals(List.of(1001L), filter.filterRecipients(9L, "channel.changed", List.of(1001L)));
    }

    private static TimeProvider timeProvider() {
        return new TimeProvider(Clock.fixed(Instant.parse("2026-04-24T12:00:00Z"), ZoneOffset.UTC));
    }

    private static NotificationServerPreference serverPreference(long accountId, String mode, long mutedUntil) {
        return new NotificationServerPreference(
                accountId,
                mode,
                mutedUntil,
                Instant.parse("2026-04-24T12:00:00Z"),
                Instant.parse("2026-04-24T12:00:00Z")
        );
    }

    private static NotificationChannelPreference channelPreference(long accountId, long channelId, String mode, long mutedUntil) {
        return new NotificationChannelPreference(
                accountId,
                channelId,
                mode,
                mutedUntil,
                Instant.parse("2026-04-24T12:00:00Z"),
                Instant.parse("2026-04-24T12:00:00Z")
        );
    }

    /**
     * `RecordingNotificationPreferenceRepository` 测试替身。
     * 职责：隔离数据库适配器，使过滤器测试只验证偏好解析语义。
     */
    private static final class RecordingNotificationPreferenceRepository implements NotificationPreferenceRepository {
        private NotificationServerPreference serverPreference;
        private final List<NotificationChannelPreference> channelPreferences = new ArrayList<>();

        @Override
        public Optional<NotificationServerPreference> findServerPreferenceByAccountId(long accountId) {
            return Optional.ofNullable(serverPreference)
                    .filter(preference -> preference.accountId() == accountId);
        }

        @Override
        public List<NotificationChannelPreference> listChannelPreferencesByAccountId(long accountId) {
            return channelPreferences.stream()
                    .filter(preference -> preference.accountId() == accountId)
                    .toList();
        }

        @Override
        public NotificationServerPreference upsertServerPreference(NotificationServerPreference preference) {
            this.serverPreference = preference;
            return preference;
        }

        @Override
        public NotificationChannelPreference upsertChannelPreference(NotificationChannelPreference preference) {
            this.channelPreferences.add(preference);
            return preference;
        }
    }
}
