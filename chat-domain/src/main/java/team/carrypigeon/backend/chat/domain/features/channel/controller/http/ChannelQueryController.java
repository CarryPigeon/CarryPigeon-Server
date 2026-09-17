package team.carrypigeon.backend.chat.domain.features.channel.controller.http;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import team.carrypigeon.backend.chat.domain.features.channel.controller.dto.ChannelListResponse;
import team.carrypigeon.backend.chat.domain.features.channel.controller.dto.ChannelSummaryResponse;
import team.carrypigeon.backend.chat.domain.features.channel.controller.dto.DiscoverChannelResponse;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.ChannelQueryApi;
import team.carrypigeon.backend.chat.domain.features.channel.domain.projection.ChannelResult;
import team.carrypigeon.backend.chat.domain.features.channel.domain.projection.DiscoverChannelResult;
import team.carrypigeon.backend.chat.domain.features.channel.domain.query.DiscoverChannelsQuery;
import team.carrypigeon.backend.chat.domain.shared.controller.CursorPageResponse;
import team.carrypigeon.backend.chat.domain.shared.controller.OpaqueCursorCodec;
import team.carrypigeon.backend.chat.domain.shared.controller.support.RequestAuthenticationContext;
import team.carrypigeon.backend.chat.domain.shared.domain.auth.AuthenticatedAccount;
import team.carrypigeon.backend.infrastructure.basic.id.IdUtil;

/**
 * 频道查询 HTTP 入口。
 * 职责：提供当前用户可见频道、频道详情与频道发现查询。
 * 边界：只转换查询协议，不承载频道生命周期或治理操作。
 */
@Validated
@RestController
@RequestMapping("/api/channels")
@PreAuthorize("isAuthenticated()")
@Tag(name = "频道与成员", description = "频道查询、成员治理、申请、发现与封禁能力。")
public class ChannelQueryController {

    private static final String DISCOVER_CURSOR_SCOPE = "channel_discover";

    private final ChannelQueryApi channelQueryDomainApi;
    private final RequestAuthenticationContext authRequestContext;

    @Autowired
    public ChannelQueryController(
            ChannelQueryApi channelQueryDomainApi,
            RequestAuthenticationContext authRequestContext
    ) {
        this.channelQueryDomainApi = channelQueryDomainApi;
        this.authRequestContext = authRequestContext;
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

    /**
     * 按频道 ID 返回当前用户可见的频道资料。
     */
    @GetMapping("/{channelId}")
    @Operation(summary = "获取频道资料", description = "按频道 ID 返回当前可见频道摘要。")
    public ChannelSummaryResponse getChannelById(
            @PathVariable @Positive(message = "channelId must be greater than 0") long channelId,
            HttpServletRequest request
    ) {
        AuthenticatedAccount principal = authRequestContext.requirePrincipal(request);
        return toChannelSummaryResponse(channelQueryDomainApi.getChannelById(principal.accountId(), channelId));
    }

    /**
     * 按关键字、类型和不透明游标发现频道。
     */
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
        String nextCursor = hasMore
                ? OpaqueCursorCodec.encode(
                        DISCOVER_CURSOR_SCOPE,
                        Long.parseLong(pageItems.get(pageItems.size() - 1).cid())
                )
                : null;
        return CursorPageResponse.of(pageItems.stream().map(item -> new DiscoverChannelResponse(
                item.cid(),
                item.name(),
                item.brief(),
                item.avatar(),
                item.memberCount(),
                item.requiresApplication()
        )).toList(), nextCursor, hasMore);
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
