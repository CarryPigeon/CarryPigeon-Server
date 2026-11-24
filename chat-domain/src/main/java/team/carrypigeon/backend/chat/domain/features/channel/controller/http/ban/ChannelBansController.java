package team.carrypigeon.backend.chat.domain.features.channel.controller.http.ban;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import team.carrypigeon.backend.chat.domain.shared.page.PageResponse;
import team.carrypigeon.backend.chat.domain.shared.result.Result;
import team.carrypigeon.backend.chat.domain.http.security.CpPrincipal;
import team.carrypigeon.backend.chat.domain.features.channel.controller.http.ban.dto.ChannelBanListItemResponse;
import team.carrypigeon.backend.chat.domain.features.channel.controller.http.ban.dto.ChannelBanRequest;
import team.carrypigeon.backend.chat.domain.features.channel.controller.http.ban.dto.ChannelBanResponse;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.ban.ChannelBanApi;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.ban.input.BanChannelMemberUntilCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.ban.result.ChannelBanListItemResult;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.ban.result.ChannelBanResult;
import team.carrypigeon.backend.chat.domain.shared.id.IdUtil;
import team.carrypigeon.backend.chat.domain.shared.page.PageQuery;
import team.carrypigeon.backend.chat.domain.shared.page.PageResult;

/**
 * 频道禁言 HTTP 入口。
 * 职责：承接频道成员禁言、解除禁言与禁言列表协议。
 * 边界：只做协议映射与领域 API 调用，不承载禁言时长、权限和系统频道规则。
 */
@RestController
@RequestMapping("/api/channels")
@PreAuthorize("isAuthenticated()")
@Tag(name = "频道禁言", description = "频道成员禁言与解禁能力。")
public class ChannelBansController {

    private final ChannelBanApi channelBanApi;

    public ChannelBansController(
            ChannelBanApi channelBanApi
    ) {
        this.channelBanApi = channelBanApi;
    }

