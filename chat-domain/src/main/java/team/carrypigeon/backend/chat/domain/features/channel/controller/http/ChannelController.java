package team.carrypigeon.backend.chat.domain.features.channel.controller.http;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import team.carrypigeon.backend.chat.domain.http.PageResponse;
import team.carrypigeon.backend.chat.domain.http.Result;
import team.carrypigeon.backend.chat.domain.http.security.CpPrincipal;
import team.carrypigeon.backend.chat.domain.features.channel.controller.http.dto.*;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.channel.ChannelApi;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.channel.ChannelQueryApi;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.channel.result.ChannelResult;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.channel.result.DiscoverChannelResult;
import team.carrypigeon.backend.chat.domain.features.channel.domain.problem.ChannelProblem;
import team.carrypigeon.backend.chat.domain.shared.id.IdUtil;
import team.carrypigeon.backend.chat.domain.shared.cursor.CursorPageResponse;
import team.carrypigeon.backend.chat.domain.shared.cursor.OpaqueCursorCodec;
import team.carrypigeon.backend.chat.domain.shared.page.PageQuery;
import team.carrypigeon.backend.chat.domain.shared.page.PageResult;

/**
 * 频道基础 HTTP 入口。
 * 职责：承接频道列表、资料读取、发现、创建、删除与资料更新协议。
 * 边界：只做协议映射与领域 API 调用，不承载频道可见性、权限和资料校验规则。
 */
@RestController
@RequestMapping("/api/channels")
@PreAuthorize("isAuthenticated()")
@Tag(name = "频道", description = "频道基础信息与频道发现能力。")
public class ChannelController {

