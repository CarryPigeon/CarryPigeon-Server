package team.carrypigeon.backend.chat.domain.features.message.adaptor.persistence;

import java.util.List;

import org.springframework.stereotype.Component;

import team.carrypigeon.backend.chat.domain.features.message.domain.model.Mention;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MentionRepository;
import team.carrypigeon.backend.infrastructure.service.database.api.model.MentionQuery;
import team.carrypigeon.backend.infrastructure.service.database.api.model.MentionRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.MentionDatabaseService;

/**
 * 基于 database-api 的提及仓储适配器。
 * 职责：在 message feature 内桥接领域 mention 模型与 database-api 记录契约，并编排已读状态流转。
 * 边界：不定义 mention 解析规则，只负责已生成提及结果的持久化与读取。
 */
@Component
public class DatabaseBackedMentionRepository implements MentionRepository {

    private final MentionDatabaseService mentionDatabaseService;

    public DatabaseBackedMentionRepository(MentionDatabaseService mentionDatabaseService) {
        this.mentionDatabaseService = mentionDatabaseService;
    }

    @Override
    public void deleteByChannelId(long channelId) {
        mentionDatabaseService.deleteByChannelId(channelId);
    }

    @Override
    public void saveAll(List<Mention> mentions) {
        mentionDatabaseService.insertAll(mentions.stream()
                .map(mention -> new MentionRecord(
                        mention.mentionId(),
                        mention.channelId(),
                        mention.messageId(),
                        mention.fromAccountId(),
                        mention.targetType(),
                        mention.targetAccountId(),
                        mention.createdAt(),
                        mention.read()
                ))
                .toList());
    }

    @Override
    public void deleteByMessageId(long messageId) {
        mentionDatabaseService.deleteByMessageId(messageId);
    }

    @Override
    public List<Mention> listByAccountId(long accountId, Long cursorMentionId, int limit, boolean unreadOnly, Long channelId) {
        // 查询全部时省略已读条件，保留收件人和频道可见性条件。
        Boolean readFilter = unreadOnly ? Boolean.FALSE : null;
        return mentionDatabaseService.find(new MentionQuery(accountId, cursorMentionId, readFilter, channelId, limit)).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<Mention> findVisibleByIds(long accountId, List<Long> mentionIds) {
        return mentionDatabaseService.findVisibleByIds(accountId, mentionIds).stream().map(this::toDomain).toList();
    }

    @Override
    public boolean markAsRead(long accountId, long mentionId) {
        return mentionDatabaseService.markAsRead(accountId, mentionId);
    }

    @Override
    public int markAllAsRead(long accountId, Long throughMentionId, Long channelId) {
        return mentionDatabaseService.markAllAsRead(accountId, throughMentionId, channelId);
    }

    private Mention toDomain(MentionRecord record) {
        return new Mention(
                record.mentionId(),
                record.channelId(),
                record.messageId(),
                record.fromAccountId(),
                record.targetType(),
                record.targetAccountId(),
                record.createdAt(),
                record.read()
        );
    }
}
