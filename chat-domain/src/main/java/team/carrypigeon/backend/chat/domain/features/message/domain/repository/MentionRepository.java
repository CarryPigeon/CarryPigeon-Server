package team.carrypigeon.backend.chat.domain.features.message.domain.repository;

import java.util.List;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.Mention;

/**
 * 提及仓储抽象。
 */
public interface MentionRepository {

    /**
     * 批量保存消息产生的提及记录。
     *
     * @param mentions 按消息声明顺序生成的提及记录
     */
    void saveAll(List<Mention> mentions);

    /**
     * 删除指定消息产生的提及记录。
     *
     * @param messageId 消息 ID
     */
    void deleteByMessageId(long messageId);

    List<Mention> listByAccountId(long accountId, Long cursorMentionId, int limit, boolean unreadOnly, Long channelId);

    boolean markAsRead(long accountId, long mentionId);

    int markAllAsRead(long accountId, Long beforeMentionId, Long channelId);
}
