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

    /** 直接更新当前账号匹配的未读集合，ID 上界排他；返回本次实际变为已读的条数，重复执行返回 0。 */
    int markAllAsRead(long accountId, Long beforeMentionId, Long channelId);
}
