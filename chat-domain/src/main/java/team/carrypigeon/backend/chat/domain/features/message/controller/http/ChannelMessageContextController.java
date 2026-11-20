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

import team.carrypigeon.backend.chat.domain.config.http.Result;
import team.carrypigeon.backend.chat.domain.config.http.security.CpPrincipal;
import team.carrypigeon.backend.chat.domain.features.message.controller.http.dto.ChannelMessageContextResponse;
import team.carrypigeon.backend.chat.domain.features.message.controller.http.dto.ChannelMessageResponse;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.ChannelMessageContextApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.input.GetChannelMessagesAroundQuery;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.result.ChannelMessageContext;

/**
 * 频道消息锚点上下文 HTTP 入口。
 * 职责：提供指定锚点消息前后的固定窗口查询。
 * 边界：不承载方向翻页或搜索。
 */
@RestController
@RequestMapping("/api/channels")
@PreAuthorize("isAuthenticated()")
@Tag(name = "频道消息", description = "频道消息锚点上下文能力。")
public class ChannelMessageContextController {

    private final ChannelMessageContextApi channelMessageContextApi;

    public ChannelMessageContextController(ChannelMessageContextApi channelMessageContextApi) {
        this.channelMessageContextApi = channelMessageContextApi;
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
    @Operation(summary = "读取锚点上下文", description = "返回锚点消息前后的固定窗口消息，结果按频道序号倒序排列。")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "返回 items 上下文消息数组", useReturnTypeSchema = true))
    public Result<ChannelMessageContextResponse> getMessagesAround(
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
