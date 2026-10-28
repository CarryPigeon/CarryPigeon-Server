package team.carrypigeon.backend.chat.domain.features.channel.domain.capability;

import java.util.Collection;
import java.util.List;

import team.carrypigeon.backend.infrastructure.basic.transaction.TransactionRunner.AfterCommitExecutor;

/**
 * 频道变化通知能力。
 * 职责：把"某频道或某账号的视图已失效"这一事实交给通知实现，在事务提交后执行。
 * 边界：只表达业务意图；不出现线上事件名、载荷字段、序列化方式或传输实现。
 */
public interface ChannelEventNotifier {

    /**
     * 通知频道成员某类视图已失效。
     *
     * @param afterCommit 提交后动作注册器
     * @param channelId 频道 ID
     * @param scope 失效范围
     * @param recipientAccountIds 接收账号快照
     */
    void channelChanged(
            AfterCommitExecutor afterCommit,
            long channelId,
            ChannelChangeScope scope,
            Collection<Long> recipientAccountIds
    );

    /**
     * 通知某账号其可见频道集合已失效。
     *
     * @param afterCommit 提交后动作注册器
     * @param ids 账号 ID
     */
    void accountChannelsChanged(AfterCommitExecutor afterCommit, List<Long> ids);
}
