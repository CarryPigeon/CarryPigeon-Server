package team.carrypigeon.backend.chat.domain.features.channel.controller.http;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import team.carrypigeon.backend.chat.domain.shared.controller.support.RequestAuthenticationContext;
import team.carrypigeon.backend.chat.domain.shared.domain.auth.AuthenticatedAccount;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.CreateChannelCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.DeleteChannelCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.DemoteChannelAdminCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.KickChannelMemberCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.PromoteChannelMemberCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.UpdateChannelProfileCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.BanChannelMemberUntilCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.UnbanChannelMemberCommand;
import team.carrypigeon.backend.chat.domain.features.server.domain.command.UpdateNotificationChannelPreferenceCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.projection.ChannelBanListItemResult;
import team.carrypigeon.backend.chat.domain.features.channel.domain.projection.ChannelBanResult;
import team.carrypigeon.backend.chat.domain.features.channel.domain.projection.DiscoverChannelResult;
import team.carrypigeon.backend.chat.domain.features.channel.domain.projection.ChannelResult;
import team.carrypigeon.backend.chat.domain.features.channel.domain.projection.ChannelMemberResult;
import team.carrypigeon.backend.chat.domain.features.channel.domain.query.ListChannelBansQuery;
import team.carrypigeon.backend.chat.domain.features.channel.domain.query.DiscoverChannelsQuery;
import team.carrypigeon.backend.chat.domain.features.channel.domain.query.ListChannelMembersQuery;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.ChannelGovernanceApi;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.ChannelLifecycleApi;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.ChannelQueryApi;
import team.carrypigeon.backend.chat.domain.features.server.domain.api.NotificationPreferenceApi;
import team.carrypigeon.backend.chat.domain.features.channel.controller.dto.BanChannelMemberV1Request;
import team.carrypigeon.backend.chat.domain.features.channel.controller.dto.ChannelBanV1Response;
import team.carrypigeon.backend.chat.domain.features.channel.controller.dto.ChannelBanListItemResponse;
import team.carrypigeon.backend.chat.domain.features.channel.controller.dto.ChannelBanListResponse;
import team.carrypigeon.backend.chat.domain.features.channel.controller.dto.DiscoverChannelResponse;
import team.carrypigeon.backend.chat.domain.features.channel.controller.dto.ChannelListResponse;
import team.carrypigeon.backend.chat.domain.features.channel.controller.dto.ChannelMemberListResponse;
import team.carrypigeon.backend.chat.domain.features.channel.controller.dto.ChannelMemberV1Response;
import team.carrypigeon.backend.chat.domain.features.channel.controller.dto.ChannelSummaryResponse;
import team.carrypigeon.backend.chat.domain.features.channel.controller.dto.CreateChannelRequest;
import team.carrypigeon.backend.chat.domain.features.channel.controller.dto.UpdateChannelNotificationPreferenceRequest;
import team.carrypigeon.backend.chat.domain.features.channel.controller.dto.UpdateChannelProfileRequest;
import team.carrypigeon.backend.chat.domain.shared.controller.CursorPageResponse;
import team.carrypigeon.backend.chat.domain.shared.controller.OpaqueCursorCodec;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.id.Ids;

/**
 * 频道 HTTP 入口。
 * 职责：提供默认频道查询、私有频道创建、邀请接受与成员查询协议能力。
 * 边界：只承接协议层请求，不承载频道业务规则。
 */
@Validated
@RestController
@RequestMapping("/api/channels")
@Tag(name = "频道与成员", description = "频道查询、成员治理、申请、发现与封禁能力。")
public class ChannelController {

    private static final String DISCOVER_CURSOR_SCOPE = "channel_discover";

    private final ChannelQueryApi channelQueryDomainApi;
    private final ChannelLifecycleApi channelLifecycleDomainApi;
    private final ChannelGovernanceApi channelGovernanceDomainApi;
    private final NotificationPreferenceApi notificationPreferenceDomainApi;
    private final RequestAuthenticationContext authRequestContext;

