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
 * 职责：在 server feature 内完成通知偏好领域模型与 database-api 记录模型转换。
 * 边界：不定义通知模式规则，只负责偏好读写的边界适配。
 */
public class DatabaseBackedNotificationPreferenceRepository implements NotificationPreferenceRepository {

    private final NotificationPreferenceDatabaseService notificationPreferenceDatabaseService;

    public DatabaseBackedNotificationPreferenceRepository(NotificationPreferenceDatabaseService notificationPreferenceDatabaseService) {
        this.notificationPreferenceDatabaseService = notificationPreferenceDatabaseService;
    }

    /**
     * 查询账户的服务级通知偏好。
     * 输出：未配置时返回空。
     */
    @Override
    public Optional<NotificationServerPreference> findServerPreferenceByAccountId(long accountId) {
        return notificationPreferenceDatabaseService.findServerPreferenceByAccountId(accountId).map(this::toDomain);
    }

    /**
     * 查询账户的频道级通知偏好列表。
     */
    @Override
    public List<NotificationChannelPreference> listChannelPreferencesByAccountId(long accountId) {
        return notificationPreferenceDatabaseService.listChannelPreferencesByAccountId(accountId).stream().map(this::toDomain).toList();
    }

    /**
     * 新增或覆盖服务级通知偏好。
     * 输出：返回调用方传入的领域对象，保持上层语义一致。
     */
    @Override
    public NotificationServerPreference upsertServerPreference(NotificationServerPreference preference) {
        notificationPreferenceDatabaseService.upsertServerPreference(toRecord(preference));
        return preference;
    }

    /**
     * 新增或覆盖频道级通知偏好。
     */
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