    /**
     * 对指定频道成员设置禁言。
     * 约束：调用方必须具备该频道 OWNER 或 ADMIN 成员角色，且目标成员角色为 MEMBER。
     * 副作用：写入或更新一条禁言记录，并在事务提交后通知频道成员。
     *
     * <p>请求 {@code PUT /api/channels/2001/bans/1002}。JSON 请求体示例 {@code {"until":1893456000000,"reason":"spam"}}。
     * <p>成功响应 HTTP 200，code 为 0，data 为 {@code ChannelBanResponse}。
     * <p>失败示例：角色不足 20011，截止时间无效 10001。
     *
     * @param channelId 目标频道 ID
     * @param targetAccountId 目标成员账户 ID
     * @param body 禁言请求，until 必填且必须大于当前时间
     * @param cpPrincipal 当前认证用户
     */
    @PutMapping(value = "/{channelId}/bans/{targetAccountId}", produces = "application/json", consumes = {"application/json", "application/*+json"})
    @Operation(
            summary = "禁言频道成员",
            description = "对指定频道成员设置禁言：until 必填且必须大于当前时间；已有未解除且未到期的禁言时拒绝重复设置。",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "请求体含 until 与 reason：until 为必填 epoch 毫秒时间戳且必须大于当前时间；"
                            + "reason 允许空字符串，最长 256 字符",
                    content = @Content(schema = @Schema(implementation = ChannelBanRequest.class))
            )
    )
    @ApiResponses(@ApiResponse(responseCode = "200", description = "data 为禁言记录，含 cid、uid、until、reason、create_time", useReturnTypeSchema = true))
    public Result banChannelMemberV1(
        @Parameter(description = "目标频道 ID", example = "2001")
        @PathVariable
        @Positive(message = "channelId must be greater than 0")
        long channelId,
        @Parameter(description = "目标成员账户 ID", example = "1002")
        @PathVariable
        @Positive(message = "targetAccountId must be greater than 0")
        long targetAccountId,
        @Valid
        @RequestBody
        ChannelBanRequest body,
        @AuthenticationPrincipal
        CpPrincipal cpPrincipal
    ) {
        ChannelBanResult result = channelBanApi.banChannelMemberUntil(
                cpPrincipal.accountId(),
                new BanChannelMemberUntilCommand(
                        channelId,
                        targetAccountId,
                        body.reason(),
                        body.until()
                )
        );
        return Result.success(new ChannelBanResponse(
                IdUtil.toString(result.channelId()),
                IdUtil.toString(result.bannedAccountId()),
                result.expiresAt() == null ? null : result.expiresAt().toEpochMilli(),
                result.reason(),
                result.createdAt().toEpochMilli()
        ));
    }

    /**
     * 解除指定频道成员的禁言。
     * 约束：调用方必须具备该频道 OWNER 或 ADMIN 成员角色，且目标禁言记录处于生效状态。
     * 副作用：标记禁言记录为已解除，并在事务提交后通知频道成员。
     *
     * <p>请求 {@code DELETE /api/channels/2001/bans/1002}。无请求体。
     * <p>成功响应 HTTP 200，code 为 0，data 为 {@code null}。
     * <p>失败示例：禁言不存在 20004。
     *
     * @param channelId 目标频道 ID
     * @param targetAccountId 目标成员账户 ID
     * @param cpPrincipal 当前认证用户
     */
    @DeleteMapping(value = "/{channelId}/bans/{targetAccountId}", produces = "application/json")
    @Operation(
            summary = "解除频道成员禁言",
            description = "解除指定频道成员当前生效的禁言状态。成功返回 200，data 为 null。"
    )
    @ApiResponses(@ApiResponse(responseCode = "200", description = "成功时 code 为 0 且 data 为 null", useReturnTypeSchema = true))
    public Result unbanChannelMember(
        @Parameter(description = "目标频道 ID", example = "2001")
        @PathVariable
        @Positive(message = "channelId must be greater than 0")
        long channelId,
        @Parameter(description = "目标成员账户 ID", example = "1002")
        @PathVariable
        @Positive(message = "targetAccountId must be greater than 0")
        long targetAccountId,
        @AuthenticationPrincipal
        CpPrincipal cpPrincipal
    ) {
        channelBanApi.unbanChannelMember(cpPrincipal.accountId(), channelId, targetAccountId);
        return Result.success();
    }

    /**
     * 返回指定频道的禁言列表。
     * 约束：调用方必须具备该频道 OWNER 或 ADMIN 成员角色。
     *
     * <p>请求 {@code GET /api/channels/2001/bans?page=1&page_size=20}。无请求体。
     * <p>成功响应 HTTP 200，code 为 0，data 为 {@code PageResponse<ChannelBanListItemResponse>}。
     * <p>失败示例：角色不足 20011。
     *
     * @param channelId 目标频道 ID
     * @param cpPrincipal 当前认证用户
     */
    @GetMapping(value = "/{channelId}/bans", produces = "application/json")
    @Operation(
            summary = "获取禁言列表",
            description = "分页返回指定频道的禁言记录列表。列表包含已解除与已过期的历史记录，由调用方按 until 自行过滤。"
    )
    @ApiResponses(@ApiResponse(responseCode = "200", description = "data 为分页外壳，items 为禁言记录数组", useReturnTypeSchema = true))
    public Result listChannelBans(
        @Parameter(description = "目标频道 ID", example = "2001")
        @PathVariable
        @Positive(message = "channelId must be greater than 0")
        long channelId,
        @Parameter(description = "页码，从 1 开始", example = "1")
        @RequestParam(name = "page", defaultValue = "1")
        @Min(value = 1, message = "page must be greater than 0")
        int page,
        @Parameter(description = "每页条数，范围 1..50", example = "20")
        @RequestParam(name = "page_size", defaultValue = "20")
        @Min(value = 1, message = "page_size must be between 1 and 50")
        @Max(value = 50, message = "page_size must be between 1 and 50")
        int pageSize,
        @AuthenticationPrincipal
        CpPrincipal cpPrincipal
    ) {
        PageResult<ChannelBanListItemResult> result = channelBanApi.listChannelBans(
                cpPrincipal.accountId(),
                channelId,
                new PageQuery(page, pageSize)
        );
        return Result.success(new PageResponse<>(
                result.items().stream().map(this::toChannelBanListItemResponse).toList(),
                result.page(),
                result.pageSize(),
                result.total(),
                result.hasMore()
        ));
    }

    private ChannelBanListItemResponse toChannelBanListItemResponse(ChannelBanListItemResult result) {
        return new ChannelBanListItemResponse(
                IdUtil.toString(result.channelId()),
                IdUtil.toString(result.bannedAccountId()),
                result.expiresAt() == null ? null : result.expiresAt().toEpochMilli(),
                result.reason(),
                result.createdAt().toEpochMilli()
        );
    }
}