    @Autowired
    public ChannelController(
            ChannelQueryApi channelQueryDomainApi,
            ChannelLifecycleApi channelLifecycleDomainApi,
            ChannelGovernanceApi channelGovernanceDomainApi,
            NotificationPreferenceApi notificationPreferenceDomainApi,
            RequestAuthenticationContext authRequestContext
    ) {
        this.channelQueryDomainApi = channelQueryDomainApi;
        this.channelLifecycleDomainApi = channelLifecycleDomainApi;
        this.channelGovernanceDomainApi = channelGovernanceDomainApi;
        this.notificationPreferenceDomainApi = notificationPreferenceDomainApi;
        this.authRequestContext = authRequestContext;
    }

    public ChannelController(
            ChannelQueryApi channelQueryDomainApi,
            ChannelLifecycleApi channelLifecycleDomainApi,
            ChannelGovernanceApi channelGovernanceDomainApi,
            RequestAuthenticationContext authRequestContext
    ) {
        this(
                channelQueryDomainApi,
                channelLifecycleDomainApi,
                channelGovernanceDomainApi,
                null,
                authRequestContext
        );
    }

    /**
     * 返回当前用户可见的频道列表。
     *
     * @param request 当前 HTTP 请求
     * @return 频道摘要列表
     */
    @GetMapping
    @Operation(summary = "获取频道列表", description = "返回当前登录用户当前可见的频道列表。")
    public ChannelListResponse listChannels(HttpServletRequest request) {
        AuthenticatedAccount principal = authRequestContext.requirePrincipal(request);
        return new ChannelListResponse(channelQueryDomainApi.listChannels(principal.accountId()).stream()
                .map(this::toChannelSummaryResponse)
                .toList());
    }

    @GetMapping("/{channelId}")
    @Operation(summary = "获取频道资料", description = "按频道 ID 返回当前可见频道摘要。")
    public ChannelSummaryResponse getChannelById(
            @PathVariable @Positive(message = "channelId must be greater than 0") long channelId,
            HttpServletRequest request
    ) {
        AuthenticatedAccount principal = authRequestContext.requirePrincipal(request);
        return toChannelSummaryResponse(channelQueryDomainApi.getChannelById(principal.accountId(), channelId));
    }

