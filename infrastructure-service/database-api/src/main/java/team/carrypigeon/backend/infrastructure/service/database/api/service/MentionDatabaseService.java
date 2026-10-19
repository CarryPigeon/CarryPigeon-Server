package team.carrypigeon.backend.infrastructure.service.database.api.service;

import java.util.List;

import team.carrypigeon.backend.infrastructure.service.database.api.model.MentionRecord;

/**
 * 提及数据库服务抽象。
 * 职责：向 chat-domain 提供提及记录写入、读取与已读更新能力。
 * 边界：不暴露 JDBC、SQL 或具体数据库框架细节。
 */
public interface MentionDatabaseService {

    /**
     * 批量写入提及记录。
     * 约束：空集合不产生数据库访问。
     *
     * @param records 待写入提及记录
     */
    void insertAll(List<MentionRecord> records);

    /**
     * 删除指定消息产生的全部提及记录。
     *
     * @param messageId 消息 ID
     */
    void deleteByMessageId(long messageId);

    /**
     * 查询账户的提及记录。
     * 输出：按提及 ID 倒序返回，最多 limit 条。
     *
     * @param accountId 账户 ID
     * @param cursorMentionId 游标提及 ID，可为空
     * @param limit 查询条数
     * @param unreadOnly 是否只返回未读提及
     * @param channelId 频道 ID，可为空表示不限频道
     * @return 提及记录列表
     */
    List<MentionRecord> findByAccountId(
            long accountId,
            Long cursorMentionId,
            int limit,
            boolean unreadOnly,
            Long channelId
    );

    /**
     * 将单条提及标记为已读。
     *
     * @param accountId 账户 ID
     * @param mentionId 提及 ID
     * @return 实际更新时返回 true；提及不存在或不属于该账户时返回 false
     */
    boolean markAsRead(long accountId, long mentionId);

    /**
     * 批量标记账户的提及为已读。
     *
     * @param accountId 账户 ID
     * @param beforeMentionId 上界提及 ID，可为空表示不限上界
     * @param channelId 频道 ID，可为空表示不限频道
     * @return 实际更新的提及数量
     */
    int markAllAsRead(long accountId, Long beforeMentionId, Long channelId);
}
