package team.carrypigeon.backend.chat.domain.features.channel.controller.http.ban;

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
@Tag(name = "频道封禁", description = "频道封禁的基础性能力")
public class ChannelBansController {

    private final ChannelBanApi channelBanApi;

    @Autowired
    public ChannelBansController(
            ChannelBanApi channelBanApi
    ) {
        this.channelBanApi = channelBanApi;
    }

    /**
     * 封禁指定频道成员。
     */
    @PutMapping("/{channelId}/bans/{targetAccountId}")
    @Operation(summary = "禁言频道成员", description = "按 docs/api/API.md 契约禁言指定成员。")
    public ResponseEntity<ChannelBanResponse> banChannelMemberV1(
            @PathVariable @Positive(message = "channelId must be greater than 0") long channelId,
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
        channelBanApi.unbanChannelMember(
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
