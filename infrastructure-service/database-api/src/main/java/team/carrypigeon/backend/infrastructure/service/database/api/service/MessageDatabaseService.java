package team.carrypigeon.backend.infrastructure.service.database.api.service;

import java.util.List;
import team.carrypigeon.backend.infrastructure.service.database.api.model.MessageRecord;

/**
 * 消息数据库服务抽象。
 * 职责：向 chat-domain 提供消息最小读写能力。
 * 边界：不暴露 JDBC、SQL 或具体数据库框架细节。
 */
public interface MessageDatabaseService {

    /**
     * 写入新的消息记录。
     *
     * @param record 待持久化消息记录
     */
    void insert(MessageRecord record);

    /**
     * 按频道查询游标之前的历史消息。
     *
     * @param channelId 频道 ID
     * @param cursorMessageId 游标消息 ID，可为空
     * @param limit 查询条数
     * @return 历史消息记录列表，按 messageId 倒序排列
     */
    List<MessageRecord> findByChannelIdBefore(long channelId, Long cursorMessageId, int limit);

    /**
     * 在频道内按关键字搜索消息。
     *
     * @param channelId 频道 ID
     * @param keyword 搜索关键字
     * @param limit 返回条数
     * @return 搜索命中消息列表
     */
    List<MessageRecord> searchByChannelId(long channelId, String keyword, int limit);
}
