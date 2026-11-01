package team.carrypigeon.backend.infrastructure.service.database.api.service;

import java.util.List;
import java.util.Optional;

import team.carrypigeon.backend.infrastructure.basic.page.PageQuery;
import team.carrypigeon.backend.infrastructure.basic.page.PageResult;
import team.carrypigeon.backend.infrastructure.service.database.api.model.MessageRecord;

/**
 * 消息数据库服务抽象。
 * 职责：提供消息表级通用元能力、方向查询与搜索例外。
 * 边界：不承载消息业务规则；查询方向由调用方法固定，搜索条件由调用方传入。
 */
public interface MessageDatabaseService {

    /**
     * 写入新的消息记录。
     *
     * @param record 待持久化消息记录
     */
    void insert(MessageRecord record);

    /**
     * 批量写入消息记录。
     *
     * @param records 待持久化消息记录
     */
    void insertAll(List<MessageRecord> records);

    /**
     * 按消息 ID 删除消息记录。
     *
     * @param messageId 消息 ID
     */
    void delete(long messageId);

    /**
     * 按消息 ID 批量删除消息记录。
     *
     * @param messageIds 消息 ID 列表
     */
    void deleteAll(List<Long> messageIds);

    /**
     * 更新已有消息记录。
     * 边界：更新完整行；哪些字段允许变化由 Repository 决定。
     *
     * @param record 待更新消息记录
     */
    void update(MessageRecord record);

    /**
     * 分页查询消息记录。
     * 说明：按消息 ID 升序返回，仅供离线维护与元能力完整性使用；业务查询使用方向查询或专用搜索。
     *
     * @param page 分页参数
     * @return 分页消息记录
     */
    PageResult<MessageRecord> list(PageQuery page);

    /**
     * 按消息 ID 查询消息记录。
     *
     * @param messageId 消息 ID
     * @return 命中时返回消息记录，未命中时返回空
     */
    Optional<MessageRecord> findById(long messageId);

    /**
     * 按消息 ID 批量查询消息记录。
     *
     * @param messageIds 消息 ID 列表
     * @return 命中的消息记录列表
     */
    List<MessageRecord> findByIds(List<Long> messageIds);

    /**
     * 查询频道最新消息。
     * 输出：按消息 ID 倒序返回，最多 limit 条。
     *
     * @param channelId 频道 ID
     * @param limit 最大返回条数
     * @return 最新消息记录列表
     */
    List<MessageRecord> findLatestByChannelId(long channelId, int limit);

    /**
     * 查询频道内指定消息之前的更旧消息。
     * 输出：按消息 ID 倒序返回，最多 limit 条。
     *
     * @param channelId 频道 ID
     * @param cursorMessageId 排他游标消息 ID
     * @param limit 最大返回条数
     * @return 更旧消息记录列表
     */
    List<MessageRecord> findBeforeByChannelId(long channelId, long cursorMessageId, int limit);

    /**
     * 查询频道内指定消息之后的更新消息。
     * 输出：按消息 ID 升序返回，最多 limit 条。
     *
     * @param channelId 频道 ID
     * @param cursorMessageId 排他游标消息 ID
     * @param limit 最大返回条数
     * @return 更新消息记录列表
     */
    List<MessageRecord> findAfterByChannelId(long channelId, long cursorMessageId, int limit);

    /**
     * 在频道内按关键字和过滤条件搜索消息。
     * 说明：status、keyword 等条件均由 Repository 传入。
     *
     * @param channelId 频道 ID
     * @param keyword 搜索关键字
     * @param cursorMessageId 搜索排他游标消息 ID，可为空
     * @param senderAccountId 发送者账户 ID，可为空
     * @param domain 消息 domain，可为空
     * @param status 消息状态，可为空
     * @param limit 返回条数
     * @return 搜索命中消息记录列表
     */
    List<MessageRecord> searchByChannelId(
            long channelId,
            String keyword,
            Long cursorMessageId,
            Long senderAccountId,
            String domain,
            String status,
            int limit
    );
}
