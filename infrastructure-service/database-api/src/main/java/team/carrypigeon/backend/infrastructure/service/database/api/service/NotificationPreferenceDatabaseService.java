package team.carrypigeon.backend.infrastructure.service.database.api.service;

import java.util.List;
import java.util.Optional;
import team.carrypigeon.backend.infrastructure.service.database.api.model.NotificationChannelPreferenceRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.model.NotificationServerPreferenceRecord;

/**
 * 通知偏好数据库服务抽象。
 */
public interface NotificationPreferenceDatabaseService {

    Optional<NotificationServerPreferenceRecord> findServerPreferenceByAccountId(long accountId);

    List<NotificationChannelPreferenceRecord> listChannelPreferencesByAccountId(long accountId);

    void upsertServerPreference(NotificationServerPreferenceRecord record);

    void upsertChannelPreference(NotificationChannelPreferenceRecord record);
}
