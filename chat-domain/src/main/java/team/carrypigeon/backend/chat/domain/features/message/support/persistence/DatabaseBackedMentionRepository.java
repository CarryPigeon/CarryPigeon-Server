package team.carrypigeon.backend.chat.domain.features.message.support.persistence;

import java.util.List;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.Mention;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MentionRepository;
import team.carrypigeon.backend.infrastructure.service.database.api.service.MentionDatabaseService;

/**
 * 基于 database-api 的提及仓储适配器。
 * 职责：在 message feature 内桥接领域 mention 模型与 database-api 记录契约。
 * 边界：不定义 mention 解析规则，只负责已生成提及结果的持久化与读取。
 */
public class DatabaseBackedMentionRepository implements MentionRepository {

    private final MentionDatabaseService mentionDatabaseService;

    public DatabaseBackedMentionRepository(MentionDatabaseService mentionDatabaseService) {
        this.mentionDatabaseService = mentionDatabaseService;
    }

    /**
     * 写入一条新的提及记录。
     * 副作用：会持久化提及的来源消息、目标账户和已读状态。
     */
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

    /**
     * 查询账户的提及流。
     * 输入：账户、游标、未读过滤和可选频道范围。
     * 输出：已转换为领域模型的提及列表。
     */
    @Override
    public List<Mention> listByAccountId(long accountId, Long cursorMentionId, int limit, boolean unreadOnly, Long channelId) {
        return mentionDatabaseService.listByAccountId(accountId, cursorMentionId, limit, unreadOnly, channelId).stream()
                .map(record -> new Mention(record.mentionId(), record.channelId(), record.messageId(), record.fromAccountId(), record.targetType(), record.targetAccountId(), record.createdAt(), record.read()))
                .toList();
    }

    /**
     * 将单条提及标记为已读。
     * 输出：返回是否真的发生了状态变更。
     */
    @Override
    public boolean markAsRead(long accountId, long mentionId) {
        return mentionDatabaseService.markAsRead(accountId, mentionId);
    }

    /**
     * 批量标记账户提及为已读。
     * 输入：账户、可选上界游标和可选频道范围。
     * 输出：返回实际更新的提及数量。
     */
    @Override
    public int markAllAsRead(long accountId, Long beforeMentionId, Long channelId) {
        return mentionDatabaseService.markAllAsRead(accountId, beforeMentionId, channelId);
    }
}
