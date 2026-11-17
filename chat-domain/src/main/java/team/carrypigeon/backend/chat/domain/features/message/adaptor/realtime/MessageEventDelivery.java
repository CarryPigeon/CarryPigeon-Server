package team.carrypigeon.backend.chat.domain.features.message.adaptor.realtime;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import team.carrypigeon.backend.infrastructure.basic.json.JsonProvider;
import team.carrypigeon.backend.infrastructure.basic.realtime.RealtimeFrame;
import team.carrypigeon.backend.infrastructure.basic.realtime.RealtimeServerProperties;
import team.carrypigeon.backend.infrastructure.basic.realtime.downlink.RealtimeEventApi;
import team.carrypigeon.backend.infrastructure.basic.realtime.downlink.RealtimeEventType;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;
import team.carrypigeon.backend.infrastructure.basic.transaction.TransactionRunner;
import team.carrypigeon.backend.infrastructure.service.database.api.service.MessageEventDatabaseService;

/** 消息 outbox 派发与保留期清理；派发状态只表示本机传输尝试，客户端处理结果由恢复游标确认。 */
@Component
@Slf4j
public class MessageEventDelivery {
    private final MessageEventDatabaseService events;
    private final TransactionRunner transactions;
    private final RealtimeEventApi realtime;
    private final JsonProvider json;
    private final TimeProvider clock;
    private final RealtimeServerProperties properties;

    public MessageEventDelivery(MessageEventDatabaseService events, TransactionRunner transactions, RealtimeEventApi realtime,
            JsonProvider json, TimeProvider clock, RealtimeServerProperties properties) {
        this.events = events;
        this.transactions = transactions;
        this.realtime = realtime;
        this.json = json;
        this.clock = clock;
        this.properties = properties;
    }

    /** 每次调度有界处理；并发工作进程按行锁领取，进程中断后仍可再次领取未提交的派发结果。 */
    @Scheduled(fixedDelay = 1000)
    public void deliverPending() {
        try {
            for (int count = 0; count < 64 && deliverNext(); count++) { }
        } catch (RuntimeException exception) {
            log.error("消息事件派发事务失败，未提交状态将在后续重试", exception);
        }
    }

    /** 返回 false 表示当前没有到期事件；重试始终保留原 event_id，允许客户端幂等应用。 */
    public boolean deliverNext() {
        return transactions.runInTransaction(() -> {
            var pending = events.claimPending(clock.nowInstant().minus(properties.getEventRetention()));
            if (pending.isEmpty()) {
                return false;
            }
            var event = pending.get();
            try {
                ObjectNode payload = (ObjectNode) json.readTree(event.payload());
                payload.put("event_sequence", event.sequence());
                realtime.publishToChannel(event.channelId(), new RealtimeFrame.Event(Long.toString(event.eventId()),
                        new RealtimeEventType(event.type()), event.createdAt().toEpochMilli(), payload));
            } catch (RuntimeException exception) {
                events.retryLater(event.eventId());
                log.warn("消息事件派发失败，保留事件并退避重试：eventId={}", event.eventId(), exception);
                return true;
            }
            // 发送后、状态提交前崩溃会重复投递；不能声称 Netty 入队等同于客户端已应用。
            events.markDelivered(event.eventId());
            return true;
        });
    }

    /** 清理只推进连续过期前缀，保留流边界以识别过期游标；超过窗口的未派发事件也不再尝试。 */
    @Scheduled(fixedDelay = 30000)
    public void purgeExpired() {
        try {
            transactions.runInTransaction(() -> events.purgeExpired(clock.nowInstant().minus(properties.getEventRetention()), 1000));
        } catch (RuntimeException exception) {
            log.error("消息事件保留期清理失败，边界与记录保持原事务状态", exception);
        }
    }
}
