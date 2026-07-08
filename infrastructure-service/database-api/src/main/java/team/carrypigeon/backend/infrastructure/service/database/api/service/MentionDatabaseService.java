package team.carrypigeon.backend.infrastructure.service.database.api.service;

import java.util.List;
import team.carrypigeon.backend.infrastructure.service.database.api.model.MentionRecord;

/**
 * 提及数据库服务抽象。
 */
public interface MentionDatabaseService {

    void insert(MentionRecord record);

    default void deleteByMessageId(long messageId) {
        throw new UnsupportedOperationException("mention delete by message is not supported");
    }

    List<MentionRecord> listByAccountId(long accountId, Long cursorMentionId, int limit, boolean unreadOnly, Long channelId);

    boolean markAsRead(long accountId, long mentionId);

    int markAllAsRead(long accountId, Long beforeMentionId, Long channelId);
}
