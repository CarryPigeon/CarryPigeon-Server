package team.carrypigeon.backend.chat.domain.features.channel.controller.http;

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
    public CPResponse<ChannelResponse> getDefaultChannel(HttpServletRequest request) {
        AuthenticatedPrincipal principal = authRequestContext.requirePrincipal(request);
        ChannelResult result = channelApplicationService.getDefaultChannel(new GetDefaultChannelCommand(principal.accountId()));
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
    public CPResponse<ChannelInviteResponse> inviteChannelMember(
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
    public CPResponse<ChannelInviteResponse> acceptChannelInvite(
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
    public CPResponse<List<ChannelMemberResponse>> listChannelMembers(
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
    public CPResponse<ChannelMemberResponse> promoteChannelMember(
            @PathVariable @Positive(message = "channelId must be greater than 0") long channelId,
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
    public CPResponse<ChannelMemberResponse> demoteChannelAdmin(
            @PathVariable @Positive(message = "channelId must be greater than 0") long channelId,
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
    public CPResponse<ChannelOwnershipTransferResponse> transferChannelOwnership(
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
    public CPResponse<ChannelMemberResponse> muteChannelMember(
            @PathVariable @Positive(message = "channelId must be greater than 0") long channelId,
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
    public CPResponse<ChannelMemberResponse> unmuteChannelMember(
            @PathVariable @Positive(message = "channelId must be greater than 0") long channelId,
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
    public CPResponse<Void> kickChannelMember(
            @PathVariable @Positive(message = "channelId must be greater than 0") long channelId,
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
    public CPResponse<ChannelBanResponse> banChannelMember(
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
    public CPResponse<ChannelBanResponse> unbanChannelMember(
            @PathVariable @Positive(message = "channelId must be greater than 0") long channelId,
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
