package team.carrypigeon.backend.chat.domain.features.message.domain.api.cleanup;

/** 消息功能拥有的频道数据清理边界，不向频道功能暴露消息仓储或表结构。 */
public interface ChannelMessageCleanupApi {
    /**
     * 后台清理已撤下频道的消息和已读状态。
     * 必须在事务中调用，且频道主记录已删除。
     */
    void deleteChannel(long channelId);

    /** 与退群或移除成员同事务清理本人的提及及阅读进度，调用方已锁定频道。 */
    void deleteMemberState(long channelId, long accountId);

    /** 与删除频道主记录同事务清空收件箱，个人收件箱按接收人直接查询。 */
    void deleteChannelMentions(long channelId);
}
