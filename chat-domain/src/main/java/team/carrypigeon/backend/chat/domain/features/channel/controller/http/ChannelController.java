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
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import team.carrypigeon.backend.chat.domain.config.http.security.CpPrincipal;
import team.carrypigeon.backend.chat.domain.features.channel.controller.http.dto.ChannelResponseBodyList;
import team.carrypigeon.backend.chat.domain.features.channel.controller.http.dto.ChannelResponseBody;
import team.carrypigeon.backend.chat.domain.features.channel.controller.http.dto.CreateChannelRequest;
import team.carrypigeon.backend.chat.domain.features.channel.controller.http.dto.DiscoverChannelResponse;
import team.carrypigeon.backend.chat.domain.features.channel.controller.http.dto.UpdateChannelProfileRequest;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.channel.ChannelApi;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.channel.ChannelQueryApi;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.channel.result.ChannelResult;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.channel.result.DiscoverChannelResult;
import team.carrypigeon.backend.chat.domain.features.channel.domain.problem.ChannelProblem;
import team.carrypigeon.backend.chat.domain.config.http.CursorPageResponse;
import team.carrypigeon.backend.chat.domain.config.http.OpaqueCursorCodec;
import team.carrypigeon.backend.chat.domain.config.http.error.ApiErrorResponse;
import team.carrypigeon.backend.infrastructure.basic.id.IdUtil;

/**
 * 频道基础 HTTP 入口。
 * 职责：承接频道列表、资料读取、发现、创建、删除与资料更新协议。
 * 边界：只做协议映射与领域 API 调用，不承载频道可见性、权限和资料校验规则。
 */
