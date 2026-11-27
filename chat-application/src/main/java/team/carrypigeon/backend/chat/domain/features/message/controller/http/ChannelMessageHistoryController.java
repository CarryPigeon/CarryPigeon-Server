package team.carrypigeon.backend.chat.domain.features.message.controller.http;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import org.hibernate.validator.constraints.Range;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import team.carrypigeon.backend.chat.domain.shared.result.Result;
import team.carrypigeon.backend.chat.domain.http.security.CpPrincipal;
import team.carrypigeon.backend.chat.domain.features.message.controller.http.dto.ChannelMessageContextResponse;
import team.carrypigeon.backend.chat.domain.features.message.controller.http.dto.ChannelMessageHistoryResponse;
import team.carrypigeon.backend.chat.domain.features.message.controller.http.dto.ChannelMessageResponse;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.ChannelMessageContextApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.ChannelMessageHistoryApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.input.GetChannelMessagesAroundQuery;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.input.ListChannelMessagesQuery;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.result.ChannelMessageContext;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.result.ChannelMessagePage;

/**
 * 频道消息历史 HTTP 入口，提供频道历史分页和指定消息的锚点上下文查询。
 */
@RestController
@RequestMapping("/api/channels")
@PreAuthorize("isAuthenticated()")
@Tag(name = "频道消息", description = "频道消息历史查询。")
public class ChannelMessageHistoryController {

    private final ChannelMessageHistoryApi channelMessageHistoryApi;
    private final ChannelMessageContextApi channelMessageContextApi;

    public ChannelMessageHistoryController(
            ChannelMessageHistoryApi channelMessageHistoryApi,
            ChannelMessageContextApi channelMessageContextApi
    ) {
        this.channelMessageHistoryApi = channelMessageHistoryApi;
        this.channelMessageContextApi = channelMessageContextApi;
    }

    /**
     * 按可选的消息 ID 上界查询频道历史。
     *
     * <p>请求 {@code GET /api/channels/2001/messages?limit=20}。无请求体。
     * <p>成功响应 HTTP 200，code 为 0，data 为 {@code ChannelMessageHistoryResponse}。
     * <p>失败示例：非成员 20002。
     *
     * @param channelId 频道 ID
     * @param beforeMessageId 排他消息 ID 上界，为 null 时返回最新一页
     * @param limit 返回条数
     * @param cpPrincipal 当前认证用户
     */
    @GetMapping(value = "/{channelId}/messages", produces = "application/json")
    @Operation(summary = "读取频道历史消息", description = "按消息 ID 降序返回，传入 before_mid 时只读取比它更旧的消息。")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "返回 items 和 has_more 分页对象", useReturnTypeSchema = true))
    public Result listMessages(
        @Parameter(description = "目标频道 ID", example = "2001")
        @PathVariable
        @Positive(message = "channelId must be greater than 0")
        long channelId,
        @Parameter(description = "排他消息 ID 上界，省略时从最新消息开始", example = "3001")
        @RequestParam(name = "before_mid", required = false)
        @Positive(message = "before_mid must be greater than 0")
        Long beforeMessageId,
        @Parameter(description = "返回条数，范围 1..50", example = "20")
        @RequestParam(defaultValue = "20")
        @Range(min = 1, max = 50, message = "limit must be between 1 and 50")
        int limit,
        @AuthenticationPrincipal
        CpPrincipal cpPrincipal
    ) {
        ChannelMessagePage page = channelMessageHistoryApi.listMessages(
            cpPrincipal.accountId(), new ListChannelMessagesQuery(channelId, beforeMessageId, limit)
        );
        return Result.success(new ChannelMessageHistoryResponse(
            page.items().stream().map(ChannelMessageResponse::from).toList(),
            page.hasMore()
        ));
    }

    /**
     * 查询锚点消息前后的上下文窗口。
     *
     * <p>请求 {@code GET /api/channels/2001/messages/around?anchor_mid=3001}。无请求体。
     * <p>成功响应 HTTP 200，code 为 0，data 为 {@code ChannelMessageContextResponse}。
     * <p>失败示例：锚点消息不存在 30001。
     *
     * @param channelId 频道 ID
     * @param anchorMid 锚点消息 ID
     * @param before 锚点之前数量
     * @param after 锚点之后数量
     * @param cpPrincipal 当前认证用户
     */
    @GetMapping(value = "/{channelId}/messages/around", produces = "application/json")
    @Operation(summary = "读取锚点上下文", description = "返回锚点消息前后的固定窗口消息，结果按消息 ID 倒序排列。")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "返回 items 上下文消息数组", useReturnTypeSchema = true))
    public Result getMessagesAround(
        @Parameter(description = "目标频道 ID", example = "2001")
        @PathVariable
        @Positive(message = "channelId must be greater than 0")
        long channelId,
        @Parameter(description = "锚点消息 ID", example = "5000")
        @RequestParam(name = "anchor_mid")
        @Positive(message = "anchor_mid must be greater than 0")
        long anchorMid,
        @Parameter(description = "锚点之前数量，范围 0..50", example = "25")
        @RequestParam(defaultValue = "25")
        @Range(min = 0, max = 50, message = "before must be between 0 and 50")
        int before,
        @Parameter(description = "锚点之后数量，范围 0..50", example = "25")
        @RequestParam(defaultValue = "25")
        @Range(min = 0, max = 50, message = "before must be between 0 and 50")
        int after,
        @AuthenticationPrincipal
        CpPrincipal cpPrincipal
    ) {
        ChannelMessageContext context = channelMessageContextApi.getMessagesAround(
            new GetChannelMessagesAroundQuery(
                cpPrincipal.accountId(),
                channelId,
                anchorMid,
                before,
                after
            )
        );
        return Result.success(new ChannelMessageContextResponse(
            context.items().stream().map(ChannelMessageResponse::from).toList()
        ));
    }
}
