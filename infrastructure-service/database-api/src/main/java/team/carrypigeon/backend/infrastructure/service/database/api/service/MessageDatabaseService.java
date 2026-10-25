package team.carrypigeon.backend.infrastructure.service.database.api.service;

import java.util.List;
import java.util.Optional;

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
     * 按消息 ID 查询消息记录。
     *
     * @param messageId 消息 ID
     * @return 命中时返回消息记录，未命中时返回空
     */
    Optional<MessageRecord> findById(long messageId);

    /**
     * 更新已有消息记录。
     * 边界：只更新 domain 载荷、提及、摘要与状态四列，不改写发送者与频道归属。
     *
     * @param record 待更新消息记录
     */
    void update(MessageRecord record);

    /**
     * 按频道查询游标之前的历史消息。
     * 输出：按消息 ID 倒序返回，最多 limit 条。
     *
     * @param channelId 频道 ID
     * @param cursorMessageId 游标消息 ID，可为空
     * @param limit 查询条数
     * @return 命中时返回消息记录列表
     */
    List<MessageRecord> findByChannelIdBefore(long channelId, Long cursorMessageId, int limit);

    /**
     * 查询频道内指定消息之后的消息。
     * 输出：按消息 ID 升序返回，最多 limit 条。
     *
     * @param channelId 频道 ID
     * @param afterMessageId 锚点消息 ID
     * @param limit 查询条数
     * @return 命中时返回消息记录列表
     */
    List<MessageRecord> findByChannelIdAfter(long channelId, long afterMessageId, int limit);

    /**
     * 在频道内按关键字和高级过滤条件搜索已发送消息。
     * 输出：按消息 ID 倒序返回，最多 limit 条。
     *
     * @param channelId 频道 ID
     * @param keyword 搜索关键字
     * @param cursorMessageId 游标消息 ID，可为空
     * @param senderAccountId 发送者账户 ID，可为空
     * @param domain 消息 domain，可为空
     * @param beforeMessageId 前界消息 ID，可为空
     * @param afterMessageId 后界消息 ID，可为空
     * @param limit 返回条数
     * @return 搜索命中消息记录列表
     */
    List<MessageRecord> searchByChannelId(
            long channelId,
            String keyword,
            Long cursorMessageId,
            Long senderAccountId,
            String domain,
            Long beforeMessageId,
            Long afterMessageId,
            int limit
    );
}
