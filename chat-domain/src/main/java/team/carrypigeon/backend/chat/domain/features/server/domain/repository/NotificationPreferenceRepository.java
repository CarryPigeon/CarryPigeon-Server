package team.carrypigeon.backend.chat.domain.features.server.domain.repository;

import java.util.List;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import team.carrypigeon.backend.chat.domain.features.server.domain.model.NotificationChannelPreference;
import team.carrypigeon.backend.chat.domain.features.server.domain.model.NotificationServerPreference;

/**
 * 通知偏好仓储抽象。
 */
public interface NotificationPreferenceRepository {

    Optional<NotificationServerPreference> findServerPreferenceByAccountId(long accountId);

    List<NotificationChannelPreference> listChannelPreferencesByAccountId(long accountId);

    /**
     * 精确查询账户在指定频道的通知偏好。
     *
     * @param accountId 账户 ID
     * @param channelId 频道 ID
     * @return 命中时返回频道偏好
     */
    Optional<NotificationChannelPreference> findChannelPreference(long accountId, long channelId);

    default Map<Long, NotificationServerPreference> findServerPreferencesByAccountIds(Collection<Long> accountIds) {
        return accountIds.stream()
                .map(id -> findServerPreferenceByAccountId(id).map(preference -> java.util.Map.entry(id, preference)))
                .flatMap(Optional::stream)
                .collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    default Map<Long, List<NotificationChannelPreference>> listChannelPreferencesByAccountIds(Collection<Long> accountIds) {
        return accountIds.stream().collect(java.util.stream.Collectors.toMap(
                id -> id, this::listChannelPreferencesByAccountId, (left, right) -> left));
    }

    NotificationServerPreference upsertServerPreference(NotificationServerPreference preference);

    NotificationChannelPreference upsertChannelPreference(NotificationChannelPreference preference);
}
