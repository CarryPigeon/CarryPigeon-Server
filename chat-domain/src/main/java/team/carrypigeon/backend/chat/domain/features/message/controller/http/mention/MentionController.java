package team.carrypigeon.backend.chat.domain.features.message.controller.http.mention;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import team.carrypigeon.backend.chat.domain.config.http.Result;
import team.carrypigeon.backend.chat.domain.config.http.security.CpPrincipal;
import team.carrypigeon.backend.chat.domain.features.message.controller.http.mention.dto.MentionItemResponse;
import team.carrypigeon.backend.chat.domain.features.message.controller.http.mention.dto.MentionListResponse;
import team.carrypigeon.backend.chat.domain.features.message.controller.http.mention.dto.UpdateMentionReadStateRequest;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.mention.MentionApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.mention.input.ListMentionsQuery;
import team.carrypigeon.backend.chat.domain.features.message.domain.capability.OpaqueCursorCodec;
import team.carrypigeon.backend.chat.domain.features.message.domain.problem.MessageProblem;
import team.carrypigeon.backend.infrastructure.basic.id.IdUtil;

/**
 * 提及收件箱 HTTP 入口。
 */
@RestController
@RequestMapping("/api/mentions")
@PreAuthorize("isAuthenticated()")
@Tag(name = "提及收件箱", description = "提及 inbox 列表能力。")
public class MentionController {

    private static final String MENTION_CURSOR_SCOPE = "mentions";

    private final MentionApi mentionDomainApi;

    /**
     * 创建提及收件箱 HTTP 入口。
     *
     * @param mentionDomainApi 提及领域 API
     */
    public MentionController(MentionApi mentionDomainApi) {
        this.mentionDomainApi = mentionDomainApi;
    }

    /**
     * 查询当前账号的提及收件箱。
     *
     * <p>请求 {@code GET /api/mentions}。无请求体。
     * <p>成功响应 HTTP 200，code 为 0，data 为 {@code MentionListResponse}。
     * <p>失败示例：游标无效 10013。
     *
     * @param cursor 分页游标
     * @param limit 查询数量
     * @param unreadOnly 是否仅查询未读提及
     * @param channelId 可选频道 ID
     */
    @GetMapping(produces = "application/json")
    @Operation(summary = "获取提及收件箱", description = "按用户返回提及列表。")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "返回提及列表", useReturnTypeSchema = true))
    public Result<MentionListResponse> listMentions(
        @RequestParam(required = false)
        String cursor,
        @RequestParam(defaultValue = "20")
        @Range(min = 1, max = 50, message = "limit must be between 1 and 50")
        int limit,
        @RequestParam(name = "unread_only", defaultValue = "false")
        boolean unreadOnly,
        @RequestParam(name = "cid", required = false)
        @Positive(message = "cid must be greater than 0")
        Long channelId,
        @AuthenticationPrincipal
        CpPrincipal principal
    ) {
        List<MentionItemResponse> queriedItems = mentionDomainApi.listMentions(new ListMentionsQuery(
                principal.accountId(),
                OpaqueCursorCodec.decode(MENTION_CURSOR_SCOPE, cursor, MessageProblem.CURSOR_INVALID),
                limit,
                unreadOnly,
                channelId
        )).stream()
                .map(mention -> new MentionItemResponse(
                        IdUtil.toString(mention.mentionId()),
                        IdUtil.toString(mention.channelId()),
                        IdUtil.toString(mention.messageId()),
                        IdUtil.toString(mention.fromAccountId()),
                        new MentionItemResponse.MentionTargetResponse(
                                mention.targetType(),
                                IdUtil.toString(mention.targetAccountId())
                        ),
                        mention.createdAt().toEpochMilli(),
                        mention.read()
                ))
                .toList();
        boolean hasMore = queriedItems.size() > limit;
        List<MentionItemResponse> items = hasMore ? queriedItems.subList(0, limit) : queriedItems;
        String nextCursor = hasMore
                ? OpaqueCursorCodec.encode(MENTION_CURSOR_SCOPE, Long.parseLong(items.getLast().mentionId()))
                : null;
        return Result.success(new MentionListResponse(items, nextCursor, hasMore));
    }

    /**
     * 将当前用户的一条提及标记为已读。
     *
     * <p>请求 {@code PUT /api/mentions/4001/read}。无请求体。
     * <p>成功响应 HTTP 200，code 为 0，data 为 {@code null}。
     * <p>失败示例：提及不存在 30002。
     *
     * @param mentionId 提及 ID
     */
    @PutMapping(value = "/{mentionId}/read", produces = "application/json")
    @Operation(summary = "标记单条提及已读", description = "将当前用户的一条提及标记为已读。")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "成功时 code 为 0 且 data 为 null", useReturnTypeSchema = true))
    public Result<Void> markMentionRead(
        @AuthenticationPrincipal
        CpPrincipal principal,
        @PathVariable
        @Positive(message = "mentionId must be greater than 0")
        Long mentionId
    ) {
        mentionDomainApi.markMentionRead(principal.accountId(), mentionId);
        return Result.success();
    }

    /**
     * TODO 待处理
     * 副作用：将匹配范围内的提及标记为已读。
     *
     * <p>请求 {@code PUT /api/mentions/read_state}。JSON 请求体示例 {@code {"before_mention_id":"4001","cid":"2001"}}。 可省略请求体。
     * <p>成功响应 HTTP 200，code 为 0，data 为 {@code null}。
     * <p>失败示例：字段校验失败 10001。
     *
     * @param body 批量已读条件
     */
    @PutMapping(value = "/read_state", produces = "application/json", consumes = {"application/json", "application/*+json"})
    @Operation(summary = "批量标记提及已读", description = "按条件批量标记当前用户提及为已读。")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "成功时 code 为 0 且 data 为 null", useReturnTypeSchema = true))
    public Result<Void> markMentionsRead(
        @Valid
        @RequestBody(required = false)
        UpdateMentionReadStateRequest body,
        @AuthenticationPrincipal
        CpPrincipal principal
    ) {
        mentionDomainApi.markMentionsRead(
                principal.accountId(),
                body == null ? null : body.beforeMentionId(),
                body == null ? null : body.cid()
        );
        return Result.success();
    }

}
