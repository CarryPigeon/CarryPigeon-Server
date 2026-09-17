package team.carrypigeon.backend.chat.domain.features.message.controller.http;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import team.carrypigeon.backend.chat.domain.shared.controller.support.RequestAuthenticationContext;
import team.carrypigeon.backend.chat.domain.shared.domain.auth.AuthenticatedAccount;
import team.carrypigeon.backend.chat.domain.features.message.domain.command.UpdateChannelReadStateCommand;
import team.carrypigeon.backend.chat.domain.features.message.domain.projection.ChannelReadStateResult;
import team.carrypigeon.backend.chat.domain.features.message.domain.projection.ChannelUnreadResult;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.MessageReadStateApi;
import team.carrypigeon.backend.chat.domain.features.message.controller.dto.ChannelReadStateResponse;
import team.carrypigeon.backend.chat.domain.features.message.controller.dto.UnreadItemResponse;
import team.carrypigeon.backend.chat.domain.features.message.controller.dto.UnreadListResponse;
import team.carrypigeon.backend.chat.domain.features.message.controller.dto.UpdateChannelReadStateRequest;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;

/**
 * 频道读状态 HTTP 入口。
 */
@RestController
@RequestMapping("/api")
@PreAuthorize("isAuthenticated()")
@Tag(name = "频道读状态", description = "频道已读状态与未读计数能力。")
@Validated
public class ChannelReadStateController {

    private final MessageReadStateApi messageReadStateApi;
    private final RequestAuthenticationContext authRequestContext;

    public ChannelReadStateController(
            MessageReadStateApi messageReadStateApi,
            RequestAuthenticationContext authRequestContext
    ) {
        this.messageReadStateApi = messageReadStateApi;
        this.authRequestContext = authRequestContext;
    }

    @PutMapping("/channels/{channelId}/read_state")
    @Operation(summary = "更新频道已读状态", description = "只前进不后退。")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "返回已读状态")})
    public ChannelReadStateResponse updateReadState(
            @PathVariable @Positive(message = "channelId must be greater than 0") long channelId,
            @Valid @RequestBody UpdateChannelReadStateRequest request,
            HttpServletRequest servletRequest
    ) {
        AuthenticatedAccount principal = authRequestContext.requirePrincipal(servletRequest);
        ChannelReadStateResult result = messageReadStateApi.updateChannelReadState(new UpdateChannelReadStateCommand(
                principal.accountId(),
                channelId,
                parseLastReadMid(request.lastReadMid()),
                request.lastReadTime()
        ));
        return new ChannelReadStateResponse(result.cid(), result.uid(), result.lastReadMid(), result.lastReadTime());
    }

    /**
     * 查询当前用户的频道未读聚合。
     *
     * @param servletRequest 当前 HTTP 请求
     * @return 未读列表响应
     */
    @GetMapping("/unreads")
    @Operation(summary = "获取未读频道列表", description = "返回当前用户的频道未读聚合。")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "返回未读列表")})
    public UnreadListResponse listUnreads(HttpServletRequest servletRequest) {
        AuthenticatedAccount principal = authRequestContext.requirePrincipal(servletRequest);
        List<UnreadItemResponse> items = messageReadStateApi.listUnreads(principal.accountId()).stream()
                .map(this::toResponse)
                .toList();
        return new UnreadListResponse(items);
    }

    private UnreadItemResponse toResponse(ChannelUnreadResult result) {
        return new UnreadItemResponse(result.cid(), result.unreadCount(), result.lastReadTime());
    }

    /**
     * 解析更新已读状态使用的消息 ID。
     * 失败语义：不是十进制雪花 ID 时返回字段级校验问题。
     *
     * @param rawValue 原始消息 ID
     * @return 最后已读消息 ID
     */
    private long parseLastReadMid(String rawValue) {
        try {
            return Long.parseLong(rawValue);
        } catch (NumberFormatException exception) {
            throw ProblemException.validationFailed("last_read_mid must be decimal snowflake string");
        }
    }
}
