package team.carrypigeon.backend.chat.domain.features.message.domain.api.readstate;

import java.util.List;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.readstate.input.UpdateChannelReadStateCommand;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.readstate.result.ChannelReadStateResult;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.readstate.result.ChannelUnreadResult;

/**
 * 消息读状态领域 API。
 * 职责：暴露频道消息已读位置更新与未读统计能力。
 */
public interface MessageReadStateApi {

    /**
     * 更新当前账号在频道中的最后已读消息位置。
     *
     * @param accountId 当前账号 ID
     * @param channelId 频道 ID
     * @param lastReadMessageId 最后已读消息 ID
     * @param lastReadTime 最后已读时间
     * @return 更新后或保持不变的读状态
     */
    ChannelReadStateResult updateChannelReadState(long accountId, long channelId, long lastReadMessageId, long lastReadTime);

    /**
     * 查询当前账号各频道的未读统计。
     *
     * @param accountId 当前账号 ID
     * @return 未读统计列表
     */
    List<ChannelUnreadResult> listUnreads(long accountId);
}
