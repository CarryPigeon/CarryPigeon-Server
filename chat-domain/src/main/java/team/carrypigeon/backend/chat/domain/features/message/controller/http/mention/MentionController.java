package team.carrypigeon.backend.chat.domain.features.message.controller.http.mention;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.hibernate.validator.constraints.Range;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import team.carrypigeon.backend.chat.domain.config.http.Result;
import team.carrypigeon.backend.chat.domain.config.http.security.CpPrincipal;
import team.carrypigeon.backend.chat.domain.features.message.controller.http.mention.dto.MentionIdsRequest;
import team.carrypigeon.backend.chat.domain.features.message.controller.http.mention.dto.MentionItemResponse;
import team.carrypigeon.backend.chat.domain.features.message.controller.http.mention.dto.MentionItemsResponse;
import team.carrypigeon.backend.chat.domain.features.message.controller.http.mention.dto.UpdateMentionReadStateRequest;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.mention.MentionApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.mention.input.ListMentionsQuery;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.mention.result.MentionPage;
import team.carrypigeon.backend.chat.domain.features.message.domain.problem.MessageProblem;
import team.carrypigeon.backend.infrastructure.basic.page.CursorPageResponse;
import team.carrypigeon.backend.infrastructure.basic.page.OpaqueCursorCodec;

/**
 * 提及收件箱 HTTP 入口。
 */
@RestController
@RequestMapping(value = "/api", produces = "application/json")
@PreAuthorize("isAuthenticated()")
@Tag(name = "提及收件箱", description = "个人和频道提及查询及已读操作。")
public class MentionController {

    private final MentionApi mentionApi;

    public MentionController(MentionApi mentionApi) {
        this.mentionApi = mentionApi;
    }

