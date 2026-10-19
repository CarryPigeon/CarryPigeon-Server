package team.carrypigeon.backend.chat.domain.features.channel.controller.http.ban;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import team.carrypigeon.backend.chat.domain.config.http.security.CpPrincipal;
import team.carrypigeon.backend.chat.domain.features.channel.controller.http.ban.dto.ChannelBanListItemResponse;
import team.carrypigeon.backend.chat.domain.features.channel.controller.http.ban.dto.ChannelBanListResponse;
import team.carrypigeon.backend.chat.domain.features.channel.controller.http.ban.dto.ChannelBanRequest;
import team.carrypigeon.backend.chat.domain.features.channel.controller.http.ban.dto.ChannelBanResponse;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.ban.ChannelBanApi;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.ban.input.BanChannelMemberUntilCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.ban.input.UnbanChannelMemberCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.ban.result.ChannelBanListItemResult;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.ban.result.ChannelBanResult;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.ban.input.ListChannelBansQuery;
import team.carrypigeon.backend.chat.domain.shared.controller.error.ApiErrorResponse;
import team.carrypigeon.backend.infrastructure.basic.id.IdUtil;

/**
 * 频道禁言 HTTP 入口。
 * 职责：承接频道成员禁言、解除禁言与禁言列表协议。
 * 边界：只做协议映射与领域 API 调用，不承载禁言时长、权限和系统频道规则。
 */
@RestController
@RequestMapping("/api/channels")
@PreAuthorize("isAuthenticated()")
@Validated
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
     * @param channelId 目标频道 ID
     * @param targetAccountId 目标成员账户 ID
     * @param body 禁言请求，until 为空表示不设置自动到期
     * @param cpPrincipal 当前认证用户
     * @return 禁言结果
     */
    @PutMapping("/{channelId}/bans/{targetAccountId}")
    @Operation(
            summary = "禁言频道成员",
            description = "对指定频道成员设置禁言。until 为空表示不设置自动到期；已有未解除且未到期的禁言时拒绝重复设置。",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "请求体含 until 与 reason：until 为 epoch 毫秒且必须大于当前时间，"
                            + "省略或为空表示不设置自动到期；reason 允许空字符串，最长 256 字符",
                    content = @Content(schema = @Schema(implementation = ChannelBanRequest.class))
            )
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "响应体为禁言记录，含 cid、uid、until、reason、create_time",
                    content = @Content(schema = @Schema(implementation = ChannelBanResponse.class))),
            @ApiResponse(responseCode = "401", description = "未认证，响应体为标准错误包装",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "非频道成员或角色不足，reason 为 not_channel_member 或 channel_ban_forbidden",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "频道不存在，reason 为 not_found",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = "请求体字段非法、until 不大于当前时间、目标成员当前角色不是 MEMBER，"
                    + "或该成员已有生效中的禁言，reason 为 validation_failed",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<ChannelBanResponse> banChannelMemberV1(
            @Parameter(description = "目标频道 ID", example = "2001")
            @PathVariable @Positive(message = "channelId must be greater than 0") long channelId,
            @Parameter(description = "目标成员账户 ID", example = "1002")
            @PathVariable @Positive(message = "targetAccountId must be greater than 0") long targetAccountId,
            @Valid @RequestBody ChannelBanRequest body,
            @AuthenticationPrincipal CpPrincipal cpPrincipal
    ) {
        ChannelBanResult result = channelBanApi.banChannelMemberUntil(
                new BanChannelMemberUntilCommand(
                        cpPrincipal.accountId(),
                        channelId,
                        targetAccountId,
                        body.reason(),
                        body.until()
                )
        );
        return ResponseEntity.ok(new ChannelBanResponse(
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
     * @param channelId 目标频道 ID
     * @param targetAccountId 目标成员账户 ID
     * @param cpPrincipal 当前认证用户
     * @return 无内容响应，HTTP 204
     */
    @DeleteMapping("/{channelId}/bans/{targetAccountId}")
    @Operation(
            summary = "解除频道成员禁言",
            description = "解除指定频道成员当前生效的禁言状态。成功返回 204，响应体为空。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "解除禁言成功，响应体为空"),
            @ApiResponse(responseCode = "401", description = "未认证，响应体为标准错误包装",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "非频道成员或角色不足，reason 为 not_channel_member 或 channel_ban_forbidden",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "频道不存在或禁言记录不存在",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = "禁言记录当前不处于生效状态，reason 为 validation_failed",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<Void> unbanChannelMember(
            @Parameter(description = "目标频道 ID", example = "2001")
            @PathVariable @Positive(message = "channelId must be greater than 0") long channelId,
            @Parameter(description = "目标成员账户 ID", example = "1002")
            @PathVariable @Positive(message = "targetAccountId must be greater than 0") long targetAccountId,
            @AuthenticationPrincipal CpPrincipal cpPrincipal
    ) {
        channelBanApi.unbanChannelMember(
                new UnbanChannelMemberCommand(cpPrincipal.accountId(), channelId, targetAccountId)
        );
        return ResponseEntity.noContent().build();
    }

    /**
     * 返回指定频道的禁言列表。
     * 约束：调用方必须具备该频道 OWNER 或 ADMIN 成员角色。
     *
     * @param channelId 目标频道 ID
     * @param cpPrincipal 当前认证用户
     * @return 禁言列表响应
     */
    @GetMapping("/{channelId}/bans")
    @Operation(
            summary = "获取禁言列表",
            description = "返回指定频道的禁言记录列表。列表包含已解除与已过期的历史记录，由调用方按 until 自行过滤。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "响应体为禁言列表外壳，items 为禁言记录数组",
                    content = @Content(schema = @Schema(implementation = ChannelBanListResponse.class))),
            @ApiResponse(responseCode = "401", description = "未认证，响应体为标准错误包装",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "非频道成员或角色不足，reason 为 not_channel_member 或 channel_ban_forbidden",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "频道不存在",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ChannelBanListResponse listChannelBans(
            @Parameter(description = "目标频道 ID", example = "2001")
            @PathVariable @Positive(message = "channelId must be greater than 0") long channelId,
            @AuthenticationPrincipal CpPrincipal cpPrincipal
    ) {
        return new ChannelBanListResponse(channelBanApi.listChannelBans(
                new ListChannelBansQuery(cpPrincipal.accountId(), channelId)
        ).stream().map(this::toChannelBanListItemResponse).toList());
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
