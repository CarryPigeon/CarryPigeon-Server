package team.carrypigeon.backend.chat.domain.features.channel.controller.http;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import team.carrypigeon.backend.chat.domain.features.auth.controller.support.AuthRequestContext;
import team.carrypigeon.backend.chat.domain.features.auth.controller.support.AuthenticatedPrincipal;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.AcceptChannelInviteCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.BanChannelMemberCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.CreatePrivateChannelCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.DemoteChannelAdminCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.GetDefaultChannelCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.GetSystemChannelCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.InviteChannelMemberCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.KickChannelMemberCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.MuteChannelMemberCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.PromoteChannelMemberCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.TransferChannelOwnershipCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.UnbanChannelMemberCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.UnmuteChannelMemberCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.dto.ChannelBanResult;
import team.carrypigeon.backend.chat.domain.features.channel.application.dto.ChannelResult;
import team.carrypigeon.backend.chat.domain.features.channel.application.dto.ChannelInviteResult;
import team.carrypigeon.backend.chat.domain.features.channel.application.dto.ChannelMemberResult;
import team.carrypigeon.backend.chat.domain.features.channel.application.dto.ChannelOwnershipTransferResult;
import team.carrypigeon.backend.chat.domain.features.channel.application.query.ListChannelMembersQuery;
import team.carrypigeon.backend.chat.domain.features.channel.application.service.ChannelApplicationService;
import team.carrypigeon.backend.chat.domain.features.channel.controller.dto.BanChannelMemberRequest;
import team.carrypigeon.backend.chat.domain.features.channel.controller.dto.ChannelBanResponse;
import team.carrypigeon.backend.chat.domain.features.channel.controller.dto.ChannelResponse;
import team.carrypigeon.backend.chat.domain.features.channel.controller.dto.ChannelInviteResponse;
import team.carrypigeon.backend.chat.domain.features.channel.controller.dto.ChannelMemberResponse;
import team.carrypigeon.backend.chat.domain.features.channel.controller.dto.ChannelOwnershipTransferResponse;
import team.carrypigeon.backend.chat.domain.features.channel.controller.dto.CreatePrivateChannelRequest;
import team.carrypigeon.backend.chat.domain.features.channel.controller.dto.InviteChannelMemberRequest;
import team.carrypigeon.backend.chat.domain.features.channel.controller.dto.MuteChannelMemberRequest;
import team.carrypigeon.backend.chat.domain.features.channel.controller.dto.TransferChannelOwnershipRequest;
import team.carrypigeon.backend.chat.domain.shared.controller.CPResponse;

/**
 * 频道 HTTP 入口。
 * 职责：提供默认频道查询、私有频道创建、邀请接受与成员查询协议能力。
 * 边界：只承接协议层请求，不承载频道业务规则。
 */
@Validated
@RestController
@RequestMapping("/api/channels")
@Tag(name = "频道与成员", description = "频道查询、私有频道创建、邀请、成员治理与封禁能力。")
public class ChannelController {

    private final ChannelApplicationService channelApplicationService;
    private final AuthRequestContext authRequestContext;

    public ChannelController(
            ChannelApplicationService channelApplicationService,
            AuthRequestContext authRequestContext
    ) {
        this.channelApplicationService = channelApplicationService;
        this.authRequestContext = authRequestContext;
    }

