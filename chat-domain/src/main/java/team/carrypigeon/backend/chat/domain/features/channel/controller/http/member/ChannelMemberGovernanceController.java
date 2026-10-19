package team.carrypigeon.backend.chat.domain.features.channel.controller.http.member;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import team.carrypigeon.backend.chat.domain.config.http.security.CpPrincipal;
import team.carrypigeon.backend.chat.domain.features.channel.controller.http.member.dto.ChannelMemberListResponse;
import team.carrypigeon.backend.chat.domain.features.channel.controller.http.member.dto.ChannelMemberResponse;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.member.ChannelMemberApi;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.member.input.DemoteChannelAdminCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.member.input.KickChannelMemberCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.member.input.ListChannelMembersQuery;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.member.input.PromoteChannelMemberCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.member.result.ChannelMemberResult;
import team.carrypigeon.backend.chat.domain.shared.controller.error.ApiErrorResponse;
import team.carrypigeon.backend.infrastructure.basic.id.IdUtil;

/**
 * 频道成员治理 HTTP 入口。
 * 职责：承接频道成员列表读取、管理员调整与成员移除协议。
 * 边界：只做协议映射与领域 API 调用，不承载成员角色规则，也不处理频道生命周期与禁言。
 */
@RestController
@RequestMapping("/api/channels")
@PreAuthorize("isAuthenticated()")
@Validated
@Tag(name = "频道成员", description = "频道成员查询与治理能力。")
public class ChannelMemberGovernanceController {

    private final ChannelMemberApi channelMembershipApi;

    public ChannelMemberGovernanceController(
            ChannelMemberApi channelMembershipApi
    ) {
        this.channelMembershipApi = channelMembershipApi;
    }

