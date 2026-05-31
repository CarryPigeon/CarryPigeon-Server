package team.carrypigeon.backend.chat.domain.features.server.support.persistence;

import java.util.List;
import java.util.Optional;
import team.carrypigeon.backend.chat.domain.features.server.domain.model.NotificationChannelPreference;
import team.carrypigeon.backend.chat.domain.features.server.domain.model.NotificationServerPreference;
import team.carrypigeon.backend.chat.domain.features.server.domain.repository.NotificationPreferenceRepository;
import team.carrypigeon.backend.infrastructure.service.database.api.model.NotificationChannelPreferenceRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.model.NotificationServerPreferenceRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.NotificationPreferenceDatabaseService;

/**
 * 基于 database-api 的通知偏好仓储适配器。
 */
public class DatabaseBackedNotificationPreferenceRepository implements NotificationPreferenceRepository {

    private final NotificationPreferenceDatabaseService notificationPreferenceDatabaseService;

    public DatabaseBackedNotificationPreferenceRepository(NotificationPreferenceDatabaseService notificationPreferenceDatabaseService) {
        this.notificationPreferenceDatabaseService = notificationPreferenceDatabaseService;
    }

    @Override
    public Optional<NotificationServerPreference> findServerPreferenceByAccountId(long accountId) {
        return notificationPreferenceDatabaseService.findServerPreferenceByAccountId(accountId).map(this::toDomain);
    }

    @Override
    public List<NotificationChannelPreference> listChannelPreferencesByAccountId(long accountId) {
        return notificationPreferenceDatabaseService.listChannelPreferencesByAccountId(accountId).stream().map(this::toDomain).toList();
    }

    @Override
    public NotificationServerPreference upsertServerPreference(NotificationServerPreference preference) {
        notificationPreferenceDatabaseService.upsertServerPreference(toRecord(preference));
        return preference;
    }

    @Override
    public NotificationChannelPreference upsertChannelPreference(NotificationChannelPreference preference) {
        notificationPreferenceDatabaseService.upsertChannelPreference(toRecord(preference));
        return preference;
    }

    private NotificationServerPreference toDomain(NotificationServerPreferenceRecord record) {
        return new NotificationServerPreference(record.accountId(), record.mode(), record.mutedUntil(), record.createdAt(), record.updatedAt());
    }

    private NotificationChannelPreference toDomain(NotificationChannelPreferenceRecord record) {
        return new NotificationChannelPreference(record.accountId(), record.channelId(), record.mode(), record.mutedUntil(), record.createdAt(), record.updatedAt());
    }

    private NotificationServerPreferenceRecord toRecord(NotificationServerPreference preference) {
        return new NotificationServerPreferenceRecord(preference.accountId(), preference.mode(), preference.mutedUntil(), preference.createdAt(), preference.updatedAt());
    }

    private NotificationChannelPreferenceRecord toRecord(NotificationChannelPreference preference) {
        return new NotificationChannelPreferenceRecord(preference.accountId(), preference.channelId(), preference.mode(), preference.mutedUntil(), preference.createdAt(), preference.updatedAt());
    }
}
