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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import team.carrypigeon.backend.chat.domain.config.http.security.CpPrincipal;
import team.carrypigeon.backend.chat.domain.features.channel.controller.dto.ChannelSummaryResponse;
import team.carrypigeon.backend.chat.domain.features.channel.controller.dto.CreateChannelRequest;
import team.carrypigeon.backend.chat.domain.features.channel.controller.dto.UpdateChannelProfileRequest;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.ChannelLifecycleApi;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.CreateChannelCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.DeleteChannelCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.UpdateChannelProfileCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.projection.ChannelResult;
import team.carrypigeon.backend.infrastructure.basic.id.IdUtil;

/**
 * 频道生命周期 HTTP 入口。
 * 职责：承接频道创建、删除和资料更新协议。
 * 边界：只映射生命周期命令，不处理成员治理或频道查询。
 */
@Validated
@RestController
@RequestMapping("/api/channels")
@PreAuthorize("isAuthenticated()")
@Tag(name = "频道与成员", description = "频道查询、成员治理、申请、发现与封禁能力。")
public class ChannelLifecycleController {

    private final ChannelLifecycleApi channelLifecycleDomainApi;

    @Autowired
    public ChannelLifecycleController(
            ChannelLifecycleApi channelLifecycleDomainApi
    ) {
        this.channelLifecycleDomainApi = channelLifecycleDomainApi;
    }

    /**
     * 创建频道并返回新频道摘要。
     */
    @PostMapping
    @Operation(summary = "创建频道", description = "按 v1 资源路径创建频道；当前内部仍复用 private channel 创建逻辑。")
    @ApiResponses({@ApiResponse(responseCode = "201", description = "频道创建成功")})
    public ResponseEntity<ChannelSummaryResponse> createChannel(
            @Valid @RequestBody CreateChannelRequest body,
            @AuthenticationPrincipal CpPrincipal cpPrincipal
    ) {
        ChannelResult result = channelLifecycleDomainApi.createChannel(new CreateChannelCommand(
                cpPrincipal.accountId(),
                body.name(),
                body.brief(),
                body.avatar()
        ));
        return ResponseEntity.status(201).body(toChannelSummaryResponse(result));
    }

    /**
     * 删除指定频道。
     */
    @DeleteMapping("/{channelId}")
    @Operation(summary = "删除频道", description = "按 v1 资源路径删除指定频道。")
    @ApiResponses({@ApiResponse(responseCode = "204", description = "频道删除成功")})
    public ResponseEntity<Void> deleteChannel(
            @PathVariable @Positive(message = "channelId must be greater than 0") long channelId,
            @AuthenticationPrincipal CpPrincipal cpPrincipal
    ) {
        channelLifecycleDomainApi.deleteChannel(new DeleteChannelCommand(cpPrincipal.accountId(), channelId));
        return ResponseEntity.noContent().build();
    }

    /**
     * 更新指定频道的名称与简介。
     */
    @PatchMapping("/{channelId}")
    @Operation(summary = "更新频道资料", description = "按 v1 资源路径更新频道名称与简介。")
    @ApiResponses({@ApiResponse(responseCode = "204", description = "频道资料更新成功")})
    public ResponseEntity<Void> updateChannelProfile(
            @PathVariable @Positive(message = "channelId must be greater than 0") long channelId,
            @Valid @RequestBody UpdateChannelProfileRequest body,
            @AuthenticationPrincipal CpPrincipal cpPrincipal
    ) {
        channelLifecycleDomainApi.updateChannelProfile(new UpdateChannelProfileCommand(
                cpPrincipal.accountId(),
                channelId,
                body.name(),
                body.brief()
        ));
        return ResponseEntity.noContent().build();
    }

    private ChannelSummaryResponse toChannelSummaryResponse(ChannelResult result) {
        return new ChannelSummaryResponse(
                IdUtil.toString(result.channelId()),
                result.name(),
                result.brief(),
                result.avatar(),
                result.ownerUid()
        );
    }
}
