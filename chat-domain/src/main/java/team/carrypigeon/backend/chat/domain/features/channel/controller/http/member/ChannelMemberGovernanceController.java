package team.carrypigeon.backend.chat.domain.features.channel.controller.http.member;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
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
import team.carrypigeon.backend.infrastructure.basic.id.IdUtil;

/**
 * 频道成员治理 HTTP 入口。
 * 职责：提供成员查询、管理员调整和成员移除协议。
 * 边界：只映射成员治理请求，不处理频道生命周期和封禁。
 */
@Validated
@RestController
@RequestMapping("/api/channels")
@PreAuthorize("isAuthenticated()")
@Tag(name = "频道成员", description = "频道成员的基础性能力")
public class ChannelMemberGovernanceController {

    private final ChannelMemberApi channelMembershipApi;

    @Autowired
    public ChannelMemberGovernanceController(
            ChannelMemberApi channelMembershipApi
    ) {
        this.channelMembershipApi = channelMembershipApi;
    }

    /**
     * 查询指定频道的成员列表。
     */
    @GetMapping("/{channelId}/members")
    @Operation(summary = "查询频道成员", description = "查询指定频道的成员列表。")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "返回频道成员列表")})
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
     */
    @PutMapping("/{channelId}/admins/{targetAccountId}")
    @Operation(summary = "设为管理员", description = "按 v1 资源路径将指定成员设为管理员。")
    @ApiResponses({@ApiResponse(responseCode = "204", description = "管理员设置成功")})
    public ResponseEntity<Void> promoteChannelMemberV1(
            @PathVariable @Positive(message = "channelId must be greater than 0") long channelId,
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
     */
    @DeleteMapping("/{channelId}/admins/{targetAccountId}")
    @Operation(summary = "撤销管理员", description = "按 v1 资源路径撤销管理员角色。")
    @ApiResponses({@ApiResponse(responseCode = "204", description = "管理员角色撤销成功")})
    public ResponseEntity<Void> demoteChannelAdminV1(
            @PathVariable @Positive(message = "channelId must be greater than 0") long channelId,
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
     */
    @DeleteMapping("/{channelId}/members/{targetAccountId}")
    @Operation(summary = "踢出频道成员", description = "将指定成员从频道中移除。")
    @ApiResponses({@ApiResponse(responseCode = "204", description = "移除成功")})
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