    /**
     * GET /api/mentions/4001，无请求体。成功返回一条提及，查询时保留已读状态。
     * 提及已撤回、属于其他收件人或频道已不可见时统一返回 30002。
     */
    @GetMapping("/mentions/{mentionId}")
    @Operation(summary = "读取一条提及")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "提及详情或错误 10001、11001、30002", useReturnTypeSchema = true))
    public Result<MentionItemResponse> getMention(
        @Parameter(description = "提及 ID", example = "4001")
        @PathVariable
        @Positive
        long mentionId,
        @AuthenticationPrincipal
        CpPrincipal principal
    ) {
        return Result.success(MentionItemResponse.from(mentionApi.getMention(principal.accountId(), mentionId)));
    }

    /**
     * POST /api/mentions/ids，请求体示例 {@code {"mention_ids":["4002","4001"]}}。
     * 成功返回 items，可见记录按首次出现顺序排列；格式或条数无效返回 10001。
     */
    @PostMapping(value = "/mentions/ids", consumes = {"application/json", "application/*+json"})
    @Operation(summary = "按 ID 批量读取提及", description = "接收 1 至 50 个 ID，略过不可见记录。")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "提及列表或错误 10001、11001", useReturnTypeSchema = true))
    public Result<MentionItemsResponse> getMentions(
        @Valid
        @RequestBody
        MentionIdsRequest request,
        @AuthenticationPrincipal
        CpPrincipal principal
    ) {
        List<MentionItemResponse> items = mentionApi.getMentions(principal.accountId(), request.mentionIds())
                .stream().map(MentionItemResponse::from).toList();
        return Result.success(new MentionItemsResponse(items));
    }

    /**
     * GET /api/mentions?unread_only=true，无请求体。
     * 按 ID 倒序分页，默认 20 条；下一页排除上一页末项，末页或空页的 next_cursor 为 null。
     * 游标与账号、频道和未读筛选绑定，混用返回 10013。
     */
    @GetMapping(value = "/mentions", params = "!cid")
    @Operation(summary = "读取个人提及收件箱", description = "提及 ID 降序，只有还有下一页时才返回游标。")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "分页结果或错误 10001、10013、11001", useReturnTypeSchema = true))
    public Result<CursorPageResponse<MentionItemResponse>> listMentions(
        @Parameter(description = "上一页的 next_cursor")
        @RequestParam(required = false)
        String cursor,
        @Parameter(description = "每页条数，默认 20", example = "20")
        @RequestParam(defaultValue = "20")
        @Range(min = 1, max = 50)
        int limit,
        @Parameter(description = "仅查询未读提及")
        @RequestParam(name = "unread_only", defaultValue = "false")
        boolean unreadOnly,
        @AuthenticationPrincipal
        CpPrincipal principal
    ) {
        String scope = "mentions_" + principal.accountId() + "_all_" + unreadOnly;
        Long before = OpaqueCursorCodec.decode(scope, cursor, MessageProblem.CURSOR_INVALID);
        var query = new ListMentionsQuery(principal.accountId(), before, limit, unreadOnly);
        return Result.success(toResponse(scope, mentionApi.listMentions(query)));
    }

    /**
     * GET /api/channels/2001/mentions?unread_only=true，无请求体。
     * 按 ID 倒序分页，默认 20 条；下一页排除上一页末项，末页或空页的 next_cursor 为 null。
     * 游标与账号、频道和未读筛选绑定，混用返回 10013。
     */
    @GetMapping("/channels/{channelId}/mentions")
    @Operation(summary = "读取频道提及收件箱", description = "提及 ID 降序，只有还有下一页时才返回游标。")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "分页结果或错误 10001、10013、11001、20001、20002", useReturnTypeSchema = true))
    public Result<CursorPageResponse<MentionItemResponse>> listChannelMentions(
        @Parameter(description = "频道 ID", example = "2001")
        @PathVariable
        @Positive
        long channelId,
        @Parameter(description = "上一页的 next_cursor")
        @RequestParam(required = false)
        String cursor,
        @Parameter(description = "每页条数，默认 20", example = "20")
        @RequestParam(defaultValue = "20")
        @Range(min = 1, max = 50)
        int limit,
        @Parameter(description = "仅查询未读提及")
        @RequestParam(name = "unread_only", defaultValue = "false")
        boolean unreadOnly,
        @AuthenticationPrincipal
        CpPrincipal principal
    ) {
        String scope = "mentions_" + principal.accountId() + "_" + channelId + "_" + unreadOnly;
        Long before = OpaqueCursorCodec.decode(scope, cursor, MessageProblem.CURSOR_INVALID);
        var query = new ListMentionsQuery(principal.accountId(), before, limit, unreadOnly);
        return Result.success(toResponse(scope, mentionApi.listChannelMentions(channelId, query)));
    }

    /** PUT /api/mentions/4001/read，无请求体。重复操作成功，data 为 null；不可见返回 30002。 */
    @PutMapping("/mentions/{mentionId}/read")
    @Operation(summary = "标记单条提及已读")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "成功或错误 10001、11001、30002", useReturnTypeSchema = true))
    public Result<Void> markMentionRead(
        @Parameter(description = "提及 ID", example = "4001")
        @PathVariable
        @Positive
        long mentionId,
        @AuthenticationPrincipal
        CpPrincipal principal
    ) {
        mentionApi.markMentionRead(principal.accountId(), mentionId);
        return Result.success();
    }

    /**
     * PUT /api/channels/2001/mentions/read。无请求体时更新范围内全部提及，
     * 提供 {@code {"through_mention_id":"4002"}} 时只更新 ID 小于等于 4002 的记录。
     * 成功时 data 为 null，包括范围内没有未读记录的情况。
     */
    @PutMapping("/channels/{channelId}/mentions/read")
    @Operation(summary = "标记当前频道提及已读", description = "可选上界包含指定提及。")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "成功或错误 10001、11001、20001、20002", useReturnTypeSchema = true))
    public Result<Void> markChannelMentionsRead(
        @Parameter(description = "频道 ID", example = "2001")
        @PathVariable
        @Positive
        long channelId,
        @Valid
        @RequestBody(required = false)
        UpdateMentionReadStateRequest body,
        @AuthenticationPrincipal
        CpPrincipal principal
    ) {
        mentionApi.markChannelMentionsRead(principal.accountId(), channelId, body == null ? null : body.throughMentionId());
        return Result.success();
    }

    /**
     * PUT /api/mentions/read。无请求体时更新范围内全部提及，
     * 提供 {@code {"through_mention_id":"4002"}} 时只更新 ID 小于等于 4002 的记录。
     * 成功时 data 为 null，包括范围内没有未读记录的情况。
     */
    @PutMapping("/mentions/read")
    @Operation(summary = "标记个人收件箱提及已读", description = "可选上界包含指定提及。")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "成功或错误 10001、11001", useReturnTypeSchema = true))
    public Result<Void> markMentionsRead(
        @Valid
        @RequestBody(required = false)
        UpdateMentionReadStateRequest body,
        @AuthenticationPrincipal
        CpPrincipal principal
    ) {
        mentionApi.markMentionsRead(principal.accountId(), body == null ? null : body.throughMentionId());
        return Result.success();
    }

    private CursorPageResponse<MentionItemResponse> toResponse(String scope, MentionPage page) {
        return CursorPageResponse.of(
            page.items().stream().map(MentionItemResponse::from).toList(),
            OpaqueCursorCodec.encode(scope, page.nextMentionId())
        );
    }
}
