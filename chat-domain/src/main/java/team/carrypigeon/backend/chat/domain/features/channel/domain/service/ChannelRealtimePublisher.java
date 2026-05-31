package team.carrypigeon.backend.chat.domain.features.channel.domain.service;

import java.util.Collection;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.Channel;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelReadState;

/**
 * 频道实时分发抽象。
 * 职责：定义频道视图与读状态变更后的实时推送端口。
 * 边界：业务层只依赖抽象，不感知 Netty 会话实现细节。
 */
public interface ChannelRealtimePublisher {

    /**
     * 向当前账户的全部在线会话分发读状态更新。
     *
     * @param readState 最新读状态
     */
    default void publishReadStateUpdated(ChannelReadState readState) {
    }

    /**
     * 向频道成员分发频道视图变更提示。
     *
     * @param channel 目标频道
     * @param scope 变更范围
     * @param recipientAccountIds 目标账户 ID 列表
     */
    default void publishChannelChanged(Channel channel, String scope, Collection<Long> recipientAccountIds) {
    }

    /**
     * 向当前账户的全部在线会话分发频道列表刷新提示。
     *
     * @param accountId 目标账户 ID
     */
    default void publishChannelsChanged(long accountId) {
    }
}
