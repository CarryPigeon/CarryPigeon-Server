package team.carrypigeon.backend.chat.domain.features.message.controller.http;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import team.carrypigeon.backend.chat.domain.features.message.controller.dto.ChannelPinItemResponse;
import team.carrypigeon.backend.chat.domain.features.message.controller.dto.ChannelPinListResponse;
import team.carrypigeon.backend.chat.domain.features.message.controller.dto.PinChannelMessageRequest;
import team.carrypigeon.backend.chat.domain.features.message.domain.command.PinChannelMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.domain.command.UnpinChannelMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.domain.projection.ChannelPinResult;
import team.carrypigeon.backend.chat.domain.features.message.domain.query.ListChannelPinsQuery;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.ChannelPinApi;
import team.carrypigeon.backend.chat.domain.shared.controller.OpaqueCursorCodec;
import team.carrypigeon.backend.chat.domain.shared.controller.support.RequestAuthenticationContext;
import team.carrypigeon.backend.chat.domain.shared.domain.auth.AuthenticatedAccount;
import team.carrypigeon.backend.infrastructure.basic.id.Ids;

/**
 * 频道置顶消息 HTTP 入口。
 * 职责：提供频道消息置顶、取消置顶和置顶列表查询协议能力。
 * 边界：只承接 pins 资源相关请求，不承载消息发送、历史查询和业务规则。
 */
@RestController
@RequestMapping("/api/channels")
public class ChannelPinsController {

    private static final String PIN_CURSOR_SCOPE = "channel_pins";

    private final ChannelPinApi channelPinDomainApi;
    private final RequestAuthenticationContext authRequestContext;

    /**
     * 创建频道置顶消息 HTTP 入口。
     *
     * @param channelPinDomainApi 频道置顶领域 API
     * @param authRequestContext 请求认证上下文
     */
    public ChannelPinsController(
            ChannelPinApi channelPinDomainApi,
            RequestAuthenticationContext authRequestContext
    ) {
        this.channelPinDomainApi = channelPinDomainApi;
        this.authRequestContext = authRequestContext;
    }

    /**
     * 置顶指定频道消息。
     *
     * @param channelId 频道 ID
     * @param messageId 消息 ID
     * @param requestBody 置顶备注请求体
     * @param request 当前 HTTP 请求
     * @return 置顶结果
     */
    @PostMapping("/{channelId}/pins/{messageId}")
    @Operation(summary = "置顶频道消息", description = "置顶指定消息。")
    public ChannelPinItemResponse pinChannelMessage(
            @PathVariable long channelId,
            @PathVariable long messageId,
            @RequestBody(required = false) PinChannelMessageRequest requestBody,
            HttpServletRequest request
    ) {
        AuthenticatedAccount principal = authRequestContext.requirePrincipal(request);
        ChannelPinResult result = channelPinDomainApi.pinChannelMessage(
                new PinChannelMessageCommand(principal.accountId(), channelId, messageId, requestBody == null ? null : requestBody.note())
        );
        return toPinResponse(result);
    }

    /**
     * 取消指定频道消息置顶。
     *
     * @param channelId 频道 ID
     * @param messageId 消息 ID
     * @param request 当前 HTTP 请求
     * @return 空响应
     */
    @DeleteMapping("/{channelId}/pins/{messageId}")
    @Operation(summary = "取消置顶频道消息", description = "取消置顶指定消息。")
    public ResponseEntity<Void> unpinChannelMessage(
            @PathVariable long channelId,
            @PathVariable long messageId,
            HttpServletRequest request
    ) {
        AuthenticatedAccount principal = authRequestContext.requirePrincipal(request);
        channelPinDomainApi.unpinChannelMessage(
                new UnpinChannelMessageCommand(principal.accountId(), channelId, messageId)
        );
        return ResponseEntity.noContent().build();
    }

    /**
     * 查询频道置顶消息列表。
     *
     * @param channelId 频道 ID
     * @param cursor 分页游标
     * @param limit 查询条数
     * @param request 当前 HTTP 请求
     * @return 置顶消息分页结果
     */
    @GetMapping("/{channelId}/pins")
    @Operation(summary = "获取频道置顶列表", description = "按频道返回置顶消息列表。")
    public ChannelPinListResponse listChannelPins(
            @PathVariable long channelId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit,
            HttpServletRequest request
    ) {
        AuthenticatedAccount principal = authRequestContext.requirePrincipal(request);
        var items = channelPinDomainApi.listChannelPins(new ListChannelPinsQuery(
                principal.accountId(),
                channelId,
                OpaqueCursorCodec.decode(PIN_CURSOR_SCOPE, cursor),
                limit
        ));
        boolean hasMore = items.size() > limit;
        var pageItems = hasMore ? items.subList(0, limit) : items;
        String nextCursor = hasMore ? OpaqueCursorCodec.encode(PIN_CURSOR_SCOPE, pageItems.get(pageItems.size() - 1).messageId()) : null;
        return new ChannelPinListResponse(pageItems.stream().map(this::toPinResponse).toList(), nextCursor, hasMore);
    }

    private ChannelPinItemResponse toPinResponse(ChannelPinResult result) {
        return new ChannelPinItemResponse(
                Ids.toString(result.channelId()),
                Ids.toString(result.messageId()),
                Ids.toString(result.pinnedByAccountId()),
                result.pinnedAt().toEpochMilli(),
                result.note()
        );
    }
}
