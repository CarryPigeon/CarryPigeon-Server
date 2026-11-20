package team.carrypigeon.backend.chat.domain.features.message.controller.http;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.constraints.Positive;
import org.hibernate.validator.constraints.Range;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import team.carrypigeon.backend.chat.domain.config.http.Result;
import team.carrypigeon.backend.chat.domain.config.http.security.CpPrincipal;
import team.carrypigeon.backend.chat.domain.features.message.controller.http.dto.MessageChangeResponse;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.change.MessageChangeApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.change.input.MessageChangeCursor;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.change.result.MessageChangePage;
import team.carrypigeon.backend.chat.domain.features.message.domain.problem.MessageProblem;
import team.carrypigeon.backend.infrastructure.basic.exception.ProblemException;

/** 频道消息短期变更的 HTTP 恢复入口；负责游标及响应的协议表示。 */
@RestController
@PreAuthorize("isAuthenticated()")
public class MessageChangeController {
    private final MessageChangeApi messageChangeApi;

    public MessageChangeController(MessageChangeApi messageChangeApi) {
        this.messageChangeApi = messageChangeApi;
    }

    /**
     * 记录当前事件位置，或读取该位置之后产生的消息变更。
     * 首次省略游标时只记录请求时的最新事件位置，不返回已有事件；客户端读取消息快照后使用返回的游标补拉快照期间产生的变更。
     * 已应用本页后才保存响应游标；事件仅包含消息 ID，客户端按需查询当前消息状态。
     * 游标最多保留两分钟，过期、事件被截断或服务端重启后需要重新获取快照。
     *
     * <p>请求 {@code GET /api/channels/2001/message-events?limit=20}。无请求体。
     * <p>成功响应 HTTP 200，code 为 0，data 为 {@code MessageChangeResponse}。
     * <p>失败示例：恢复位置过期 30005，须刷新快照。
     *
     * @param channelId 频道 ID，调用账号必须是当前成员
     * @param cursor 上一次返回的 sync_cursor；省略时记录当前事件位置并返回空 items
     * @param limit 每页条数，范围 1 至 50，默认 50
     * @param principal 当前认证账号
     */
    @GetMapping(value = "/api/channels/{channelId}/message-events", produces = "application/json")
    @Operation(summary = "恢复频道消息变更", description = "省略 cursor 记录当前事件位置并返回空 items；读取消息快照后使用 sync_cursor 补拉之后产生的变更。事件只包含频道 ID 和消息 ID，按 event_id 去重。游标过期或服务端重启后返回业务码 30005，需重新读取消息快照。")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "返回 items、sync_cursor 和 has_more；省略 cursor 时 items 为空，sync_cursor 记录本次请求时的最新事件位置", useReturnTypeSchema = true))
    public Result<MessageChangeResponse> getChanges(
        @Parameter(description = "目标频道 ID")
        @PathVariable
        @Positive(message = "channelId must be greater than 0")
        long channelId,
        @Parameter(description = "上次返回的 sync_cursor；省略时记录当前事件位置并返回空 items")
        @RequestParam(required = false)
        String cursor,
        @Parameter(description = "每页最大条数，范围 1 至 50")
        @RequestParam(defaultValue = "50")
        @Range(min = 1, max = 50, message = "limit must be between 1 and 50")
        int limit,
        @AuthenticationPrincipal
        CpPrincipal principal
    ) {
        MessageChangePage page = messageChangeApi.getChanges(principal.accountId(), channelId,
                decodeCursor(channelId, cursor), limit);
        return Result.success(new MessageChangeResponse(page.items().stream()
                .map(event -> new MessageChangeResponse.Event(Long.toString(event.eventId()), event.sequence(),
                        event.type(), event.createdAt(), Map.of("channel_id", Long.toString(event.channelId()),
                                "message_id", Long.toString(event.messageId()))))
                .toList(), encodeCursor(page.nextCursor()), page.hasMore()));
    }

    /** 游标只用于当前频道的事件恢复，不与消息历史游标互通。 */
    private MessageChangeCursor decodeCursor(long channelId, String cursor) {
        if (cursor == null) {
            return null;
        }
        try {
            String[] parts = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8).split(":", -1);
            if (parts.length != 4 || Long.parseLong(parts[1]) != channelId) {
                throw new IllegalArgumentException();
            }
            return new MessageChangeCursor(UUID.fromString(parts[0]), channelId,
                    Long.parseLong(parts[2]), Long.parseLong(parts[3]));
        } catch (IllegalArgumentException exception) {
            throw ProblemException.of(MessageProblem.CURSOR_INVALID);
        }
    }

    private String encodeCursor(MessageChangeCursor cursor) {
        String value = cursor.instanceId() + ":" + cursor.channelId() + ":" + cursor.sequence() + ":" + cursor.issuedAt();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
