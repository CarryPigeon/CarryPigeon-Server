package team.carrypigeon.backend.chat.domain.features.message.controller.http.readstate;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

import team.carrypigeon.backend.chat.domain.shared.result.Result;
import team.carrypigeon.backend.chat.domain.http.security.CpPrincipal;
import team.carrypigeon.backend.chat.domain.features.message.controller.http.readstate.dto.ChannelReadStateResponse;
import team.carrypigeon.backend.chat.domain.features.message.controller.http.readstate.dto.ChannelReadSummaryResponse;
import team.carrypigeon.backend.chat.domain.features.message.controller.http.readstate.dto.UnreadItemResponse;
import team.carrypigeon.backend.chat.domain.features.message.controller.http.readstate.dto.UnreadListResponse;
import team.carrypigeon.backend.chat.domain.features.message.controller.http.readstate.dto.UpdateChannelReadStateRequest;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.readstate.MessageReadStateApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.readstate.result.ChannelReadStateResult;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.readstate.result.ChannelReadSummaryResult;

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
     * 读取当前成员在一个频道的阅读位置和未读数。
     * <p>GET /api/channels/2001/read_state，无请求体。成功时返回 HTTP 200，
     * data 示例 {@code {"cid":"2001","uid":"1001","last_read_mid":null,"unread_count":3}}。
     * 频道不存在返回 20001，非成员返回 20002。
     */
    @GetMapping(value = "/channels/{channelId}/read_state", produces = "application/json")
    @Operation(summary = "读取频道已读位置和未读数", description = "入群时以最新消息初始化锚点，空频道从 null 开始；统计排除本人和已撤回的消息。")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "成功或业务错误 10001、11001、20001、20002", useReturnTypeSchema = true))
    public Result getReadState(
        @Parameter(description = "频道 ID", example = "2001")
        @PathVariable
        @Positive
        long channelId,
        @AuthenticationPrincipal
        CpPrincipal principal
    ) {
        ChannelReadSummaryResult result = messageReadStateApi.getChannelReadState(principal.accountId(), channelId);
        return Result.success(new ChannelReadSummaryResponse(
            Long.toString(result.channelId()),
            Long.toString(result.accountId()),
            result.lastReadMessageId() == null ? null : result.lastReadMessageId().toString(),
            result.unreadCount()
        ));
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
    public Result updateReadState(
        @Parameter(description = "频道 ID", example = "2001")
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
    @Operation(summary = "获取未读频道列表", description = "只返回有未读消息的已加入频道，total 为频道数。每个频道最多计至 10000，排除本人发送及已撤回的消息。")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "返回未读列表", useReturnTypeSchema = true))
    public Result listUnreads(
        @AuthenticationPrincipal
        CpPrincipal principal
    ) {
        List<UnreadItemResponse> items = messageReadStateApi.listUnreads(principal.accountId()).stream()
            .map(result -> new UnreadItemResponse(Long.toString(result.channelId()),
                    result.unreadCount()))
            .toList();
        return Result.success(new UnreadListResponse(items.size(),items));
    }
}
