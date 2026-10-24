package team.carrypigeon.backend.chat.domain.features.message.domain.capability;

import java.util.Collection;

import team.carrypigeon.backend.infrastructure.basic.plugin.message.CPChannelMessage;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelReadState;
import team.carrypigeon.backend.infrastructure.basic.transaction.TransactionRunner.AfterCommitExecutor;

/**
 * 消息事件通知能力。
 * 职责：把"消息已创建、消息已撤回、某账号已读位置更新"这些事实交给通知实现，在事务提交后执行。
 * 边界：只表达业务意图；不出现线上事件名、载荷字段、序列化方式或传输实现。
 */
public interface MessageEventNotifier {

    /**
     * 通知新消息，并按被提及账号逐个下发提及事件。
     *
     * @param afterCommit 提交后动作注册器
     * @param message 已持久化的消息快照
     * @param recipientAccountIds 接收账号快照
     */
    void messageCreated(
            AfterCommitExecutor afterCommit,
            CPChannelMessage message,
            Collection<Long> recipientAccountIds
    );

    /**
     * 通知消息撤回。
     *
     * @param afterCommit 提交后动作注册器
     * @param message 撤回后的消息快照
     * @param recipientAccountIds 接收账号快照
     */
    void messageRecalled(
            AfterCommitExecutor afterCommit,
            CPChannelMessage message,
            Collection<Long> recipientAccountIds
    );

    /**
     * 通知已读位置更新，仅发给本人。
     *
     * @param afterCommit 提交后动作注册器
     * @param readState 已读状态快照
     */
    void readStateUpdated(AfterCommitExecutor afterCommit, ChannelReadState readState);
}
