package team.carrypigeon.backend.chat.domain.features.server.domain.service;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.chat.domain.features.server.domain.api.RealtimeEventApi;
import team.carrypigeon.backend.chat.domain.features.server.domain.api.event.RealtimeEvent;
import team.carrypigeon.backend.chat.domain.features.server.domain.capability.RealtimeEventSink;

/**
 * 实时事件发布实现。
 * 职责：规范化接收人并把事件委派给投递能力，同时把投递失败隔离在业务事务之外。
 * 边界：不查询仓储、不接触 JSON 或 Netty、不解释载荷、不改变事件语义。
 */
@Service
public class RealtimeEventDomainApi implements RealtimeEventApi {

    private static final Logger log = LoggerFactory.getLogger(RealtimeEventDomainApi.class);

    private final RealtimeEventSink realtimeEventSink;

    public RealtimeEventDomainApi(RealtimeEventSink realtimeEventSink) {
        this.realtimeEventSink = realtimeEventSink;
    }

    @Override
    public void publish(RealtimeEvent event) {
        if (event == null) {
            return;
        }
        List<Long> recipients = event.recipientAccountIds();
        if (recipients.isEmpty()) {
            return;
        }
        try {
            realtimeEventSink.deliver(event, recipients);
        } catch (RuntimeException exception) {
            // 事务已提交，推送失败不得回传为业务失败；丢失的事件由客户端通过 HTTP 全量同步补齐
            log.error("Failed to deliver realtime event, type={}", event.type().wireName(), exception);
        }
    }
}
