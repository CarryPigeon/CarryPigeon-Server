package team.carrypigeon.backend.chat.domain.features.message.support.persistence;

import java.util.List;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MessageRepository;
import team.carrypigeon.backend.infrastructure.service.database.api.model.MessageRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.MessageDatabaseService;

/**
 * 基于 database-api 的消息仓储适配器。
 * 职责：在 message feature 内完成领域消息与数据库持久化投影之间的转换。
 * 边界：不包含 SQL 与数据库驱动细节，也不把 database-api 契约直接提升为领域模型。
 */
public class DatabaseBackedMessageRepository implements MessageRepository {

    private final MessageDatabaseService messageDatabaseService;

    public DatabaseBackedMessageRepository(MessageDatabaseService messageDatabaseService) {
        this.messageDatabaseService = messageDatabaseService;
    }

    /**
     * 持久化一条新消息。
     * 输入：已完成业务校验和消息构建的领域消息。
     * 输出：返回原领域对象，保持上层调用链继续使用同一语义对象。
     */
    @Override
    public ChannelMessage save(ChannelMessage message) {
        messageDatabaseService.insert(toPersistenceRecord(message));
        return message;
    }

    /**
     * 按消息 ID 查询单条消息。
     * 输出：存在时返回领域消息，不存在时返回空。
     */
    @Override
    public java.util.Optional<ChannelMessage> findById(long messageId) {
        return messageDatabaseService.findById(messageId)
                .map(this::toDomainMessage);
    }

    /**
     * 覆盖更新消息的持久化投影。
     * 副作用：会把编辑后的领域消息写回数据库。
     */
    @Override
    public ChannelMessage update(ChannelMessage message) {
        messageDatabaseService.update(toPersistenceRecord(message));
        return message;
    }

    /**
     * 删除指定消息的持久化记录。
     * 边界：这里只负责数据删除，不定义撤回或审计规则。
     */
    @Override
    public void delete(long messageId) {
        messageDatabaseService.delete(messageId);
    }

    /**
     * 查询频道内早于游标的历史消息。
     * 输出：返回已转换为领域模型的消息列表。
     */
    @Override
    public List<ChannelMessage> findByChannelIdBefore(long channelId, Long cursorMessageId, int limit) {
        return messageDatabaseService.findByChannelIdBefore(channelId, cursorMessageId, limit)
                .stream()
                .map(this::toDomainMessage)
                .toList();
    }

    /**
     * 查询频道内晚于锚点消息的增量消息。
     * 原因：用于客户端补齐断连后的消息窗口。
     */
    @Override
    public List<ChannelMessage> findByChannelIdAfter(long channelId, long afterMessageId, int limit) {
        return messageDatabaseService.findByChannelIdAfter(channelId, afterMessageId, limit)
                .stream()
                .map(this::toDomainMessage)
                .toList();
    }

    /**
     * 在单个频道内执行关键字搜索。
     * 约束：搜索语义由底层 database-api 实现决定，这里只做模型转换。
     */
    @Override
    public List<ChannelMessage> searchByChannelId(long channelId, String keyword, int limit) {
        return messageDatabaseService.searchByChannelId(channelId, keyword, limit)
                .stream()
                .map(this::toDomainMessage)
                .toList();
    }

    /**
     * 按复合过滤条件搜索频道消息。
     * 输入：关键字、发送者、领域类型和前后消息锚点等过滤项。
     * 输出：符合条件的领域消息集合。
     */
    @Override
    public List<ChannelMessage> searchByChannelId(
            long channelId,
            String keyword,
            Long cursorMessageId,
            Long senderAccountId,
            String domain,
            Long beforeMessageId,
            Long afterMessageId,
            int limit
    ) {
        return messageDatabaseService.searchByChannelId(
                        channelId,
                        keyword,
                        cursorMessageId,
                        senderAccountId,
                        domain,
                        beforeMessageId,
                        afterMessageId,
                        limit
                ).stream()
                .map(this::toDomainMessage)
                .toList();
    }

    private ChannelMessage toDomainMessage(MessageRecord record) {
        return new ChannelMessage(
                record.messageId(),
                record.serverId(),
                record.conversationId(),
                record.channelId(),
                record.senderId(),
                record.messageType(),
                record.body(),
                record.previewText(),
                record.searchableText(),
                record.payload(),
                record.metadata(),
                record.mentions(),
                record.forwardedFrom(),
                record.status(),
                record.createdAt(),
                record.editedAt(),
                record.editVersion()
        );
    }

    private MessageRecord toPersistenceRecord(ChannelMessage message) {
        return new MessageRecord(
                message.messageId(),
                message.serverId(),
                message.conversationId(),
                message.channelId(),
                message.senderId(),
                message.messageType(),
                message.body(),
                message.previewText(),
                message.searchableText(),
                message.payload(),
                message.metadata(),
                message.mentions(),
                message.forwardedFrom(),
                message.status(),
                message.createdAt(),
                message.editedAt(),
                message.editVersion()
        );
    }
}
