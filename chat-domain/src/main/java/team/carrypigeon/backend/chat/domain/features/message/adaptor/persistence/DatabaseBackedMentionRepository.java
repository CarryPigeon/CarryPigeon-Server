package team.carrypigeon.backend.chat.domain.features.message.adaptor.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
@ConditionalOnProperty(prefix = "cp.infrastructure.service.database", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DatabaseBackedMentionRepository implements MentionRepository {

    private static final int ALL_ROWS = Integer.MAX_VALUE;

    private final MentionDatabaseService mentionDatabaseService;

    public DatabaseBackedMentionRepository(MentionDatabaseService mentionDatabaseService) {
        this.mentionDatabaseService = mentionDatabaseService;
    }

    /**
     * 批量写入消息产生的提及记录。
     * 副作用：一次适配调用持久化提及的来源消息、目标账户和已读状态。
     */
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

    /**
     * 删除指定消息产生的提及记录。
     */
    @Override
    public void deleteByMessageId(long messageId) {
        mentionDatabaseService.deleteByMessageId(messageId);
    }

    /**
     * 查询账户的提及流。
     * 输入：账户、游标、未读过滤和可选频道范围。
     * 输出：已转换为领域模型的提及列表。
     */
    @Override
    public List<Mention> listByAccountId(long accountId, Long cursorMentionId, int limit, boolean unreadOnly, Long channelId) {
        Boolean readFilter = unreadOnly ? Boolean.FALSE : null;
        return mentionDatabaseService.find(new MentionQuery(accountId, cursorMentionId, readFilter, channelId, limit)).stream()
                .map(this::toDomain)
                .toList();
    }

    /**
     * 将单条提及标记为已读。
     * 输出：返回目标提及是否存在且归属于当前账户。
     */
    @Override
    public boolean markAsRead(long accountId, long mentionId) {
        Optional<MentionRecord> existing = mentionDatabaseService.findById(mentionId);
        if (existing.isEmpty() || existing.get().targetAccountId() != accountId) {
            return false;
        }
        MentionRecord record = existing.get();
        if (record.read()) {
            return true;
        }
        mentionDatabaseService.update(new MentionRecord(
                record.mentionId(),
                record.channelId(),
                record.messageId(),
                record.fromAccountId(),
                record.targetType(),
                record.targetAccountId(),
                record.createdAt(),
                true
        ));
        return true;
    }

    /**
     * 批量标记账户提及为已读。
     * 输入：账户、可选上界游标和可选频道范围。
     * 输出：返回实际更新的提及数量。
     */
    @Override
    public int markAllAsRead(long accountId, Long beforeMentionId, Long channelId) {
        List<MentionRecord> records = mentionDatabaseService.find(
                new MentionQuery(accountId, beforeMentionId, Boolean.FALSE, channelId, ALL_ROWS)
        );
        if (records.isEmpty()) {
            return 0;
        }
        mentionDatabaseService.updateAll(records.stream()
                .map(record -> new MentionRecord(
                        record.mentionId(),
                        record.channelId(),
                        record.messageId(),
                        record.fromAccountId(),
                        record.targetType(),
                        record.targetAccountId(),
                        record.createdAt(),
                        true
                ))
                .toList());
        return records.size();
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
