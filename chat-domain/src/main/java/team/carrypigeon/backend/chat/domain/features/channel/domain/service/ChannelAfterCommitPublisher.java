package team.carrypigeon.backend.chat.domain.features.channel.domain.service;

import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import team.carrypigeon.backend.chat.domain.features.channel.domain.event.AccountChannelsChangedEvent;
import team.carrypigeon.backend.chat.domain.features.channel.domain.event.ChannelChangedEvent;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.Channel;
import team.carrypigeon.backend.infrastructure.service.database.api.transaction.TransactionRunner.AfterCommitExecutor;

/**
 * 频道事务后实时发布协作对象。
 * 职责：集中登记频道事实事件的 after-commit 发布动作。
 * 边界：只发布 channel 拥有的事件，不引用 realtime 协议或 server feature。
 */
class ChannelAfterCommitPublisher {

    private final ApplicationEventPublisher eventPublisher;

    ChannelAfterCommitPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    void publishChannelChangedAfterCommit(
            AfterCommitExecutor afterCommit,
            Channel channel,
            String scope,
            List<Long> recipientAccountIds
    ) {
        afterCommit.execute(() -> eventPublisher.publishEvent(new ChannelChangedEvent(
                channel.id(), scope, recipientAccountIds
        )));
    }

    void publishChannelsChangedAfterCommit(AfterCommitExecutor afterCommit, long accountId) {
        afterCommit.execute(() -> eventPublisher.publishEvent(new AccountChannelsChangedEvent(accountId)));
    }
}
