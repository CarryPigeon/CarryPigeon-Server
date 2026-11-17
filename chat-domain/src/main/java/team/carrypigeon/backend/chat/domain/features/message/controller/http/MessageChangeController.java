package team.carrypigeon.backend.chat.domain.features.message.controller.http;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import jakarta.validation.constraints.Positive;
import org.hibernate.validator.constraints.Range;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import team.carrypigeon.backend.chat.domain.config.http.security.CpPrincipal;
import team.carrypigeon.backend.chat.domain.features.message.domain.problem.MessageProblem;
import team.carrypigeon.backend.chat.domain.features.message.domain.service.message.MessageChangeService;
import team.carrypigeon.backend.infrastructure.basic.exception.ProblemException;

/** 消息创建与撤回变更的 HTTP 恢复入口；游标独立于消息历史分页，绑定版本和频道。 */
@RestController
@Validated
@PreAuthorize("isAuthenticated()")
public class MessageChangeController {
    private final MessageChangeService changes;

    public MessageChangeController(MessageChangeService changes) {
        this.changes = changes;
    }

    /** 先省略 cursor 获取水位，再读取消息快照；之后从该水位补拉并按 event_id 幂等应用。 */
    @GetMapping("/api/channels/{channelId}/message-events")
    @Operation(summary = "恢复消息变更", description = "省略 cursor 只建立水位；先取水位再读消息快照，再持 sync_cursor 补拉。按事件顺序应用并以 event_id 去重；创建按 message_id 更新或插入，撤回更新同一消息。已应用后才保存游标，不能用推送中的最大序号跳过补拉。410 要求重建快照。")
    public Response getChanges(@PathVariable @Positive long channelId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "50") @Range(min = 1, max = 50) int limit,
            @AuthenticationPrincipal CpPrincipal principal) {
        Long sequence = decode(channelId, cursor);
        var page = changes.getChanges(principal.accountId(), channelId, sequence, limit);
        boolean hasMore = page.events().size() > limit;
        var events = page.events().stream().limit(limit).toList();
        long appliedThrough = events.isEmpty() ? (sequence == null ? page.latestSequence() : sequence) : events.getLast().sequence();
        return new Response(events.stream().map(event -> new Event(Long.toString(event.eventId()), event.sequence(),
                event.type(), event.createdAt().toEpochMilli(), event.payload())).toList(), encode(channelId, appliedThrough), hasMore);
    }

    private String encode(long channelId, long sequence) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(
                ("message_events_v1_" + channelId + ":" + sequence).getBytes(StandardCharsets.UTF_8));
    }

    /** 空频道合法水位为 0；空白、其它频道或消息历史游标均不能冒充事件游标。 */
    private Long decode(long channelId, String cursor) {
        if (cursor == null) {
            return null;
        }
        try {
            String raw = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            String prefix = "message_events_v1_" + channelId + ":";
            if (!raw.startsWith(prefix)) {
                throw new IllegalArgumentException();
            }
            long sequence = Long.parseLong(raw.substring(prefix.length()));
            if (sequence < 0) {
                throw new IllegalArgumentException();
            }
            return sequence;
        } catch (IllegalArgumentException exception) {
            throw ProblemException.of(MessageProblem.CURSOR_INVALID);
        }
    }

    /** eventId 为 HTTP 字符串 ID；sequence 是频道事件位置，不能用推送中的最大值跳过恢复。 */
    public record Event(String eventId, long sequence, String eventType, long serverTime, Map<String, Object> payload) { }
    /** syncCursor 在空页仍保留；客户端只在按顺序应用本页后推进。 */
    public record Response(List<Event> items, String syncCursor, boolean hasMore) { }
}
