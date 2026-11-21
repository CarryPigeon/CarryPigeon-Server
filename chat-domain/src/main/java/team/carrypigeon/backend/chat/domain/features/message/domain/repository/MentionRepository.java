package team.carrypigeon.backend.chat.domain.features.message.domain.repository;

import java.util.List;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.Mention;

/**
 * 提及仓储抽象。
 */
public interface MentionRepository {

    /** 删除频道下的全部提及记录。 */
    void deleteByChannelId(long channelId);

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

    /** 只返回收件人当前可见的记录，查询结果无顺序保证。 */
    List<Mention> findVisibleByIds(long accountId, List<Long> mentionIds);

    /** 单条幂等已读；本人可见且已读时也返回 true。 */
    boolean markAsRead(long accountId, long mentionId);

    /** 直接更新当前账号匹配的未读集合，包含 ID 上界；返回本次实际变为已读的条数，重复执行返回 0。 */
    int markAllAsRead(long accountId, Long throughMentionId, Long channelId);

    /** 清除离开频道的接收人全部提及，包括已读记录。 */
    void deleteByChannelAndRecipient(long channelId, long accountId);
}
