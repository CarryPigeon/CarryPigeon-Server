package team.carrypigeon.backend.chat.domain.features.message.controller.http.readstate;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import team.carrypigeon.backend.chat.domain.config.http.Result;
import team.carrypigeon.backend.chat.domain.config.http.security.CpPrincipal;
import team.carrypigeon.backend.chat.domain.features.message.controller.http.readstate.dto.ChannelReadStateResponse;
import team.carrypigeon.backend.chat.domain.features.message.controller.http.readstate.dto.UnreadItemResponse;
import team.carrypigeon.backend.chat.domain.features.message.controller.http.readstate.dto.UnreadListResponse;
import team.carrypigeon.backend.chat.domain.features.message.controller.http.readstate.dto.UpdateChannelReadStateRequest;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.readstate.MessageReadStateApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.readstate.result.ChannelReadStateResult;

/**
 * 频道读状态 HTTP 入口。
 */
@RestController
@RequestMapping("/api")
@PreAuthorize("isAuthenticated()")
@Tag(name = "频道读状态", description = "频道已读状态与未读计数能力。")
public class ChannelReadStateController {

    private final MessageReadStateApi messageReadStateApi;

    public ChannelReadStateController(
        MessageReadStateApi messageReadStateApi
    ) {
        this.messageReadStateApi = messageReadStateApi;
    }

    /**
     * 更新频道已读状态
     *
     * <p>请求 {@code PUT /api/channels/2001/read_state}。JSON 请求体示例 {@code {"last_read_mid":"3001"}}。
     * <p>成功响应 HTTP 200，code 为 0，data 为 {@code ChannelReadStateResponse}。
     * <p>失败示例：目标消息不存在 30001。
     */
    @PutMapping(value = "/channels/{channelId}/read_state", produces = "application/json", consumes = {"application/json", "application/*+json"})
    @Operation(summary = "更新频道已读状态", description = "只前进不后退。")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "返回已读状态", useReturnTypeSchema = true))
    public Result<ChannelReadStateResponse> updateReadState(
        @PathVariable
        @Positive(message = "channelId must be greater than 0")
        long channelId,
        @Valid
        @RequestBody
        UpdateChannelReadStateRequest request,
        @AuthenticationPrincipal
        CpPrincipal principal
    ) {
        ChannelReadStateResult result = messageReadStateApi.updateChannelReadState(
            principal.accountId(),
            channelId,
            request.lastReadMid()
        );
        return Result.success(new ChannelReadStateResponse(
            Long.toString(result.channelId()),
            Long.toString(result.accountId()),
            Long.toString(result.lastReadMessageId())
        ));
    }

    /**
     * 查询当前用户的频道未读聚合。
     *
     * <p>请求 {@code GET /api/unreads}。无请求体。
     * <p>成功响应 HTTP 200，code 为 0，data 为 {@code UnreadListResponse}。
     * <p>失败示例：缺少认证 11001。
     */
    @GetMapping(value = "/unreads", produces = "application/json")
    @Operation(summary = "获取未读频道列表", description = "返回当前用户的频道未读聚合；不足 10000 精确统计，达到上限显示 10000+。排除本人及已撤回消息。")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "返回未读列表", useReturnTypeSchema = true))
    public Result<UnreadListResponse> listUnreads(
        @AuthenticationPrincipal
        CpPrincipal principal
    ) {
        List<UnreadItemResponse> items = messageReadStateApi.listUnreads(principal.accountId()).stream()
            .map(result -> new UnreadItemResponse(Long.toString(result.channelId()),
                    result.unreadCount() >= 10000 ? "10000+" : Long.toString(result.unreadCount())))
            .toList();
        return Result.success(new UnreadListResponse(items));
    }
}
