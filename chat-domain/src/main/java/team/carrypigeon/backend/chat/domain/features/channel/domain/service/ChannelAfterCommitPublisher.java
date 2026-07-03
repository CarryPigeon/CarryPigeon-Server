package team.carrypigeon.backend.chat.domain.features.channel.domain.service;

import java.util.List;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.Channel;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelReadState;
import team.carrypigeon.backend.chat.domain.features.channel.domain.port.ChannelRealtimePublisher;
import team.carrypigeon.backend.infrastructure.service.database.api.transaction.TransactionRunner.AfterCommitExecutor;

/**
 * 频道事务后实时发布协作对象。
 * 职责：集中登记频道资料、成员、封禁、申请和已读状态相关的 after-commit 发布动作。
 * 边界：只负责提交后的实时发布，不读取仓储、不修改频道状态。
 */
class ChannelAfterCommitPublisher {

    private final ChannelRealtimePublisher channelRealtimePublisher;

    ChannelAfterCommitPublisher(ChannelRealtimePublisher channelRealtimePublisher) {
        this.channelRealtimePublisher = channelRealtimePublisher;
    }

    void publishReadStateUpdatedAfterCommit(AfterCommitExecutor afterCommit, ChannelReadState readState) {
        afterCommit.execute(() -> channelRealtimePublisher.publishReadStateUpdated(readState));
    }

    void publishChannelChangedAfterCommit(
            AfterCommitExecutor afterCommit,
            Channel channel,
            String scope,
            List<Long> recipientAccountIds
    ) {
        afterCommit.execute(() -> channelRealtimePublisher.publishChannelChanged(channel, scope, recipientAccountIds));
    }

    void publishChannelsChangedAfterCommit(AfterCommitExecutor afterCommit, long accountId) {
        afterCommit.execute(() -> channelRealtimePublisher.publishChannelsChanged(accountId));
    }
}
