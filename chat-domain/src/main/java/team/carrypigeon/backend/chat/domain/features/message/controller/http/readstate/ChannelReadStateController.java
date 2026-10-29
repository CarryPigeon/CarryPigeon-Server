package team.carrypigeon.backend.chat.domain.features.message.controller.http.readstate;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import team.carrypigeon.backend.chat.domain.config.http.security.CpPrincipal;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.readstate.result.ChannelReadStateResult;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.readstate.MessageReadStateApi;
import team.carrypigeon.backend.chat.domain.features.message.controller.http.readstate.dto.ChannelReadStateResponse;
import team.carrypigeon.backend.chat.domain.features.message.controller.http.readstate.dto.UnreadItemResponse;
import team.carrypigeon.backend.chat.domain.features.message.controller.http.readstate.dto.UnreadListResponse;
import team.carrypigeon.backend.chat.domain.features.message.controller.http.readstate.dto.UpdateChannelReadStateRequest;

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

    public ChannelReadStateController(
        MessageReadStateApi messageReadStateApi
    ) {
        this.messageReadStateApi = messageReadStateApi;
    }

    @PutMapping("/channels/{channelId}/read_state")
    @Operation(summary = "更新频道已读状态", description = "只前进不后退。")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "返回已读状态")})
    public ChannelReadStateResponse updateReadState(
        @PathVariable @Positive(message = "channelId must be greater than 0") long channelId,
        @Valid @RequestBody UpdateChannelReadStateRequest request,
        @AuthenticationPrincipal CpPrincipal principal
    ) {
        ChannelReadStateResult result = messageReadStateApi.updateChannelReadState(
            principal.accountId(),
            channelId,
            request.lastReadMid(),
            request.lastReadTime()
        );
        return new ChannelReadStateResponse(result.cid(), result.uid(), result.lastReadMid(), result.lastReadTime());
    }

    /**
     * 查询当前用户的频道未读聚合。
     *
     * @return 未读列表响应
     */
    @GetMapping("/unreads")
    @Operation(summary = "获取未读频道列表", description = "返回当前用户的频道未读聚合。")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "返回未读列表")})
    public UnreadListResponse listUnreads(
        @AuthenticationPrincipal CpPrincipal principal
    ) {
        List<UnreadItemResponse> items = messageReadStateApi.listUnreads(principal.accountId()).stream()
            .map(result -> new UnreadItemResponse(result.cid(), result.unreadCount(), result.lastReadTime()))
            .toList();
        return new UnreadListResponse(items);
    }
}