@RestController
@RequestMapping("/api/channels")
@PreAuthorize("isAuthenticated()")
@Validated
@Tag(name = "频道", description = "频道基础信息与频道发现能力。")
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
     * 返回当前账号存在成员关系的频道列表<br/>
     */
    @GetMapping
    @Operation(
            summary = "获取频道列表",
            description = "返回当前登录账号存在成员关系的频道列表。无可见频道时返回空数组，不返回 404。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "响应体为频道列表外壳，channels 为频道摘要数组",
                    content = @Content(schema = @Schema(implementation = ChannelResponseBodyList.class))),
            @ApiResponse(responseCode = "401", description = "未认证，响应体为标准错误包装",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<ChannelResponseBodyList> listChannels(
        @AuthenticationPrincipal CpPrincipal cpPrincipal
    ) {
        return ResponseEntity.ok().body(new ChannelResponseBodyList(channelApi.listChannels(cpPrincipal.accountId()).stream()
            .map(
                result -> new ChannelResponseBody(
                    IdUtil.toString(result.channelId()),
                    result.name(),
                    result.brief(),
                    result.systemChannel(),
                    result.createdAt().toEpochMilli()
                )
            )
            .toList()));
    }

    /**
     * 按频道 ID 返回频道资料。
     * 约束：调用方必须是该频道成员，否则拒绝访问。
     *
     * @param channelId 目标频道 ID
     * @param cpPrincipal 当前认证用户
     * @return 频道资料响应
     */
    @GetMapping("/{channelId}")
    @Operation(
            summary = "获取频道资料",
            description = "按频道 ID 返回频道名称、简介、系统频道标记与创建时间。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "响应体为频道详情，含 cid、name、brief、system_channel、created_at",
                    content = @Content(schema = @Schema(implementation = ChannelResponseBody.class))),
            @ApiResponse(responseCode = "401", description = "未认证，响应体为标准错误包装",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "当前账号不是该频道成员，reason 为 channel_membership_required",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "频道不存在",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ChannelResponseBody getChannelById(
        @Parameter(description = "目标频道 ID", example = "2001")
        @PathVariable @Positive(message = "channelId must be greater than 0") long channelId,
        @AuthenticationPrincipal CpPrincipal cpPrincipal
    ) {
        ChannelResult result = channelApi.getChannelById(cpPrincipal.accountId(), channelId);
        return new ChannelResponseBody(
            IdUtil.toString(result.channelId()),
            result.name(),
            result.brief(),
            result.systemChannel(),
            result.createdAt().toEpochMilli()
        );
    }

    /**
     * TODO 待审核
     * 按关键字与不透明游标发现频道。
     * 约束：游标由本接口签发，非法或过期的游标按参数错误拒绝。
     *
     * @param keyword 频道名称或简介关键字；为空表示不过滤
     * @param cursor 不透明分页游标；为空表示从第一页开始
     * @param limit 单页返回条数，范围 1..50
     * @param cpPrincipal 当前认证用户
     * @return 游标分页的频道发现结果
     */
    @GetMapping("/discover")
    @Operation(
            summary = "发现频道",
            description = "按关键字分页发现频道。响应体为通用游标外壳：items 为频道摘要数组，"
                    + "next_cursor 为空表示没有更多数据，has_more 表示是否还有下一页。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "响应体为 items + next_cursor + has_more 的游标分页结构"),
            @ApiResponse(responseCode = "401", description = "未认证，响应体为标准错误包装",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = "游标非法或 limit 超出 1..50，reason 为 validation_failed 或 cursor_invalid",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public CursorPageResponse<DiscoverChannelResponse> discoverChannels(
        @Parameter(description = "频道名称或简介关键字", example = "技术")
        @RequestParam(name = "q", required = false) String keyword,
        @Parameter(description = "上一页返回的分页游标", example = "Y2hhbm5lbF9kaXNjb3ZlcjoyMDAx")
        @RequestParam(required = false) String cursor,
        @Parameter(description = "单页返回条数，范围 1..50", example = "20")
        @RequestParam(defaultValue = "20")
        @Min(value = 1, message = "limit must be between 1 and 50")
        @Max(value = 50, message = "limit must be between 1 and 50") int limit,
        @AuthenticationPrincipal CpPrincipal cpPrincipal
    ) {
        Long cursorChannelId = OpaqueCursorCodec.decode("channel_discover", cursor, ChannelProblem.CURSOR_INVALID);
        List<DiscoverChannelResult> items = channelQueryApi.discoverChannels(cpPrincipal.accountId(), keyword, cursorChannelId, limit);
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
     * 创建频道并把创建者设为所有者<br/>
     * 请求示例：
     * ```json
     * {
     *     "name": "频道名称",
     *     "brief": "频道简介"
     * }
     * ```
     */
    @PostMapping
    @Operation(
            summary = "创建频道",
            description = "创建普通频道并把创建者设为其所有者。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "响应体为新建频道摘要项，含 cid、name、brief",
                    content = @Content(schema = @Schema(implementation = ChannelResponseBody.class))),
            @ApiResponse(responseCode = "401", description = "未认证，响应体为标准错误包装",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = "请求体字段非法，reason 为 validation_failed",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<ChannelResponseBody> createChannel(
        @Valid @RequestBody CreateChannelRequest body,
        @AuthenticationPrincipal CpPrincipal cpPrincipal
    ) {
        ChannelResult result = channelApi.createChannel(cpPrincipal.accountId(), body.name(), body.brief());
        return ResponseEntity.status(201).body(new ChannelResponseBody(
            IdUtil.toString(result.channelId()),
            result.name(),
            result.brief(),
            result.systemChannel(),
            result.createdAt().toEpochMilli()
        ));
    }

    /**
     * 删除指定频道。
     */
    @DeleteMapping("/{channelId}")
    @Operation(
            summary = "删除频道",
            description = "删除指定频道及其全部成员关系。成功返回 204，响应体为空。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "删除成功，响应体为空"),
            @ApiResponse(responseCode = "401", description = "未认证，响应体为标准错误包装",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "非频道成员或不是所有者，reason 为 not_channel_member 或 channel_owner_required",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "频道不存在",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "频道仍存在申请或禁言等依赖数据，reason 为 channel_delete_blocked",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<Void> deleteChannel(
        @Parameter(description = "目标频道 ID", example = "2001")
        @PathVariable @Positive(message = "channelId must be greater than 0") long channelId,
        @AuthenticationPrincipal CpPrincipal cpPrincipal
    ) {
        channelApi.deleteChannel(cpPrincipal.accountId(), channelId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 更新指定频道的名称与简介
     * <br/>
     * 请求示例：
     * ```json
     * {
     *     "name": "频道名称",
     *     "brief": "频道简介"
     * }
     * ```
     */
    @PatchMapping("/{channelId}")
    @Operation(
            summary = "更新频道资料",
            description = "更新频道名称与简介，要求 OWNER 或 ADMIN 成员角色。成功返回 204，响应体为空。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "更新成功，响应体为空"),
            @ApiResponse(responseCode = "401", description = "未认证，响应体为标准错误包装",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "非频道成员或角色不足，reason 为 not_channel_member 或 channel_profile_forbidden",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "频道不存在",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = "请求体字段非法，reason 为 validation_failed",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<Void> updateChannelProfile(
        @Parameter(description = "目标频道 ID", example = "2001")
        @PathVariable @Positive(message = "channelId must be greater than 0") long channelId,
        @Valid @RequestBody UpdateChannelProfileRequest body,
        @AuthenticationPrincipal CpPrincipal cpPrincipal
    ) {
        channelApi.updateChannelProfile(cpPrincipal.accountId(), channelId, body.name(), body.brief());
        return ResponseEntity.noContent().build();
    }
}