    @GetMapping("/discover")
    public CursorPageResponse<DiscoverChannelResponse> discoverChannels(
            @RequestParam(name = "q", required = false) String keyword,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "20") int limit,
            HttpServletRequest request
    ) {
        AuthenticatedAccount principal = authRequestContext.requirePrincipal(request);
        List<DiscoverChannelResult> items = channelQueryDomainApi.discoverChannels(new DiscoverChannelsQuery(
                principal.accountId(),
                keyword,
                OpaqueCursorCodec.decode(DISCOVER_CURSOR_SCOPE, cursor),
                type,
                limit
        ));
        boolean hasMore = items.size() > limit;
        List<DiscoverChannelResult> pageItems = hasMore ? items.subList(0, limit) : items;
        String nextCursor = hasMore ? OpaqueCursorCodec.encode(DISCOVER_CURSOR_SCOPE, Long.parseLong(pageItems.get(pageItems.size() - 1).cid())) : null;
        return CursorPageResponse.of(pageItems.stream().map(item -> new DiscoverChannelResponse(
                item.cid(),
                item.name(),
                item.brief(),
                item.avatar(),
                item.memberCount(),
                item.requiresApplication()
        )).toList(), nextCursor, hasMore);
    }

    @PostMapping
    @Operation(summary = "创建频道", description = "按 v1 资源路径创建频道；当前内部仍复用 private channel 创建逻辑。")
    public ResponseEntity<ChannelSummaryResponse> createChannel(
            @Valid @RequestBody CreateChannelRequest body,
            HttpServletRequest request
    ) {
        AuthenticatedAccount principal = authRequestContext.requirePrincipal(request);
        ChannelResult result = channelLifecycleDomainApi.createChannel(new CreateChannelCommand(
                principal.accountId(),
                body.name(),
                body.brief(),
                body.avatar()
        ));
        return ResponseEntity.status(201).body(toChannelSummaryResponse(result));
    }

    @DeleteMapping("/{channelId}")
    @Operation(summary = "删除频道", description = "按 v1 资源路径删除指定频道。")
    public ResponseEntity<Void> deleteChannel(
            @PathVariable @Positive(message = "channelId must be greater than 0") long channelId,
            HttpServletRequest request
    ) {
        AuthenticatedAccount principal = authRequestContext.requirePrincipal(request);
        channelLifecycleDomainApi.deleteChannel(new DeleteChannelCommand(principal.accountId(), channelId));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{channelId}")
    @Operation(summary = "更新频道资料", description = "按 v1 资源路径更新频道名称与简介。")
    public ResponseEntity<Void> updateChannelProfile(
            @PathVariable @Positive(message = "channelId must be greater than 0") long channelId,
            @Valid @RequestBody UpdateChannelProfileRequest body,
            HttpServletRequest request
    ) {
        AuthenticatedAccount principal = authRequestContext.requirePrincipal(request);
        channelLifecycleDomainApi.updateChannelProfile(new UpdateChannelProfileCommand(
                principal.accountId(),
                channelId,
                body.name(),
                body.brief()
        ));
        return ResponseEntity.noContent().build();
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
            @ApiResponse(responseCode = "200", description = "返回频道成员列表")
    })
    public ChannelMemberListResponse listChannelMembers(
            @Parameter(description = "目标频道 ID", example = "2001")
            @PathVariable @Positive(message = "channelId must be greater than 0") long channelId,
            HttpServletRequest request
    ) {
        AuthenticatedAccount principal = authRequestContext.requirePrincipal(request);
        List<ChannelMemberResult> result = channelQueryDomainApi.listChannelMembers(
                new ListChannelMembersQuery(principal.accountId(), channelId)
        );
        return new ChannelMemberListResponse(result.stream().map(this::toChannelMemberV1Response).toList());
    }

    @PutMapping("/{channelId}/admins/{targetAccountId}")
    @Operation(summary = "设为管理员", description = "按 v1 资源路径将指定成员设为管理员。")
    public ResponseEntity<Void> promoteChannelMemberV1(
            @PathVariable @Positive(message = "channelId must be greater than 0") long channelId,
            @PathVariable @Positive(message = "targetAccountId must be greater than 0") long targetAccountId,
            HttpServletRequest request
    ) {
        AuthenticatedAccount principal = authRequestContext.requirePrincipal(request);
        channelGovernanceDomainApi.promoteChannelMember(
                new PromoteChannelMemberCommand(principal.accountId(), channelId, targetAccountId)
        );
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{channelId}/admins/{targetAccountId}")
    @Operation(summary = "撤销管理员", description = "按 v1 资源路径撤销管理员角色。")
    public ResponseEntity<Void> demoteChannelAdminV1(
            @PathVariable @Positive(message = "channelId must be greater than 0") long channelId,
            @PathVariable @Positive(message = "targetAccountId must be greater than 0") long targetAccountId,
            HttpServletRequest request
    ) {
        AuthenticatedAccount principal = authRequestContext.requirePrincipal(request);
        channelGovernanceDomainApi.demoteChannelAdmin(
                new DemoteChannelAdminCommand(principal.accountId(), channelId, targetAccountId)
        );
        return ResponseEntity.noContent().build();
    }

    /**
     * 踢出频道成员。
     */
    @DeleteMapping("/{channelId}/members/{targetAccountId}")
    @Operation(summary = "踢出频道成员", description = "将指定成员从频道中移除。")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "移除成功")
    })
    public ResponseEntity<Void> kickChannelMember(
            @Parameter(description = "目标频道 ID", example = "2001")
            @PathVariable @Positive(message = "channelId must be greater than 0") long channelId,
            @Parameter(description = "目标成员账户 ID", example = "1002")
            @PathVariable @Positive(message = "targetAccountId must be greater than 0") long targetAccountId,
            HttpServletRequest request
    ) {
        AuthenticatedAccount principal = authRequestContext.requirePrincipal(request);
        channelGovernanceDomainApi.kickChannelMember(
                new KickChannelMemberCommand(principal.accountId(), channelId, targetAccountId)
        );
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{channelId}/bans/{targetAccountId}")
    @Operation(summary = "禁言频道成员", description = "按 docs/t v1 契约禁言指定成员。")
    public ResponseEntity<ChannelBanV1Response> banChannelMemberV1(
            @PathVariable @Positive(message = "channelId must be greater than 0") long channelId,
            @PathVariable @Positive(message = "targetAccountId must be greater than 0") long targetAccountId,
            @Valid @RequestBody BanChannelMemberV1Request body,
            HttpServletRequest request
    ) {
        AuthenticatedAccount principal = authRequestContext.requirePrincipal(request);
        ChannelBanResult result = channelGovernanceDomainApi.banChannelMemberUntil(
                new BanChannelMemberUntilCommand(
                        principal.accountId(),
                        channelId,
                        targetAccountId,
                        body.reason(),
                        body.until()
                )
        );
        return ResponseEntity.ok(new ChannelBanV1Response(
                Ids.toString(result.channelId()),
                Ids.toString(result.bannedAccountId()),
                result.expiresAt() == null ? null : result.expiresAt().toEpochMilli(),
                result.reason(),
                result.createdAt().toEpochMilli()
        ));
    }

    @DeleteMapping("/{channelId}/bans/{targetAccountId}")
    @Operation(summary = "解除频道封禁", description = "解除指定频道成员的封禁状态。")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "解除成功")
    })
    public ResponseEntity<Void> unbanChannelMember(
            @PathVariable @Positive(message = "channelId must be greater than 0") long channelId,
            @PathVariable @Positive(message = "targetAccountId must be greater than 0") long targetAccountId,
            HttpServletRequest request
    ) {
        AuthenticatedAccount principal = authRequestContext.requirePrincipal(request);
        channelGovernanceDomainApi.unbanChannelMember(
                new UnbanChannelMemberCommand(principal.accountId(), channelId, targetAccountId)
        );
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{channelId}/bans")
    @Operation(summary = "获取禁言列表", description = "按频道返回封禁列表。")
    public ChannelBanListResponse listChannelBans(
            @PathVariable @Positive(message = "channelId must be greater than 0") long channelId,
            HttpServletRequest request
    ) {
        AuthenticatedAccount principal = authRequestContext.requirePrincipal(request);
        return new ChannelBanListResponse(channelQueryDomainApi.listChannelBans(
                new ListChannelBansQuery(principal.accountId(), channelId)
        )
                .stream()
                .map(this::toChannelBanListItemResponse)
                .toList());
    }

    @PutMapping("/{channelId}/notification_preference")
    public ResponseEntity<Void> updateChannelNotificationPreference(
            @PathVariable @Positive(message = "channelId must be greater than 0") long channelId,
            @Valid @RequestBody UpdateChannelNotificationPreferenceRequest body,
            HttpServletRequest request
    ) {
        AuthenticatedAccount principal = authRequestContext.requirePrincipal(request);
        notificationPreferenceDomainApi.updateChannelPreference(new UpdateNotificationChannelPreferenceCommand(
                principal.accountId(),
                channelId,
                body.mode(),
                body.mutedUntil()
        ));
        return ResponseEntity.noContent().build();
    }

    private ChannelSummaryResponse toChannelSummaryResponse(ChannelResult result) {
        return new ChannelSummaryResponse(
                Ids.toString(result.channelId()),
                result.name(),
                result.brief(),
                result.avatar(),
                result.ownerUid()
        );
    }

    private ChannelMemberV1Response toChannelMemberV1Response(ChannelMemberResult result) {
        return new ChannelMemberV1Response(
                Ids.toString(result.accountId()),
                result.role().toLowerCase(),
                result.nickname(),
                result.avatarUrl(),
                result.joinedAt()
        );
    }

    private ChannelBanListItemResponse toChannelBanListItemResponse(ChannelBanListItemResult result) {
        return new ChannelBanListItemResponse(
                Ids.toString(result.channelId()),
                Ids.toString(result.bannedAccountId()),
                result.expiresAt() == null ? null : result.expiresAt().toEpochMilli(),
                result.reason(),
                result.createdAt().toEpochMilli()
        );
    }

    private Long parseOptionalSnowflake(String rawValue, boolean cursorField) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(rawValue.trim());
        } catch (RuntimeException exception) {
            if (cursorField) {
                throw ProblemException.validationFailed("cursor_invalid", "cursor is invalid");
            }
            throw ProblemException.validationFailed("snowflake value is invalid");
        }
    }
}
