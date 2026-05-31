package team.carrypigeon.backend.chat.domain.features.message.support.persistence;

import java.util.List;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.Mention;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MentionRepository;
import team.carrypigeon.backend.infrastructure.service.database.api.service.MentionDatabaseService;

/**
 * 基于 database-api 的提及仓储适配器。
 */
public class DatabaseBackedMentionRepository implements MentionRepository {

    private final MentionDatabaseService mentionDatabaseService;

    public DatabaseBackedMentionRepository(MentionDatabaseService mentionDatabaseService) {
        this.mentionDatabaseService = mentionDatabaseService;
    }

    @Override
    public void save(Mention mention) {
        mentionDatabaseService.insert(new team.carrypigeon.backend.infrastructure.service.database.api.model.MentionRecord(
                mention.mentionId(),
                mention.channelId(),
                mention.messageId(),
                mention.fromAccountId(),
                mention.targetType(),
                mention.targetAccountId(),
                mention.createdAt(),
                mention.read()
        ));
    }

    @Override
    public List<Mention> listByAccountId(long accountId, Long cursorMentionId, int limit, boolean unreadOnly, Long channelId) {
        return mentionDatabaseService.listByAccountId(accountId, cursorMentionId, limit, unreadOnly, channelId).stream()
                .map(record -> new Mention(record.mentionId(), record.channelId(), record.messageId(), record.fromAccountId(), record.targetType(), record.targetAccountId(), record.createdAt(), record.read()))
                .toList();
    }

    @Override
    public boolean markAsRead(long accountId, long mentionId) {
        return mentionDatabaseService.markAsRead(accountId, mentionId);
    }

    @Override
    public int markAllAsRead(long accountId, Long beforeMentionId, Long channelId) {
        return mentionDatabaseService.markAllAsRead(accountId, beforeMentionId, channelId);
    }
}
