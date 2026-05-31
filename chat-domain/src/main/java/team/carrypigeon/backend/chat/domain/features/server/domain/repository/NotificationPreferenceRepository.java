package team.carrypigeon.backend.chat.domain.features.server.domain.repository;

import java.util.List;
import java.util.Optional;
import team.carrypigeon.backend.chat.domain.features.server.domain.model.NotificationChannelPreference;
import team.carrypigeon.backend.chat.domain.features.server.domain.model.NotificationServerPreference;

/**
 * 通知偏好仓储抽象。
 */
public interface NotificationPreferenceRepository {

    Optional<NotificationServerPreference> findServerPreferenceByAccountId(long accountId);

    List<NotificationChannelPreference> listChannelPreferencesByAccountId(long accountId);

    NotificationServerPreference upsertServerPreference(NotificationServerPreference preference);

    NotificationChannelPreference upsertChannelPreference(NotificationChannelPreference preference);
}