    /**
     * 查询当前服务端默认频道。
     *
     * @param request 当前 HTTP 请求
     * @return 统一响应包装的默认频道结果
     */
    @GetMapping("/default")
    @Operation(summary = "读取默认频道", description = "读取当前服务端的默认频道。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "业务成功时 `CPResponse.code=100`；当前账户未加入默认频道时可能返回 `300`；默认频道不存在时可能返回 `404`")
    })
    public CPResponse<ChannelResponse> getDefaultChannel(HttpServletRequest request) {
        AuthenticatedPrincipal principal = authRequestContext.requirePrincipal(request);
        ChannelResult result = channelApplicationService.getDefaultChannel(new GetDefaultChannelCommand(principal.accountId()));
        return CPResponse.success(toChannelResponse(result));
    }

    /**
     * 查询当前服务端 canonical system 频道。
     *
     * @param request 当前 HTTP 请求
     * @return 统一响应包装的 system 频道结果
     */
    @GetMapping("/system")
    @Operation(summary = "读取系统频道", description = "读取当前服务端的 canonical system 频道。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "业务成功时 `CPResponse.code=100`；当前账户不满足 system 频道访问条件时可能返回 `300`；系统频道不存在时可能返回 `404`")
    })
    public CPResponse<ChannelResponse> getSystemChannel(HttpServletRequest request) {
        AuthenticatedPrincipal principal = authRequestContext.requirePrincipal(request);
        ChannelResult result = channelApplicationService.getSystemChannel(new GetSystemChannelCommand(principal.accountId()));
        return CPResponse.success(toChannelResponse(result));
    }

    /**
     * 创建 private channel。
     *
     * @param body 创建请求
     * @param request 当前 HTTP 请求
     * @return 已创建频道结果
     */
    @PostMapping("/private")
    @Operation(summary = "创建私有频道", description = "创建一个新的私有频道。")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "私有频道创建请求体。当前只需提供频道名称。", required = true,
            content = @Content(schema = @Schema(implementation = CreatePrivateChannelRequest.class)))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "业务成功时 `CPResponse.code=100`；名称为空或过长时通常返回 `200` 业务码")
    })
    public CPResponse<ChannelResponse> createPrivateChannel(
            @Valid @RequestBody CreatePrivateChannelRequest body,
            HttpServletRequest request
    ) {
        AuthenticatedPrincipal principal = authRequestContext.requirePrincipal(request);
        ChannelResult result = channelApplicationService.createPrivateChannel(
                new CreatePrivateChannelCommand(principal.accountId(), body.name())
        );
        return CPResponse.success(toChannelResponse(result));
    }

    /**
     * 邀请成员加入 private channel。
     *
     * @param channelId 频道 ID
     * @param body 邀请请求
     * @param request 当前 HTTP 请求
     * @return 邀请结果
     */
    @PostMapping("/{channelId}/invites")
    @Operation(summary = "邀请成员加入频道", description = "邀请指定账户加入当前私有频道。")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "频道邀请请求体。指定被邀请账户 ID。", required = true,
            content = @Content(schema = @Schema(implementation = InviteChannelMemberRequest.class)))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "业务成功时 `CPResponse.code=100`；无邀请权限、目标已在频道中、频道不存在或请求体不合法时可能返回 `200/300/404` 业务码")
    })
    public CPResponse<ChannelInviteResponse> inviteChannelMember(
            @Parameter(description = "目标频道 ID", example = "2001")
            @PathVariable @Positive(message = "channelId must be greater than 0") long channelId,
            @Valid @RequestBody InviteChannelMemberRequest body,
            HttpServletRequest request
    ) {
        AuthenticatedPrincipal principal = authRequestContext.requirePrincipal(request);
        ChannelInviteResult result = channelApplicationService.inviteChannelMember(
                new InviteChannelMemberCommand(principal.accountId(), channelId, body.inviteeAccountId())
        );
        return CPResponse.success(toChannelInviteResponse(result));
    }

    /**
     * 当前账户接受频道邀请。
     *
     * @param channelId 频道 ID
     * @param request 当前 HTTP 请求
     * @return 接受后的邀请结果
     */
    @PostMapping("/{channelId}/invites/accept")
    @Operation(summary = "接受频道邀请", description = "当前登录账户接受指定频道邀请。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "业务成功时 `CPResponse.code=100`；邀请不存在、邀请状态不正确或当前账户无权接受时可能返回 `200/300/404` 业务码")
    })
    public CPResponse<ChannelInviteResponse> acceptChannelInvite(
            @Parameter(description = "目标频道 ID", example = "2001")
            @PathVariable @Positive(message = "channelId must be greater than 0") long channelId,
            HttpServletRequest request
    ) {
        AuthenticatedPrincipal principal = authRequestContext.requirePrincipal(request);
        ChannelInviteResult result = channelApplicationService.acceptChannelInvite(
                new AcceptChannelInviteCommand(principal.accountId(), channelId)
        );
        return CPResponse.success(toChannelInviteResponse(result));
    }

    /**
     * 查询频道成员列表。
     *
     * @param channelId 频道 ID
     * @param request 当前 HTTP 请求
     * @return 成员列表结果
     */
    @GetMapping("/{channelId}/members")
    @Operation(summary = "查询频道成员", description = "查询指定频道的成员列表。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "业务成功时 `CPResponse.code=100`；当前账户无成员资格或频道不存在时可能返回 `300/404` 业务码")
    })
    public CPResponse<List<ChannelMemberResponse>> listChannelMembers(
            @Parameter(description = "目标频道 ID", example = "2001")
            @PathVariable @Positive(message = "channelId must be greater than 0") long channelId,
            HttpServletRequest request
    ) {
        AuthenticatedPrincipal principal = authRequestContext.requirePrincipal(request);
        List<ChannelMemberResult> result = channelApplicationService.listChannelMembers(
                new ListChannelMembersQuery(principal.accountId(), channelId)
        );
        return CPResponse.success(result.stream().map(this::toChannelMemberResponse).toList());
    }

    /**
     * 提升频道成员为 ADMIN。
     */
    @PostMapping("/{channelId}/members/{targetAccountId}/admin")
    @Operation(summary = "提升管理员", description = "将指定频道成员提升为 ADMIN。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "业务成功时 `CPResponse.code=100`；无治理权限、目标账户非法或频道不存在时可能返回 `200/300/404` 业务码")
    })
    public CPResponse<ChannelMemberResponse> promoteChannelMember(
            @Parameter(description = "目标频道 ID", example = "2001")
            @PathVariable @Positive(message = "channelId must be greater than 0") long channelId,
            @Parameter(description = "目标成员账户 ID", example = "1002")
            @PathVariable @Positive(message = "targetAccountId must be greater than 0") long targetAccountId,
            HttpServletRequest request
    ) {
        AuthenticatedPrincipal principal = authRequestContext.requirePrincipal(request);
        ChannelMemberResult result = channelApplicationService.promoteChannelMember(
                new PromoteChannelMemberCommand(principal.accountId(), channelId, targetAccountId)
        );
        return CPResponse.success(toChannelMemberResponse(result));
    }

    /**
     * 降级频道 ADMIN 为 MEMBER。
     */
    @DeleteMapping("/{channelId}/members/{targetAccountId}/admin")
    @Operation(summary = "取消管理员", description = "将指定频道 ADMIN 降级为 MEMBER。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "业务成功时 `CPResponse.code=100`；无治理权限、目标不存在或频道不存在时可能返回 `300/404` 业务码")
    })
    public CPResponse<ChannelMemberResponse> demoteChannelAdmin(
            @Parameter(description = "目标频道 ID", example = "2001")
            @PathVariable @Positive(message = "channelId must be greater than 0") long channelId,
            @Parameter(description = "目标管理员账户 ID", example = "1002")
            @PathVariable @Positive(message = "targetAccountId must be greater than 0") long targetAccountId,
            HttpServletRequest request
    ) {
        AuthenticatedPrincipal principal = authRequestContext.requirePrincipal(request);
        ChannelMemberResult result = channelApplicationService.demoteChannelAdmin(
                new DemoteChannelAdminCommand(principal.accountId(), channelId, targetAccountId)
        );
        return CPResponse.success(toChannelMemberResponse(result));
    }

    /**
     * 转移频道所有权。
     */
    @PostMapping("/{channelId}/ownership-transfer")
    @Operation(summary = "转移频道所有权", description = "将指定频道的所有权转移给目标账户。")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "频道所有权转移请求体。指定新的 OWNER 账户 ID。", required = true,
            content = @Content(schema = @Schema(implementation = TransferChannelOwnershipRequest.class)))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "业务成功时 `CPResponse.code=100`；无 OWNER 权限、目标账户不合法或频道不存在时可能返回 `200/300/404` 业务码")
    })
    public CPResponse<ChannelOwnershipTransferResponse> transferChannelOwnership(
            @Parameter(description = "目标频道 ID", example = "2001")
            @PathVariable @Positive(message = "channelId must be greater than 0") long channelId,
            @Valid @RequestBody TransferChannelOwnershipRequest body,
            HttpServletRequest request
    ) {
        AuthenticatedPrincipal principal = authRequestContext.requirePrincipal(request);
        ChannelOwnershipTransferResult result = channelApplicationService.transferChannelOwnership(
                new TransferChannelOwnershipCommand(principal.accountId(), channelId, body.targetAccountId())
        );
        return CPResponse.success(toChannelOwnershipTransferResponse(result));
    }

    /**
     * 禁言频道成员。
     */
    @PostMapping("/{channelId}/members/{targetAccountId}/mute")
    @Operation(summary = "禁言频道成员", description = "在指定频道内禁言目标成员一段时间。")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "频道禁言请求体。通过 `durationSeconds` 指定禁言持续时间（秒）。", required = true,
            content = @Content(schema = @Schema(implementation = MuteChannelMemberRequest.class)))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "业务成功时 `CPResponse.code=100`；无治理权限、duration 非法、目标不存在或频道不存在时可能返回 `200/300/404` 业务码")
    })
    public CPResponse<ChannelMemberResponse> muteChannelMember(
            @Parameter(description = "目标频道 ID", example = "2001")
            @PathVariable @Positive(message = "channelId must be greater than 0") long channelId,
            @Parameter(description = "目标成员账户 ID", example = "1002")
            @PathVariable @Positive(message = "targetAccountId must be greater than 0") long targetAccountId,
            @Valid @RequestBody MuteChannelMemberRequest body,
            HttpServletRequest request
    ) {
        AuthenticatedPrincipal principal = authRequestContext.requirePrincipal(request);
        ChannelMemberResult result = channelApplicationService.muteChannelMember(
                new MuteChannelMemberCommand(principal.accountId(), channelId, targetAccountId, body.durationSeconds())
        );
        return CPResponse.success(toChannelMemberResponse(result));
    }

    /**
     * 解除频道成员禁言。
     */
    @DeleteMapping("/{channelId}/members/{targetAccountId}/mute")
    @Operation(summary = "解除成员禁言", description = "解除指定频道成员当前的禁言状态。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "业务成功时 `CPResponse.code=100`；无治理权限、目标不存在或频道不存在时可能返回 `300/404` 业务码")
    })
    public CPResponse<ChannelMemberResponse> unmuteChannelMember(
            @Parameter(description = "目标频道 ID", example = "2001")
            @PathVariable @Positive(message = "channelId must be greater than 0") long channelId,
            @Parameter(description = "目标成员账户 ID", example = "1002")
            @PathVariable @Positive(message = "targetAccountId must be greater than 0") long targetAccountId,
            HttpServletRequest request
    ) {
        AuthenticatedPrincipal principal = authRequestContext.requirePrincipal(request);
        ChannelMemberResult result = channelApplicationService.unmuteChannelMember(
                new UnmuteChannelMemberCommand(principal.accountId(), channelId, targetAccountId)
        );
        return CPResponse.success(toChannelMemberResponse(result));
    }

    /**
     * 踢出频道成员。
     */
    @DeleteMapping("/{channelId}/members/{targetAccountId}")
    @Operation(summary = "踢出频道成员", description = "将指定成员从频道中移除。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "业务成功时 `CPResponse.code=100` 且 `data=null`；无治理权限、目标不存在或频道不存在时可能返回 `300/404` 业务码")
    })
    public CPResponse<Void> kickChannelMember(
            @Parameter(description = "目标频道 ID", example = "2001")
            @PathVariable @Positive(message = "channelId must be greater than 0") long channelId,
            @Parameter(description = "目标成员账户 ID", example = "1002")
            @PathVariable @Positive(message = "targetAccountId must be greater than 0") long targetAccountId,
            HttpServletRequest request
    ) {
        AuthenticatedPrincipal principal = authRequestContext.requirePrincipal(request);
        channelApplicationService.kickChannelMember(
                new KickChannelMemberCommand(principal.accountId(), channelId, targetAccountId)
        );
        return CPResponse.success(null);
    }

    /**
     * 封禁频道成员。
     */
    @PostMapping("/{channelId}/bans")
    @Operation(summary = "封禁频道成员", description = "封禁指定频道成员，并可附带原因与时长。")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "频道封禁请求体。可提供封禁原因；`durationSeconds` 为空表示无限期封禁。", required = true,
            content = @Content(schema = @Schema(implementation = BanChannelMemberRequest.class)))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "业务成功时 `CPResponse.code=100`；无治理权限、目标不存在、封禁参数非法或频道不存在时可能返回 `200/300/404` 业务码")
    })
    public CPResponse<ChannelBanResponse> banChannelMember(
            @Parameter(description = "目标频道 ID", example = "2001")
            @PathVariable @Positive(message = "channelId must be greater than 0") long channelId,
            @Valid @RequestBody BanChannelMemberRequest body,
            HttpServletRequest request
    ) {
        AuthenticatedPrincipal principal = authRequestContext.requirePrincipal(request);
        ChannelBanResult result = channelApplicationService.banChannelMember(
                new BanChannelMemberCommand(principal.accountId(), channelId, body.targetAccountId(), body.reason(), body.durationSeconds())
        );
        return CPResponse.success(toChannelBanResponse(result));
    }

    /**
     * 解除频道封禁。
     */
    @DeleteMapping("/{channelId}/bans/{targetAccountId}")
    @Operation(summary = "解除频道封禁", description = "解除指定频道成员的封禁状态。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "业务成功时 `CPResponse.code=100`；目标封禁不存在、无治理权限或频道不存在时可能返回 `300/404` 业务码")
    })
    public CPResponse<ChannelBanResponse> unbanChannelMember(
            @Parameter(description = "目标频道 ID", example = "2001")
            @PathVariable @Positive(message = "channelId must be greater than 0") long channelId,
            @Parameter(description = "被解封账户 ID", example = "1002")
            @PathVariable @Positive(message = "targetAccountId must be greater than 0") long targetAccountId,
            HttpServletRequest request
    ) {
        AuthenticatedPrincipal principal = authRequestContext.requirePrincipal(request);
        ChannelBanResult result = channelApplicationService.unbanChannelMember(
                new UnbanChannelMemberCommand(principal.accountId(), channelId, targetAccountId)
        );
        return CPResponse.success(toChannelBanResponse(result));
    }

    private ChannelResponse toChannelResponse(ChannelResult result) {
        return new ChannelResponse(
                result.channelId(),
                result.conversationId(),
                result.name(),
                result.type(),
                result.defaultChannel(),
                result.createdAt(),
                result.updatedAt()
        );
    }

    private ChannelInviteResponse toChannelInviteResponse(ChannelInviteResult result) {
        return new ChannelInviteResponse(
                result.channelId(),
                result.inviteeAccountId(),
                result.inviterAccountId(),
                result.status(),
                result.createdAt(),
                result.respondedAt()
        );
    }

    private ChannelMemberResponse toChannelMemberResponse(ChannelMemberResult result) {
        return new ChannelMemberResponse(
                result.accountId(),
                result.nickname(),
                result.avatarUrl(),
                result.role(),
                result.joinedAt(),
                result.mutedUntil()
        );
    }

    private ChannelBanResponse toChannelBanResponse(ChannelBanResult result) {
        return new ChannelBanResponse(
                result.channelId(),
                result.bannedAccountId(),
                result.operatorAccountId(),
                result.reason(),
                result.expiresAt(),
                result.createdAt(),
                result.revokedAt()
        );
    }

    private ChannelOwnershipTransferResponse toChannelOwnershipTransferResponse(ChannelOwnershipTransferResult result) {
        return new ChannelOwnershipTransferResponse(
                result.channelId(),
                result.previousOwnerAccountId(),
                result.previousOwnerRole(),
                result.newOwnerAccountId(),
                result.newOwnerRole()
        );
    }
}
