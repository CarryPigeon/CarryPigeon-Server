package team.carrypigeon.backend.infrastructure.service.database.api.service;

import java.util.List;
import team.carrypigeon.backend.infrastructure.service.database.api.model.MentionRecord;

/**
 * 提及数据库服务抽象。
 */
public interface MentionDatabaseService {

    /**
     * 批量写入提及记录；空集合不产生数据库访问。
     *
     * @param records 待写入记录
     */
    void insertAll(List<MentionRecord> records);

    void deleteByMessageId(long messageId);

    List<MentionRecord> listByAccountId(long accountId, Long cursorMentionId, int limit, boolean unreadOnly, Long channelId);

    boolean markAsRead(long accountId, long mentionId);

    int markAllAsRead(long accountId, Long beforeMentionId, Long channelId);
}
