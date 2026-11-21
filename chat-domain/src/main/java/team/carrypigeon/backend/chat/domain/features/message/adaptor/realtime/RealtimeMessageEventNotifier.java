package team.carrypigeon.backend.chat.domain.features.message.adaptor.realtime;

import java.util.List;

import org.springframework.stereotype.Component;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.infrastructure.basic.realtime.RealtimeFrame;
import team.carrypigeon.backend.infrastructure.basic.id.IdGenerator;

import team.carrypigeon.backend.chat.domain.features.message.domain.capability.MessageEventNotifier;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelReadState;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.Mention;
import team.carrypigeon.backend.infrastructure.basic.plugin.message.api.CPChannelMessage;
import team.carrypigeon.backend.infrastructure.basic.realtime.downlink.RealtimeEvent;
import team.carrypigeon.backend.infrastructure.basic.realtime.downlink.RealtimeEventApi;
import team.carrypigeon.backend.infrastructure.basic.realtime.downlink.RealtimeEventType;
import team.carrypigeon.backend.infrastructure.basic.transaction.TransactionRunner.AfterCommitExecutor;

/**
 * 基于实时通道的消息事件通知适配器。
 * 职责：在业务提交后发布消息和已读通知；频道消息事件暂存于进程内。
 * 边界：频道事件仅携带消息 ID，不缓存消息正文。
 */
@Component
@Slf4j
public class RealtimeMessageEventNotifier implements MessageEventNotifier {

    private static final RealtimeEventType MESSAGE_CREATED = new RealtimeEventType("message.created");
    private static final RealtimeEventType MESSAGE_RECALLED = new RealtimeEventType("message.recalled");
    private static final RealtimeEventType MENTION_CREATED = new RealtimeEventType("mention.created");
    private static final RealtimeEventType READ_STATE_UPDATED = new RealtimeEventType("read_state.updated");

    private final RealtimeEventApi realtimeEventApi;
    private final ChannelEventBuffer eventBuffer;
    private final IdGenerator ids;

    public RealtimeMessageEventNotifier(RealtimeEventApi realtimeEventApi,
            ChannelEventBuffer eventBuffer, IdGenerator ids) {
        this.realtimeEventApi = realtimeEventApi;
        this.eventBuffer = eventBuffer;
        this.ids = ids;
    }

    @Override
    public void readStateUpdated(AfterCommitExecutor afterCommit, ChannelReadState readState) {
        afterCommit.execute(() -> realtimeEventApi.publish(new RealtimeEvent(
                READ_STATE_UPDATED,
                new ReadStateUpdatedPayload(
                        Long.toString(readState.channelId()),
                        Long.toString(readState.accountId()),
                        Long.toString(readState.lastReadMessageId())
                ),
                List.of(readState.accountId())
        )));
    }

    @Override
    public void messageCreated(AfterCommitExecutor afterCommit, CPChannelMessage message) {
        afterCommit.execute(() -> publishMessageEvent(MESSAGE_CREATED, message));
    }

    @Override
    public void messageRecalled(AfterCommitExecutor afterCommit, CPChannelMessage message) {
        afterCommit.execute(() -> publishMessageEvent(MESSAGE_RECALLED, message));
    }

    private void publishMessageEvent(RealtimeEventType type, CPChannelMessage message) {
        var event = eventBuffer.append(message.channelId(), ids.nextLongId(), type.wireName(), message.messageId());
        try {
            realtimeEventApi.publishToChannel(message.channelId(), new RealtimeFrame.Event(
                    Long.toString(event.eventId()), type, event.createdAt(), Map.of(
                            "channel_id", Long.toString(message.channelId()),
                            "message_id", Long.toString(message.messageId()),
                            "event_sequence", event.sequence())));
        } catch (RuntimeException exception) {
            // 数据库已提交；保留事件供重连补拉，不能将已成功的业务操作报告为失败。
            log.warn("频道事件推送失败，重连后可通过游标补拉：channelId={}, eventId={}",
                    message.channelId(), event.eventId(), exception);
        }
    }

    @Override
    public void mentionCreated(AfterCommitExecutor afterCommit, Mention mention) {
        afterCommit.execute(() -> realtimeEventApi.publish(new RealtimeEvent(
                MENTION_CREATED,
                new MentionCreatedPayload(
                        Long.toString(mention.mentionId()),
                        Long.toString(mention.channelId()),
                        Long.toString(mention.messageId()),
                        Long.toString(mention.fromAccountId()),
                        Long.toString(mention.targetAccountId()),
                        mention.createdAt().toEpochMilli()
                ),
                List.of(mention.targetAccountId())
        )));
    }

}
