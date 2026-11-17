package team.carrypigeon.backend.chat.domain.features.message.adaptor.realtime;

import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Component;
import team.carrypigeon.backend.infrastructure.basic.json.JsonProvider;
import team.carrypigeon.backend.infrastructure.basic.id.IdGenerator;
import team.carrypigeon.backend.infrastructure.service.database.api.service.MessageEventDatabaseService;

import team.carrypigeon.backend.chat.domain.features.message.domain.capability.MessageEventNotifier;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelReadState;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.Mention;
import team.carrypigeon.backend.infrastructure.basic.plugin.message.api.CPChannelMessage;
import team.carrypigeon.backend.infrastructure.basic.realtime.downlink.RealtimeEvent;
import team.carrypigeon.backend.infrastructure.basic.realtime.downlink.RealtimeEventApi;
import team.carrypigeon.backend.infrastructure.basic.realtime.downlink.RealtimeEventType;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;
import team.carrypigeon.backend.infrastructure.basic.transaction.TransactionRunner.AfterCommitExecutor;

/**
 * 基于实时通道的消息事件通知适配器。
 * 职责：把域内的消息事件意图翻译为线上事件名与载荷，创建与撤回在消息事务中入库，其余事件仍在提交后实时发布。
 * 边界：不读取仓储，domain data 直接使用业务消息快照。
 */
@Component
public class RealtimeMessageEventNotifier implements MessageEventNotifier {

    private static final RealtimeEventType MESSAGE_CREATED = new RealtimeEventType("message.created");
    private static final RealtimeEventType MESSAGE_RECALLED = new RealtimeEventType("message.recalled");
    private static final RealtimeEventType MENTION_CREATED = new RealtimeEventType("mention.created");
    private static final RealtimeEventType READ_STATE_UPDATED = new RealtimeEventType("read_state.updated");

    private final RealtimeEventApi realtimeEventApi;
    private final TimeProvider timeProvider;
    private final MessageEventDatabaseService events;
    private final IdGenerator ids;
    private final JsonProvider json;

    public RealtimeMessageEventNotifier(RealtimeEventApi realtimeEventApi, TimeProvider timeProvider,
            MessageEventDatabaseService events, IdGenerator ids, JsonProvider json) {
        this.realtimeEventApi = realtimeEventApi;
        this.timeProvider = timeProvider;
        this.events = events;
        this.ids = ids;
        this.json = json;
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
    public void messageCreated(CPChannelMessage message) {
        // 事件与消息共用当前事务；不依赖提交后内存回调，进程中断后仍能恢复。
        events.append(ids.nextLongId(), message.channelId(), message.messageId(), MESSAGE_CREATED.wireName(),
                json.toJson(new MessageCreatedPayload(Long.toString(message.channelId()), toBody(message))));
    }

    @Override
    public void messageRecalled(CPChannelMessage message) {
        events.append(ids.nextLongId(), message.channelId(), message.messageId(), MESSAGE_RECALLED.wireName(),
                json.toJson(new MessageRecalledPayload(Long.toString(message.channelId()),
                        Long.toString(message.messageId()), timeProvider.nowMillis())));
        // 已撤回正文不能从保留窗口内的创建事件重新读取；与撤回和事件追加一起提交。
        events.redactCreated(message.channelId(), message.messageId(),
                json.toJson(new MessageCreatedPayload(Long.toString(message.channelId()), toBody(message))));
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

    /**
     * 把领域消息快照翻译为线上消息体。
     *
     * @param message 消息快照
     * @return 线上消息体
     */
    private MessageBody toBody(CPChannelMessage message) {
        return new MessageBody(
                Long.toString(message.messageId()),
                Long.toString(message.senderId()),
                Long.toString(message.channelId()),
                message.sequence(),
                message.domain(),
                message.data(),
                message.sendTime().toEpochMilli(),
                message.mentions().stream().map(String::valueOf).toList(),
                message.preview(),
                message.status().name().toLowerCase(Locale.ROOT)
        );
    }
}
