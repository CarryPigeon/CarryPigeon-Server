package team.carrypigeon.backend.chat.domain.features.message.domain.api.cleanup;

/** 消息功能拥有的频道数据清理边界，不向频道功能暴露消息仓储或表结构。 */
public interface ChannelMessageCleanupApi {
    /**
     * 清理一批已删除频道的消息、提及、已读、发送请求和序号数据。
     * 必须在持有频道删除任务锁的事务中调用；返回 true 表示本次检查时已无待清理消息侧数据。
     */
    boolean deleteChannelBatch(long channelId, int limit);
}
