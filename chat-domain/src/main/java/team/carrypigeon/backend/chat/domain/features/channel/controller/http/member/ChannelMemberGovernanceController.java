package team.carrypigeon.backend.chat.domain.features.channel.controller.http.member;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import team.carrypigeon.backend.chat.domain.shared.page.PageResponse;
import team.carrypigeon.backend.chat.domain.shared.result.Result;
import team.carrypigeon.backend.chat.domain.http.security.CpPrincipal;
import team.carrypigeon.backend.chat.domain.features.channel.controller.http.member.dto.ChannelMemberResponse;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.member.ChannelMemberApi;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.member.result.ChannelMemberResult;
import team.carrypigeon.backend.chat.domain.shared.id.IdUtil;
import team.carrypigeon.backend.chat.domain.shared.page.PageQuery;
import team.carrypigeon.backend.chat.domain.shared.page.PageResult;

/**
 * 频道成员治理 HTTP 入口。
 * 职责：承接频道成员列表读取、管理员调整与成员移除协议。
 * 边界：只做协议映射与领域 API 调用，不承载成员角色规则，也不处理频道生命周期与禁言。
 */
@RestController
@RequestMapping("/api/channels")
@PreAuthorize("isAuthenticated()")
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
     * <p>请求 {@code GET /api/channels/2001/members?page=1&page_size=20}。无请求体。
     * <p>成功响应 HTTP 200，code 为 0，data 为 {@code PageResponse<ChannelMemberResponse>}。
     * <p>失败示例：非成员 20002。
     *
     * @param channelId 目标频道 ID
     * @param cpPrincipal 当前认证用户
     */
    @GetMapping(value = "/{channelId}/members", produces = "application/json")
    @Operation(
            summary = "查询频道成员",
            description = "分页返回指定频道的成员列表。成员资料缺失时对应用户名与头像字段为空，不影响列表返回。"
    )
    @ApiResponses(@ApiResponse(responseCode = "200", description = "data 为分页外壳，items 为成员数组，含 uid、username、role、avatar、join_time", useReturnTypeSchema = true))
    public Result listChannelMembers(
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
        PageResult<ChannelMemberResult> result = channelMembershipApi.listChannelMembers(
                cpPrincipal.accountId(),
                channelId,
                new PageQuery(page, pageSize)
        );
        return Result.success(new PageResponse<>(
                result.items().stream().map(
                        res -> new ChannelMemberResponse(
                                IdUtil.toString(res.accountId()),
                                res.username(),
                                res.role().toLowerCase(),
                                res.avatarUrl(),
                                res.joinedAt()
                        )
                ).toList(),
                result.page(),
                result.pageSize(),
                result.total(),
                result.hasMore()
        ));
    }

    /**
     * 将指定成员设为管理员。
     * 约束：调用方必须具备该频道 OWNER 成员角色，且目标成员当前角色为 MEMBER。
     * 副作用：更新目标成员的频道角色为 ADMIN，并在事务提交后通知频道成员。
     *
     * <p>请求 {@code PUT /api/channels/2001/admins/1002}。无请求体。
     * <p>成功响应 HTTP 200，code 为 0，data 为 {@code null}。
     * <p>失败示例：角色不足 20007，治理冲突 20015。
     *
     * @param channelId 目标频道 ID
     * @param targetAccountId 目标成员账户 ID
     * @param cpPrincipal 当前认证用户
     */
    @PutMapping(value = "/{channelId}/admins/{targetAccountId}", produces = "application/json")
    @Operation(
            summary = "设为管理员",
            description = "将指定频道成员的角色提升为 ADMIN，要求 OWNER 成员角色。请求体为空，成功时 data 为 null。"
    )
    @ApiResponses(@ApiResponse(responseCode = "200", description = "成功时 code 为 0 且 data 为 null", useReturnTypeSchema = true))
    public Result promoteChannelMemberV1(
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
        channelMembershipApi.promoteChannelMember(cpPrincipal.accountId(), channelId, targetAccountId);
        return Result.success();
    }

    /**
     * 撤销指定成员的管理员角色。
     * 约束：调用方必须具备该频道 OWNER 成员角色，且目标成员当前角色为 ADMIN。
     * 副作用：更新目标成员的频道角色为 MEMBER，并在事务提交后通知频道成员。
     *
     * <p>请求 {@code DELETE /api/channels/2001/admins/1002}。无请求体。
     * <p>成功响应 HTTP 200，code 为 0，data 为 {@code null}。
     * <p>失败示例：角色不足 20007，治理冲突 20015。
     *
     * @param channelId 目标频道 ID
     * @param targetAccountId 目标成员账户 ID
     * @param cpPrincipal 当前认证用户
     */
    @DeleteMapping(value = "/{channelId}/admins/{targetAccountId}", produces = "application/json")
    @Operation(
            summary = "撤销管理员",
            description = "将指定频道成员的角色降级为 MEMBER，要求 OWNER 成员角色。请求体为空，成功时 data 为 null。"
    )
    @ApiResponses(@ApiResponse(responseCode = "200", description = "成功时 code 为 0 且 data 为 null", useReturnTypeSchema = true))
    public Result demoteChannelAdminV1(
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
        channelMembershipApi.demoteChannelAdmin(cpPrincipal.accountId(), channelId, targetAccountId);
        return Result.success();
    }

    /**
     * 将指定成员移出频道。
     * 约束：调用方必须具备该频道 OWNER 或 ADMIN 成员角色，且目标成员当前角色为 MEMBER。
     * 副作用：删除目标成员的频道成员关系，并在事务提交后通知频道成员与目标账号。
     *
     * <p>请求 {@code DELETE /api/channels/2001/members/1002}。无请求体。
     * <p>成功响应 HTTP 200，code 为 0，data 为 {@code null}。
     * <p>失败示例：角色不足 20009。
     *
     * @param channelId 目标频道 ID
     * @param targetAccountId 目标成员账户 ID
     * @param cpPrincipal 当前认证用户
     */
    @DeleteMapping(value = "/{channelId}/members/{targetAccountId}", produces = "application/json")
    @Operation(
            summary = "踢出频道成员",
            description = "删除指定成员的频道成员关系，要求 OWNER 或 ADMIN 成员角色。请求体为空，成功时 data 为 null。"
    )
    @ApiResponses(@ApiResponse(responseCode = "200", description = "成功时 code 为 0 且 data 为 null", useReturnTypeSchema = true))
    public Result kickChannelMember(
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
        channelMembershipApi.kickChannelMember(cpPrincipal.accountId(), channelId, targetAccountId);
        return Result.success();
    }
}
