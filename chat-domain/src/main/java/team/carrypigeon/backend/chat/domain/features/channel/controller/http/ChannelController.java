package team.carrypigeon.backend.chat.domain.features.channel.controller.http;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import team.carrypigeon.backend.chat.domain.config.http.security.CpPrincipal;
import team.carrypigeon.backend.chat.domain.features.channel.controller.http.dto.ChannelListResponse;
import team.carrypigeon.backend.chat.domain.features.channel.controller.http.dto.ChannelListResponseItem;
import team.carrypigeon.backend.chat.domain.features.channel.controller.http.dto.ChannelQueryResponse;
import team.carrypigeon.backend.chat.domain.features.channel.controller.http.dto.CreateChannelRequest;
import team.carrypigeon.backend.chat.domain.features.channel.controller.http.dto.DiscoverChannelResponse;
import team.carrypigeon.backend.chat.domain.features.channel.controller.http.dto.UpdateChannelProfileRequest;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.channel.ChannelApi;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.channel.ChannelQueryApi;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.channel.input.CreateChannelCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.channel.input.DeleteChannelCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.channel.input.DiscoverChannelsQuery;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.channel.input.UpdateChannelProfileCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.channel.result.ChannelResult;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.channel.result.DiscoverChannelResult;
import team.carrypigeon.backend.chat.domain.shared.controller.CursorPageResponse;
import team.carrypigeon.backend.chat.domain.shared.controller.OpaqueCursorCodec;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.id.IdUtil;

/**
 * 频道基础 HTTP 入口。
 * 职责：提供频道的基础功能
 * 边界：只提供频道的基本功能，如何创建更新等
 */
@Validated
@RestController
@RequestMapping("/api/channels")
@PreAuthorize("isAuthenticated()")
@Tag(name = "频道", description = "提供频道管理的基础性能力。")
public class ChannelController {

    private final ChannelApi channelApi;
    private final ChannelQueryApi channelQueryApi;

    public ChannelController(
        ChannelApi channelApi,
        ChannelQueryApi channelQueryApi
    ) {
        this.channelApi = channelApi;
        this.channelQueryApi = channelQueryApi;
    }

    /**
     * 返回当前用户可见的频道列表。
     *
     * @return 频道摘要列表，用于客户端简略渲染
     */
    @GetMapping
    @Operation(summary = "获取频道列表", description = "返回当前登录用户当前可见的频道列表。")
    public ChannelListResponse listChannels(
        @AuthenticationPrincipal CpPrincipal cpPrincipal
    ) {
        return new ChannelListResponse(channelApi.listChannels(cpPrincipal.accountId()).stream()
            .map(
                result -> new ChannelListResponseItem(
                    IdUtil.toString(result.channelId()),
                    result.name(),
                    result.brief()
                )
            )
            .toList());
    }

    /**
     * 按频道 ID 返回当前用户可见的频道资料。
     */
    @GetMapping("/{channelId}")
    @Operation(summary = "获取频道资料", description = "按频道 ID 返回当前可见频道摘要。")
    public ChannelQueryResponse getChannelById(
        @PathVariable @Positive(message = "channelId must be greater than 0") long channelId,
        @AuthenticationPrincipal CpPrincipal cpPrincipal
    ) {
        ChannelResult result = channelApi.getChannelById(cpPrincipal.accountId(), channelId);
        return new ChannelQueryResponse(
            IdUtil.toString(result.channelId()),
            result.name(),
            result.brief(),
            result.systemChannel(),
            result.createdAt().toEpochMilli()
        );
    }

    /**
     * 按关键字、类型和不透明游标发现频道。
     * TODO 待审核
     */
    @GetMapping("/discover")
    public CursorPageResponse<DiscoverChannelResponse> discoverChannels(
        @RequestParam(name = "q", required = false) String keyword,
        @RequestParam(required = false) String cursor,
        @RequestParam(defaultValue = "20")
        @Min(value = 1, message = "limit must be between 1 and 50")
        @Max(value = 50, message = "limit must be between 1 and 50") int limit,
        @AuthenticationPrincipal CpPrincipal cpPrincipal
    ) {
        Long cursorChannelId = OpaqueCursorCodec.decode("channel_discover", cursor);
        if (cursorChannelId != null && cursorChannelId <= 0) {
            throw ProblemException.validationFailed("cursor_invalid", "cursor is invalid");
        }
        List<DiscoverChannelResult> items = channelQueryApi.discoverChannels(new DiscoverChannelsQuery(
            cpPrincipal.accountId(),
            keyword,
            OpaqueCursorCodec.decode("channel_discover", cursor),
            limit
        ));
        boolean hasMore = items.size() > limit;
        List<DiscoverChannelResult> pageItems = hasMore ? items.subList(0, limit) : items;
        String nextCursor = hasMore
            ? OpaqueCursorCodec.encode(
            "channel_discover",
            pageItems.getLast().cid()
        )
            : null;
        return CursorPageResponse.of(pageItems.stream().map(item -> new DiscoverChannelResponse(
            item.cid()+"",
            item.name(),
            item.brief(),
            item.avatar(),
            item.memberCount(),
            item.requiresApplication()
        )).toList(), nextCursor, hasMore);
    }


    /**
     * 创建频道并返回新频道摘要。
     */
    @PostMapping
    @Operation(summary = "创建频道", description = "创建频道")
    @ApiResponses({@ApiResponse(responseCode = "201", description = "频道创建成功")})
    public ResponseEntity<ChannelListResponseItem> createChannel(
        @Valid @RequestBody CreateChannelRequest body,
        @AuthenticationPrincipal CpPrincipal cpPrincipal
    ) {
        ChannelResult result = channelApi.createChannel(new CreateChannelCommand(
            cpPrincipal.accountId(),
            body.name(),
            body.brief(),
            body.avatar()
        ));
        return ResponseEntity.status(201).body(new ChannelListResponseItem(
            IdUtil.toString(result.channelId()),
            result.name(),
            result.brief()
        ));
    }

    /**
     * 删除指定频道。
     */
    @DeleteMapping("/{channelId}")
    @Operation(summary = "删除频道", description = "删除指定频道。")
    @ApiResponses({@ApiResponse(responseCode = "204", description = "频道删除成功")})
    public ResponseEntity<Void> deleteChannel(
        @PathVariable @Positive(message = "channelId must be greater than 0") long channelId,
        @AuthenticationPrincipal CpPrincipal cpPrincipal
    ) {
        channelApi.deleteChannel(new DeleteChannelCommand(cpPrincipal.accountId(), channelId));
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
        channelApi.updateChannelProfile(new UpdateChannelProfileCommand(
            cpPrincipal.accountId(),
            channelId,
            body.name(),
            body.brief()
        ));
        return ResponseEntity.noContent().build();
    }
}
