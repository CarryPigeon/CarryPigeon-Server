package team.carrypigeon.backend.chat.domain.features.channel.domain.repository;

import java.util.List;
import java.util.Optional;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelReadState;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelUnread;

/**
 * 频道已读状态仓储抽象。
 */
public interface ChannelReadStateRepository {

    Optional<ChannelReadState> findByChannelIdAndAccountId(long channelId, long accountId);

    ChannelReadState upsert(ChannelReadState readState);

    List<ChannelUnread> listUnreadsByAccountId(long accountId);
}