    private static final String DISCOVER_CURSOR_SCOPE = "channel_discover";

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
     * 查询所有普通频道的公开摘要，系统频道隐藏；发现结果不授予资料或消息读取权限。
     *
     * <p>请求 {@code GET /api/channels/discover?q=project&limit=20}。无请求体。
     * <p>后续页把上一页返回的 {@code next_cursor} 原样传回 {@code cursor}，不要解析或拼接。
     * <p>成功响应 HTTP 200，code 为 0，data 为 {@code CursorPageResponse<DiscoverChannelResponse>}。
     * <p>失败示例：查询参数错误 10001，游标无效 10013。
     */
    @GetMapping(value = "/discover", produces = "application/json")
    @Operation(summary = "发现频道", description = "按频道 ID 升序发现普通频道，系统频道隐藏。关键词按名称或简介包含匹配；加入频道仍需申请审批，消息仅成员可读。next_cursor 为不透明游标，必须原样回传。")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "成功时 data 为 CursorPageResponse<DiscoverChannelResponse>，失败时返回五位错误码", useReturnTypeSchema = true))
    public Result<CursorPageResponse<DiscoverChannelResponse>> discoverChannels(
        @Parameter(description = "名称或简介关键词，省略或空白时不过滤")
        @RequestParam(name = "q", required = false)
        @Size(max = 128, message = "q must contain at most 128 characters")
        String keyword,
        @Parameter(description = "上一页返回的 next_cursor，不透明游标，不能解析或拼接")
        @RequestParam(required = false)
        String cursor,
        @RequestParam(defaultValue = "20")
        @Min(value = 1, message = "limit must be between 1 and 50")
        @Max(value = 50, message = "limit must be between 1 and 50")
        int limit,
        @AuthenticationPrincipal
        CpPrincipal principal
    ) {
        Long cursorChannelId = OpaqueCursorCodec.decode(DISCOVER_CURSOR_SCOPE, cursor, ChannelProblem.CURSOR_INVALID);
        List<DiscoverChannelResult> found = channelQueryApi.discoverChannels(
                principal.accountId(), keyword, cursorChannelId, limit);
        boolean hasMore = found.size() > limit;
        List<DiscoverChannelResult> items = hasMore ? found.subList(0, limit) : found;
        String nextCursor = hasMore
                ? OpaqueCursorCodec.encode(DISCOVER_CURSOR_SCOPE, items.getLast().cid())
                : null;

        return Result.success(CursorPageResponse.of(items.stream().map(item -> new DiscoverChannelResponse(
                Long.toString(item.cid()), item.name(), item.brief(), item.avatar(),
                item.memberCount(), item.requiresApplication())).toList(),
                nextCursor, hasMore));
    }

    /**
     * 返回当前账号存在成员关系的频道列表
     *
     * <p>请求 {@code GET /api/channels?page=1&page_size=20}。无请求体。
     * <p>成功响应 HTTP 200，code 为 0，data 为 {@code PageResponse<ChannelResponseBody>}。
     * <p>失败示例：分页参数错误 10001。
     */
    @GetMapping(produces = "application/json")
    @Operation(
            summary = "获取频道列表",
            description = "分页返回当前登录账号存在成员关系的频道列表。无可见频道时返回空 items。"
    )
    @ApiResponses(@ApiResponse(responseCode = "200", description = "data 为分页外壳，items 为频道摘要数组", useReturnTypeSchema = true))
    public Result<PageResponse<ChannelResponseBody>> listChannels(
        @Parameter(description = "页码，从 1 开始", example = "1")
        @RequestParam(name = "page", defaultValue = "1")
        @Min(value = 1, message = "page must be greater than 0")
        int page,
        @Parameter(description = "每页条数，范围 1..50", example = "20")
        @RequestParam(name = "page_size", defaultValue = "20")
        @Min(value = 1, message = "page_size must be between 1 and 50")
        @Max(value = 50, message = "page_size must be between 1 and 50")
        int pageSize,
        @AuthenticationPrincipal
        CpPrincipal cpPrincipal
    ) {
        PageResult<ChannelResult> result = channelApi.listChannels(
                cpPrincipal.accountId(),
                new PageQuery(page, pageSize)
        );
        return Result.success(new PageResponse<>(
                result.items().stream()
                        .map(channel -> new ChannelResponseBody(
                                IdUtil.toString(channel.channelId()),
                                channel.name(),
                                channel.brief(),
                                channel.systemChannel(),
                                channel.createdAt().toEpochMilli()
                        ))
                        .toList(),
                result.page(),
                result.pageSize(),
                result.total(),
                result.hasMore()
        ));
    }

    /**
     * 按频道 ID 返回频道资料。
     * 约束：调用方必须是该频道成员，否则拒绝访问。
     *
     * <p>请求 {@code GET /api/channels/2001}。无请求体。
     * <p>成功响应 HTTP 200，code 为 0，data 为 {@code ChannelResponseBody}。
     * <p>失败示例：频道不存在 20001
     *
     * @param channelId 目标频道 ID
     */
    @GetMapping(value = "/{channelId}", produces = "application/json")
    @Operation(
            summary = "获取频道资料",
            description = "按频道 ID 返回频道名称、简介、系统频道标记与创建时间。"
    )
    @ApiResponses(@ApiResponse(responseCode = "200", description = "data 为频道详情，含 cid、name、brief、system_channel、created_at", useReturnTypeSchema = true))
    public Result<ChannelResponseBody> getChannelById(
        @Parameter(description = "目标频道 ID", example = "2001")
        @PathVariable
        @Positive(message = "channelId must be greater than 0")
        long channelId
    ) {
        ChannelResult result = channelApi.getChannelById(channelId);
        return Result.success(new ChannelResponseBody(
            IdUtil.toString(result.channelId()),
            result.name(),
            result.brief(),
            result.systemChannel(),
            result.createdAt().toEpochMilli()
        ));
    }

    /**
     * 退出频道
     * 约束：调用方必须是该频道成员，否则拒绝访问。
     *
     * <p>请求 {@code POST /api/channels/2001/quit}。无请求体。
     * <p>成功响应 HTTP 200，code 为 0，data 为 {@code null}。
     * <p>失败示例：频道不存在 20001，要求成员 20007。
     */
    @PostMapping(value = "/{channelId}/quit", produces = "application/json")
    @Operation(
            summary = "退出频道",
            description = "提交后立即退出频道，关联数据由后台异步清理。返回 200，data 为 null；频道不存在时返回业务码 20001。"
    )
    @ApiResponses(@ApiResponse(responseCode = "200", description = "成功时 code 为 0 且 data 为 null", useReturnTypeSchema = true))
    public Result<Void> quitChannel(
        @Parameter(description = "目标频道 ID", example = "2001")
        @PathVariable
        @Positive(message = "channelId must be greater than 0")
        long channelId,
        @AuthenticationPrincipal
        CpPrincipal cpPrincipal
    ) {
        channelApi.quitChannel(cpPrincipal.accountId(), channelId);
        return Result.success();
    }

    /**
     * 转让频道
     * 约束：调用方必须是该频道所有者，否则拒绝访问。
     *
     * <p>请求 {@code PUT /api/channels/2001/transfer}。JSON 请求体示例 {@code {"new_owner": "1001"}}。
     * <p>成功响应 HTTP 200，code 为 0，data 为 {@code null}。
     * <p>失败示例：频道不存在 20001，要求所有者 20006，用户不存在 20002，用户不在频道 20003。
     *
     * @param channelId 目标频道 ID
     * @param body 转让请求体，包含新所有者 ID
     */
    @PutMapping(value = "/{channelId}/transfer", produces = "application/json")
    @Operation(
            summary = "转让频道",
            description = "提交后频道所有者变更为 new_owner，原所有者不再拥有该频道。"
    )
    @ApiResponses(@ApiResponse(responseCode = "200", description = "成功时 code 为 0 且 data 为 null", useReturnTypeSchema = true))
    public Result<Void> transferChannel(
        @Parameter(description = "目标频道 ID", example = "2001")
        @PathVariable
        @Positive(message = "channelId must be greater than 0")
        long channelId,
        @Parameter(description = "转让请求体，包含新所有者 ID", example = "{\"new_owner\": \"1001\"}")
        @RequestBody
        TransferChannelRequest body,
        @AuthenticationPrincipal
        CpPrincipal cpPrincipal
    ) {
        long newOwnerId = Long.parseLong(body.newOwner());
        // 数据校验
        if (newOwnerId <= 0) {
            throw new IllegalArgumentException("newOwnerId must be greater than 0");
        }
        channelApi.transferChannel(cpPrincipal.accountId(), channelId, newOwnerId);
        return Result.success();
    }

    /**
     * 按频道 ID 列表返回频道资料。
     * 约束：调用方必须是该频道成员，否则拒绝访问。
     *
     * <p>请求 {@code POST /api/channels/list}。JSON 请求体示例 {@code [2001, 2002]}。
     * <p>成功响应 HTTP 200，code 为 0，data 为 {@code ChannelListResponseBody}。
     * <p>失败示例：频道不存在 20001
     *
     * @param channelIds 目标频道 ID 列表
     */
    @PostMapping(value = "/list", produces = "application/json")
    @Operation(
            summary = "获取频道列表",
            description = "按频道 ID 列表返回频道名称、简介、系统频道标记与创建时间。"
    )
    @ApiResponses(@ApiResponse(responseCode = "200", description = "data 为频道详情数组，含 cid、name、brief、system_channel、created_at", useReturnTypeSchema = true))
    public Result<ChannelListResponseBody> getChannelByIds(
        @Parameter(description = "目标频道 ID 列表", example = "[2001, 2002]")
        @RequestBody
        List<Long> channelIds
    ) {
        List<ChannelResult> results = channelApi.getChannelByIds(channelIds);
        return Result.success(new ChannelListResponseBody(
            results.size(),
            results.stream()
                .map(channel -> new ChannelResponseBody(
                    IdUtil.toString(channel.channelId()),
                    channel.name(),
                    channel.brief(),
                    channel.systemChannel(),
                    channel.createdAt().toEpochMilli()
                ))
                .toList()
        ));
    }

    /**
     * 创建频道并把创建者设为所有者
     *
     * <p>请求 {@code POST /api/channels}。JSON 请求体示例 {@code {"name":"项目讨论","brief":"版本计划"}}。
     * <p>成功响应 HTTP 200，code 为 0，data 为 {@code ChannelResponseBody}。
     * <p>失败示例：字段校验失败 10001。
     */
    @PostMapping(produces = "application/json", consumes = {"application/json", "application/*+json"})
    @Operation(
            summary = "创建频道",
            description = "创建普通频道并把创建者设为其所有者。"
    )
    @ApiResponses(@ApiResponse(responseCode = "200", description = "data 为新建频道资料", useReturnTypeSchema = true))
    public Result<ChannelResponseBody> createChannel(
        @Valid
        @RequestBody
        CreateChannelRequest body,
        @AuthenticationPrincipal
        CpPrincipal cpPrincipal
    ) {
        ChannelResult result = channelApi.createChannel(cpPrincipal.accountId(), body.name(), body.brief());
        return Result.success(new ChannelResponseBody(
            IdUtil.toString(result.channelId()),
            result.name(),
            result.brief(),
            result.systemChannel(),
            result.createdAt().toEpochMilli()
        ));
    }

    /**
     * 删除指定频道。
     *
     * <p>请求 {@code DELETE /api/channels/2001}。无请求体。
     * <p>成功响应 HTTP 200，code 为 0，data 为 {@code null}。
     * <p>失败示例：频道不存在 20001，要求所有者 20006。
     */
    @DeleteMapping(value = "/{channelId}", produces = "application/json")
    @Operation(
            summary = "删除频道",
            description = "提交后频道立即不可访问，关联数据由后台异步清理。返回 200，data 为 null；频道不存在时返回业务码 20001。"
    )
    @ApiResponses(@ApiResponse(responseCode = "200", description = "成功时 code 为 0 且 data 为 null", useReturnTypeSchema = true))
    public Result<Void> deleteChannel(
        @Parameter(description = "目标频道 ID", example = "2001")
        @PathVariable
        @Positive(message = "channelId must be greater than 0")
        long channelId,
        @AuthenticationPrincipal
        CpPrincipal cpPrincipal
    ) {
        channelApi.deleteChannel(cpPrincipal.accountId(), channelId);
        return Result.success();
    }

    /**
     * 更新指定频道的名称与简介
     *
     * <p>请求 {@code PATCH /api/channels/2001}。JSON 请求体示例 {@code {"name":"项目讨论","brief":"版本计划"}}。
     * <p>成功响应 HTTP 200，code 为 0，data 为 {@code null}。
     * <p>失败示例：频道不存在 20001，角色不足 20010。
     */
    @PatchMapping(value = "/{channelId}", produces = "application/json", consumes = {"application/json", "application/*+json"})
    @Operation(
            summary = "更新频道资料",
            description = "更新频道名称与简介，要求 OWNER 或 ADMIN 成员角色。成功返回 200，data 为 null。"
    )
    @ApiResponses(@ApiResponse(responseCode = "200", description = "成功时 code 为 0 且 data 为 null", useReturnTypeSchema = true))
    public Result<Void> updateChannelProfile(
        @Parameter(description = "目标频道 ID", example = "2001")
        @PathVariable
        @Positive(message = "channelId must be greater than 0")
        long channelId,
        @Valid
        @RequestBody
        UpdateChannelProfileRequest body,
        @AuthenticationPrincipal
        CpPrincipal cpPrincipal
    ) {
        channelApi.updateChannelProfile(cpPrincipal.accountId(), channelId, body.name(), body.brief());
        return Result.success();
    }
}
