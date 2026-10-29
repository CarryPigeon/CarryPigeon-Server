package team.carrypigeon.backend.chat.domain.features.message.controller.http.mention;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import team.carrypigeon.backend.chat.domain.config.http.security.CpPrincipal;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.mention.input.ListMentionsQuery;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.mention.MentionApi;
import team.carrypigeon.backend.chat.domain.features.message.controller.http.mention.dto.MentionItemResponse;
import team.carrypigeon.backend.chat.domain.features.message.controller.http.mention.dto.MentionListResponse;
import team.carrypigeon.backend.chat.domain.features.message.controller.http.mention.dto.UpdateMentionReadStateRequest;
import team.carrypigeon.backend.chat.domain.config.http.OpaqueCursorCodec;
import team.carrypigeon.backend.chat.domain.features.message.domain.problem.MessageProblem;
import team.carrypigeon.backend.infrastructure.basic.id.IdUtil;

/**
 * 提及收件箱 HTTP 入口。
 */
@Validated
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
     * TODO 待审核
     * 查询当前账号的提及收件箱。
     * 输入：游标、数量、未读筛选和可选频道筛选。
     * 输出：提及列表分页响应。
     *
     * @param cursor 分页游标
     * @param limit 查询数量
     * @param unreadOnly 是否仅查询未读提及
     * @param channelId 可选频道 ID
     * @return 提及列表分页响应
     */
    @GetMapping
    @Operation(summary = "获取提及收件箱", description = "按用户返回提及列表。")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "返回提及列表")})
    public MentionListResponse listMentions(
        @RequestParam(required = false) String cursor,
        @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "limit must be between 1 and 50")
            @Max(value = 50, message = "limit must be between 1 and 50") int limit,
        @RequestParam(name = "unread_only", defaultValue = "false") boolean unreadOnly,
        @RequestParam(name = "cid", required = false)
            @Positive(message = "cid must be greater than 0") Long channelId,
        @AuthenticationPrincipal CpPrincipal principal
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
        return new MentionListResponse(items, nextCursor, hasMore);
    }

    /**
     * 将当前用户的一条提及标记为已读。
     *
     * @param mentionId 提及 ID
     * @return HTTP 204
     */
    @PutMapping("/{mentionId}/read")
    @Operation(summary = "标记单条提及已读", description = "将当前用户的一条提及标记为已读。")
    @ApiResponses({@ApiResponse(responseCode = "204", description = "已标记为已读")})
    public ResponseEntity<Void> markMentionRead(
        @AuthenticationPrincipal CpPrincipal principal,
        @PathVariable @Positive(message = "mentionId must be greater than 0") Long mentionId
    ) {
        mentionDomainApi.markMentionRead(principal.accountId(), mentionId);
        return ResponseEntity.noContent().build();
    }

    /**
     * TODO 待处理
     * 批量标记当前账号提及为已读。
     * 输入：可选 mention 上界和可选频道 ID。
     * 副作用：将匹配范围内的提及标记为已读。
     *
     * @param body 批量已读条件
     * @return HTTP 204
     */
    @PutMapping("/read_state")
    @Operation(summary = "批量标记提及已读", description = "按条件批量标记当前用户提及为已读。")
    @ApiResponses({@ApiResponse(responseCode = "204", description = "已批量标记为已读")})
    public ResponseEntity<Void> markMentionsRead(
        @Valid @RequestBody(required = false) UpdateMentionReadStateRequest body,
        @AuthenticationPrincipal CpPrincipal principal
    ) {
        mentionDomainApi.markMentionsRead(
                principal.accountId(),
                body == null ? null : body.beforeMentionId(),
                body == null ? null : body.cid()
        );
        return ResponseEntity.noContent().build();
    }

}
