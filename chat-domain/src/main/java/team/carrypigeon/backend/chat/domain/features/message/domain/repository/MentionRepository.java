package team.carrypigeon.backend.chat.domain.features.message.domain.repository;

import java.util.List;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.Mention;

/**
 * 提及仓储抽象。
 */
public interface MentionRepository {

    void save(Mention mention);

    List<Mention> listByAccountId(long accountId, Long cursorMentionId, int limit, boolean unreadOnly, Long channelId);

    boolean markAsRead(long accountId, long mentionId);

    int markAllAsRead(long accountId, Long beforeMentionId, Long channelId);
}
