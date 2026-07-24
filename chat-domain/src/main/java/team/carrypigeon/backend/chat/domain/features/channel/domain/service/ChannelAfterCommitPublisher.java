package team.carrypigeon.backend.chat.domain.features.channel.domain.service;

import java.util.List;
import java.util.Map;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.Channel;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelReadState;
import team.carrypigeon.backend.chat.domain.features.server.domain.api.RealtimeEventApi;
import team.carrypigeon.backend.chat.domain.features.server.domain.command.PublishRealtimeEventCommand;
import team.carrypigeon.backend.infrastructure.service.database.api.transaction.TransactionRunner.AfterCommitExecutor;

/**
 * 频道事务后实时发布协作对象。
 * 职责：集中登记频道资料、成员、封禁、申请和已读状态相关的 after-commit 发布动作。
 * 边界：只负责提交后的实时发布，不读取仓储、不修改频道状态。
 */
class ChannelAfterCommitPublisher {

    private final RealtimeEventApi realtimeEventApi;

    ChannelAfterCommitPublisher(RealtimeEventApi realtimeEventApi) {
        this.realtimeEventApi = realtimeEventApi;
    }

    void publishReadStateUpdatedAfterCommit(AfterCommitExecutor afterCommit, ChannelReadState readState) {
        afterCommit.execute(() -> realtimeEventApi.publish(new PublishRealtimeEventCommand(
                readState.channelId(),
                "read_state.updated",
                Map.of(
                        "cid", Long.toString(readState.channelId()),
                        "uid", Long.toString(readState.accountId()),
                        "last_read_mid", Long.toString(readState.lastReadMessageId()),
                        "last_read_time", readState.lastReadTime().toEpochMilli()
                ),
                List.of(readState.accountId()),
                true
        )));
    }

    void publishChannelChangedAfterCommit(
            AfterCommitExecutor afterCommit,
            Channel channel,
            String scope,
            List<Long> recipientAccountIds
    ) {
        afterCommit.execute(() -> realtimeEventApi.publish(new PublishRealtimeEventCommand(
                channel.id(),
                "channel.changed",
                Map.of(
                        "cid", Long.toString(channel.id()),
                        "scope", scope == null || scope.isBlank() ? "profile" : scope,
                        "hint", "refresh"
                ),
                recipientAccountIds,
                true
        )));
    }

    void publishChannelsChangedAfterCommit(AfterCommitExecutor afterCommit, long accountId) {
        afterCommit.execute(() -> realtimeEventApi.publish(new PublishRealtimeEventCommand(
                null,
                "channels.changed",
                Map.of("hint", "refresh"),
                List.of(accountId),
                false
        )));
    }
}
