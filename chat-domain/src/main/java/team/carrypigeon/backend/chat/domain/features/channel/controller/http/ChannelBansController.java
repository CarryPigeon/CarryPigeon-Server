package team.carrypigeon.backend.chat.domain.features.channel.controller.http;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.beans.factory.annotation.Autowired;
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
import team.carrypigeon.backend.chat.domain.features.channel.controller.dto.BanChannelMemberV1Request;
import team.carrypigeon.backend.chat.domain.features.channel.controller.dto.ChannelBanListItemResponse;
import team.carrypigeon.backend.chat.domain.features.channel.controller.dto.ChannelBanListResponse;
import team.carrypigeon.backend.chat.domain.features.channel.controller.dto.ChannelBanV1Response;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.ChannelGovernanceApi;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.ChannelQueryApi;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.BanChannelMemberUntilCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.UnbanChannelMemberCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.projection.ChannelBanListItemResult;
import team.carrypigeon.backend.chat.domain.features.channel.domain.projection.ChannelBanResult;
import team.carrypigeon.backend.chat.domain.features.channel.domain.query.ListChannelBansQuery;
import team.carrypigeon.backend.infrastructure.basic.id.IdUtil;

/**
 * 频道封禁 HTTP 入口。
 * 职责：提供频道成员封禁、解禁和封禁列表协议。
 * 边界：只映射封禁相关请求，不处理其它成员治理操作。
 */
@Validated
@RestController
@RequestMapping("/api/channels")
@PreAuthorize("isAuthenticated()")
@Tag(name = "频道与成员", description = "频道查询、成员治理、申请、发现与封禁能力。")
public class ChannelBansController {

    private final ChannelQueryApi channelQueryDomainApi;
    private final ChannelGovernanceApi channelGovernanceDomainApi;

    @Autowired
    public ChannelBansController(
            ChannelQueryApi channelQueryDomainApi,
            ChannelGovernanceApi channelGovernanceDomainApi
    ) {
        this.channelQueryDomainApi = channelQueryDomainApi;
        this.channelGovernanceDomainApi = channelGovernanceDomainApi;
    }

    /**
     * 封禁指定频道成员。
     */
    @PutMapping("/{channelId}/bans/{targetAccountId}")
    @Operation(summary = "禁言频道成员", description = "按 docs/api/API.md 契约禁言指定成员。")
    public ResponseEntity<ChannelBanV1Response> banChannelMemberV1(
            @PathVariable @Positive(message = "channelId must be greater than 0") long channelId,
            @PathVariable @Positive(message = "targetAccountId must be greater than 0") long targetAccountId,
            @Valid @RequestBody BanChannelMemberV1Request body,
            @AuthenticationPrincipal CpPrincipal cpPrincipal
    ) {
        ChannelBanResult result = channelGovernanceDomainApi.banChannelMemberUntil(
                new BanChannelMemberUntilCommand(
                        cpPrincipal.accountId(),
                        channelId,
                        targetAccountId,
                        body.reason(),
                        body.until()
                )
        );
        return ResponseEntity.ok(new ChannelBanV1Response(
                IdUtil.toString(result.channelId()),
                IdUtil.toString(result.bannedAccountId()),
                result.expiresAt() == null ? null : result.expiresAt().toEpochMilli(),
                result.reason(),
                result.createdAt().toEpochMilli()
        ));
    }

    /**
     * 解除指定频道成员的封禁。
     */
    @DeleteMapping("/{channelId}/bans/{targetAccountId}")
    @Operation(summary = "解除频道封禁", description = "解除指定频道成员的封禁状态。")
    @ApiResponses({@ApiResponse(responseCode = "204", description = "解除成功")})
    public ResponseEntity<Void> unbanChannelMember(
            @PathVariable @Positive(message = "channelId must be greater than 0") long channelId,
            @AuthenticationPrincipal CpPrincipal cpPrincipal,
            @PathVariable @Positive(message = "targetAccountId must be greater than 0") long targetAccountId
    ) {
        channelGovernanceDomainApi.unbanChannelMember(
                new UnbanChannelMemberCommand(cpPrincipal.accountId(), channelId, targetAccountId)
        );
        return ResponseEntity.noContent().build();
    }

    /**
     * 返回指定频道的封禁列表。
     */
    @GetMapping("/{channelId}/bans")
    @Operation(summary = "获取禁言列表", description = "按频道返回封禁列表。")
    public ChannelBanListResponse listChannelBans(
            @PathVariable @Positive(message = "channelId must be greater than 0") long channelId,
            @AuthenticationPrincipal CpPrincipal cpPrincipal
    ) {
        return new ChannelBanListResponse(channelQueryDomainApi.listChannelBans(
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
