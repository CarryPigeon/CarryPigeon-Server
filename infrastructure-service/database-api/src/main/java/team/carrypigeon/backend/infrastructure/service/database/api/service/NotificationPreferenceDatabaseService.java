package team.carrypigeon.backend.infrastructure.service.database.api.service;

import java.util.List;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import team.carrypigeon.backend.infrastructure.service.database.api.model.NotificationChannelPreferenceRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.model.NotificationServerPreferenceRecord;

/**
 * 通知偏好数据库服务抽象。
 */
public interface NotificationPreferenceDatabaseService {

    Optional<NotificationServerPreferenceRecord> findServerPreferenceByAccountId(long accountId);

    List<NotificationChannelPreferenceRecord> listChannelPreferencesByAccountId(long accountId);

    /** 精确读取账户与频道复合键对应的偏好。 */
    Optional<NotificationChannelPreferenceRecord> findChannelPreference(long accountId, long channelId);

    /** 批量读取账户级偏好，避免实时广播按接收人逐个查询。 */
    Map<Long, NotificationServerPreferenceRecord> findServerPreferencesByAccountIds(Collection<Long> accountIds);

    /** 批量读取频道级偏好，返回按账户分组的结果。 */
    Map<Long, List<NotificationChannelPreferenceRecord>> listChannelPreferencesByAccountIds(Collection<Long> accountIds);

    void upsertServerPreference(NotificationServerPreferenceRecord record);

    void upsertChannelPreference(NotificationChannelPreferenceRecord record);
}
