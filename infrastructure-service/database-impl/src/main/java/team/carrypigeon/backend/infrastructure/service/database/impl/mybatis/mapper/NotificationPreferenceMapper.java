package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity.NotificationChannelPreferenceEntity;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity.NotificationServerPreferenceEntity;

/**
 * 通知偏好 Mapper。
 */
@Mapper
public interface NotificationPreferenceMapper {

    @Select("""
            SELECT account_id, mode, muted_until, created_at, updated_at
            FROM chat_notification_server_preference
            WHERE account_id = #{accountId}
            LIMIT 1
            """)
    NotificationServerPreferenceEntity findServerPreferenceByAccountId(@Param("accountId") long accountId);

    @Select("""
            SELECT account_id, channel_id, mode, muted_until, created_at, updated_at
            FROM chat_notification_channel_preference
            WHERE account_id = #{accountId}
            ORDER BY channel_id ASC
            """)
    List<NotificationChannelPreferenceEntity> listChannelPreferencesByAccountId(@Param("accountId") long accountId);

    @Insert("""
            INSERT INTO chat_notification_server_preference (
                account_id, mode, muted_until, created_at, updated_at
            ) VALUES (
                #{accountId}, #{mode}, #{mutedUntil}, #{createdAt}, #{updatedAt}
            )
            ON DUPLICATE KEY UPDATE
                mode = VALUES(mode),
                muted_until = VALUES(muted_until),
                updated_at = VALUES(updated_at)
            """)
    int upsertServerPreference(NotificationServerPreferenceEntity entity);

    @Insert("""
            INSERT INTO chat_notification_channel_preference (
                account_id, channel_id, mode, muted_until, created_at, updated_at
            ) VALUES (
                #{accountId}, #{channelId}, #{mode}, #{mutedUntil}, #{createdAt}, #{updatedAt}
            )
            ON DUPLICATE KEY UPDATE
                mode = VALUES(mode),
                muted_until = VALUES(muted_until),
                updated_at = VALUES(updated_at)
            """)
    int upsertChannelPreference(NotificationChannelPreferenceEntity entity);
}
