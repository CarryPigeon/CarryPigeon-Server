package team.carrypigeon.backend.chat.domain.features.server.application.service;

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
import team.carrypigeon.backend.chat.domain.features.server.application.command.UpdateNotificationChannelPreferenceCommand;
import team.carrypigeon.backend.chat.domain.features.server.application.command.UpdateNotificationServerPreferenceCommand;
import team.carrypigeon.backend.chat.domain.features.server.domain.model.NotificationChannelPreference;
import team.carrypigeon.backend.chat.domain.features.server.domain.model.NotificationServerPreference;
import team.carrypigeon.backend.chat.domain.features.server.domain.repository.NotificationPreferenceRepository;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("contract")
class NotificationPreferenceApplicationServiceTests {

    @Test
    @DisplayName("get notification preferences returns defaults when missing")
    void getNotificationPreferences_missingPreferences_returnsDefaults() {
        RecordingNotificationPreferenceRepository repository = new RecordingNotificationPreferenceRepository();
        NotificationPreferenceApplicationService service = new NotificationPreferenceApplicationService(repository, new StubChannelRepository(), new StubChannelMemberRepository(), timeProvider());

        var result = service.getNotificationPreferences(1001L);

        assertEquals("all", result.server().mode());
        assertEquals(0L, result.server().mutedUntil());
        assertEquals(0, result.channels().size());
    }

    @Test
    @DisplayName("update server preference stores normalized preference")
    void updateServerPreference_storesNormalizedPreference() {
        RecordingNotificationPreferenceRepository repository = new RecordingNotificationPreferenceRepository();
        NotificationPreferenceApplicationService service = new NotificationPreferenceApplicationService(repository, new StubChannelRepository(), new StubChannelMemberRepository(), timeProvider());

        service.updateServerPreference(new UpdateNotificationServerPreferenceCommand(1001L, "muted", 0L));

        assertEquals("muted", repository.serverPreference.mode());
        assertEquals(0L, repository.serverPreference.mutedUntil());
    }

    @Test
    @DisplayName("update channel preference non member throws forbidden")
    void updateChannelPreference_nonMember_throwsForbidden() {
        RecordingNotificationPreferenceRepository repository = new RecordingNotificationPreferenceRepository();
        NotificationPreferenceApplicationService service = new NotificationPreferenceApplicationService(repository, new StubChannelRepository(), new NonMemberChannelMemberRepository(), timeProvider());

        ProblemException exception = assertThrows(ProblemException.class, () -> service.updateChannelPreference(new UpdateNotificationChannelPreferenceCommand(1001L, 9L, "inherit", 0L)));

        assertEquals("not_channel_member", exception.reason());
    }

    private static TimeProvider timeProvider() {
        return new TimeProvider(Clock.fixed(Instant.parse("2026-04-24T12:00:00Z"), ZoneOffset.UTC));
    }

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

    private static final class StubChannelRepository implements ChannelRepository {
        @Override public Optional<Channel> findDefaultChannel() { return Optional.empty(); }
        @Override public Optional<Channel> findSystemChannel() { return Optional.empty(); }
        @Override public Optional<Channel> findById(long channelId) { return Optional.of(new Channel(channelId, channelId, "general", "", "", "1001", "public", false, Instant.parse("2026-04-24T12:00:00Z"), Instant.parse("2026-04-24T12:00:00Z"))); }
    }

    private static final class StubChannelMemberRepository implements ChannelMemberRepository {
        @Override public boolean exists(long channelId, long accountId) { return true; }
        @Override public void save(team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMember channelMember) { }
        @Override public Optional<team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMember> findByChannelIdAndAccountId(long channelId, long accountId) { return Optional.empty(); }
        @Override public List<team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMember> findByChannelId(long channelId) { return List.of(); }
        @Override public List<Long> findAccountIdsByChannelId(long channelId) { return List.of(); }
    }

    private static final class NonMemberChannelMemberRepository implements ChannelMemberRepository {
        @Override public boolean exists(long channelId, long accountId) { return false; }
        @Override public void save(team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMember channelMember) { }
        @Override public Optional<team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMember> findByChannelIdAndAccountId(long channelId, long accountId) { return Optional.empty(); }
        @Override public List<team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMember> findByChannelId(long channelId) { return List.of(); }
        @Override public List<Long> findAccountIdsByChannelId(long channelId) { return List.of(); }
    }
}