    /**
     * 查询指定频道的成员列表。
     * 约束：调用方必须是该频道成员。
     *
     * @param channelId 目标频道 ID
     * @param cpPrincipal 当前认证用户
     * @return 频道成员列表响应
     */
    @GetMapping("/{channelId}/members")
    @Operation(
            summary = "查询频道成员",
            description = "返回指定频道的成员列表。成员资料缺失时对应用户名与头像字段为空，不影响列表返回。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "响应体为成员列表外壳，items 为成员数组，含 uid、username、role、avatar、join_time",
                    content = @Content(schema = @Schema(implementation = ChannelMemberListResponse.class))),
            @ApiResponse(responseCode = "401", description = "未认证，响应体为标准错误包装",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "当前账号不是该频道成员，reason 为 not_channel_member",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "频道不存在",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ChannelMemberListResponse listChannelMembers(
            @Parameter(description = "目标频道 ID", example = "2001")
            @PathVariable @Positive(message = "channelId must be greater than 0") long channelId,
            @AuthenticationPrincipal CpPrincipal cpPrincipal
    ) {
        List<ChannelMemberResult> result = channelMembershipApi.listChannelMembers(
                new ListChannelMembersQuery(cpPrincipal.accountId(), channelId)
        );
        return new ChannelMemberListResponse(result.stream().map(
            res -> new ChannelMemberResponse(
                IdUtil.toString(res.accountId()),
                res.username(),
                res.role().toLowerCase(),
                res.avatarUrl(),
                res.joinedAt()
            )
        ).toList());
    }

    /**
     * 将指定成员设为管理员。
     * 约束：调用方必须具备该频道 OWNER 成员角色，且目标成员当前角色为 MEMBER。
     * 副作用：更新目标成员的频道角色为 ADMIN，并在事务提交后通知频道成员。
     *
     * @param channelId 目标频道 ID
     * @param targetAccountId 目标成员账户 ID
     * @param cpPrincipal 当前认证用户
     * @return 无内容响应，HTTP 204
     */
    @PutMapping("/{channelId}/admins/{targetAccountId}")
    @Operation(
            summary = "设为管理员",
            description = "将指定频道成员的角色提升为 ADMIN，要求 OWNER 成员角色。请求体为空，成功返回 204。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "管理员设置成功，响应体为空"),
            @ApiResponse(responseCode = "401", description = "未认证，响应体为标准错误包装",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "操作者或目标不是频道成员，或操作者不是频道所有者，reason 为 not_channel_member 或 channel_role_forbidden",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "频道不存在，reason 为 not_found",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = "目标成员当前角色不是 MEMBER，reason 为 validation_failed",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<Void> promoteChannelMemberV1(
            @Parameter(description = "目标频道 ID", example = "2001")
            @PathVariable @Positive(message = "channelId must be greater than 0") long channelId,
            @Parameter(description = "目标成员账户 ID", example = "1002")
            @PathVariable @Positive(message = "targetAccountId must be greater than 0") long targetAccountId,
            @AuthenticationPrincipal CpPrincipal cpPrincipal
    ) {
        channelMembershipApi.promoteChannelMember(
                new PromoteChannelMemberCommand(cpPrincipal.accountId(), channelId, targetAccountId)
        );
        return ResponseEntity.noContent().build();
    }

    /**
     * 撤销指定成员的管理员角色。
     * 约束：调用方必须具备该频道 OWNER 成员角色，且目标成员当前角色为 ADMIN。
     * 副作用：更新目标成员的频道角色为 MEMBER，并在事务提交后通知频道成员。
     *
     * @param channelId 目标频道 ID
     * @param targetAccountId 目标成员账户 ID
     * @param cpPrincipal 当前认证用户
     * @return 无内容响应，HTTP 204
     */
    @DeleteMapping("/{channelId}/admins/{targetAccountId}")
    @Operation(
            summary = "撤销管理员",
            description = "将指定频道成员的角色降级为 MEMBER，要求 OWNER 成员角色。请求体为空，成功返回 204。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "管理员角色撤销成功，响应体为空"),
            @ApiResponse(responseCode = "401", description = "未认证，响应体为标准错误包装",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "操作者或目标不是频道成员，或操作者不是频道所有者，reason 为 not_channel_member 或 channel_role_forbidden",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "频道不存在，reason 为 not_found",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = "目标成员当前角色不是 ADMIN，reason 为 validation_failed",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<Void> demoteChannelAdminV1(
            @Parameter(description = "目标频道 ID", example = "2001")
            @PathVariable @Positive(message = "channelId must be greater than 0") long channelId,
            @Parameter(description = "目标成员账户 ID", example = "1002")
            @PathVariable @Positive(message = "targetAccountId must be greater than 0") long targetAccountId,
            @AuthenticationPrincipal CpPrincipal cpPrincipal
    ) {
        channelMembershipApi.demoteChannelAdmin(
                new DemoteChannelAdminCommand(cpPrincipal.accountId(), channelId, targetAccountId)
        );
        return ResponseEntity.noContent().build();
    }

    /**
     * 将指定成员移出频道。
     * 约束：调用方必须具备该频道 OWNER 或 ADMIN 成员角色，且目标成员当前角色为 MEMBER。
     * 副作用：删除目标成员的频道成员关系，并在事务提交后通知频道成员与目标账号。
     *
     * @param channelId 目标频道 ID
     * @param targetAccountId 目标成员账户 ID
     * @param cpPrincipal 当前认证用户
     * @return 无内容响应，HTTP 204
     */
    @DeleteMapping("/{channelId}/members/{targetAccountId}")
    @Operation(
            summary = "踢出频道成员",
            description = "删除指定成员的频道成员关系，要求 OWNER 或 ADMIN 成员角色。请求体为空，成功返回 204。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "成员移除成功，响应体为空"),
            @ApiResponse(responseCode = "401", description = "未认证，响应体为标准错误包装",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "非频道成员或角色不足，reason 为 not_channel_member 或 channel_kick_forbidden",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "频道不存在或目标成员不存在",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = "目标成员当前角色不是 MEMBER，reason 为 validation_failed",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<Void> kickChannelMember(
            @Parameter(description = "目标频道 ID", example = "2001")
            @PathVariable @Positive(message = "channelId must be greater than 0") long channelId,
            @Parameter(description = "目标成员账户 ID", example = "1002")
            @PathVariable @Positive(message = "targetAccountId must be greater than 0") long targetAccountId,
            @AuthenticationPrincipal CpPrincipal cpPrincipal
    ) {
        channelMembershipApi.kickChannelMember(
                new KickChannelMemberCommand(cpPrincipal.accountId(), channelId, targetAccountId)
        );
        return ResponseEntity.noContent().build();
    }
}
