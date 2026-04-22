package team.carrypigeon.backend.chat.domain.features.message.domain.repository;

import java.util.List;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage;

/**
 * 消息仓储抽象。
 * 职责：定义频道消息的业务语义读写入口。
 * 边界：不暴露数据库实现细节。
 */
public interface MessageRepository {

    /**
     * 保存频道消息。
     *
     * @param message 待持久化消息
     * @return 已持久化消息
     */
    ChannelMessage save(ChannelMessage message);

    /**
     * 查询频道历史消息。
     *
     * @param channelId 频道 ID
     * @param cursorMessageId 历史查询游标消息 ID，可为空
     * @param limit 查询条数
     * @return 历史消息列表，按 messageId 倒序排列
     */
    List<ChannelMessage> findByChannelIdBefore(long channelId, Long cursorMessageId, int limit);
}
