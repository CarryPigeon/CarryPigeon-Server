package team.carrypigeon.backend.chat.domain.features.message.controller.http;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import team.carrypigeon.backend.chat.domain.features.auth.controller.support.AuthRequestContext;
import team.carrypigeon.backend.chat.domain.features.auth.controller.support.AuthenticatedPrincipal;
import team.carrypigeon.backend.chat.domain.features.message.application.dto.MentionResult;
import team.carrypigeon.backend.chat.domain.features.message.application.query.ListMentionsQuery;
import team.carrypigeon.backend.chat.domain.features.message.application.service.MentionApplicationService;
import team.carrypigeon.backend.chat.domain.features.message.controller.dto.MentionItemResponse;
import team.carrypigeon.backend.chat.domain.features.message.controller.dto.MentionListResponse;
import team.carrypigeon.backend.chat.domain.features.message.controller.dto.UpdateMentionReadStateRequest;
import team.carrypigeon.backend.chat.domain.shared.controller.OpaqueCursorCodec;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.id.Ids;

/**
 * 提及收件箱 HTTP 入口。
 */
@RestController
@RequestMapping("/api/mentions")
@Tag(name = "提及收件箱", description = "提及 inbox 列表能力。")
public class MentionController {

    private static final String MENTION_CURSOR_SCOPE = "mentions";

    private final MentionApplicationService mentionApplicationService;
    private final AuthRequestContext authRequestContext;

    public MentionController(MentionApplicationService mentionApplicationService, AuthRequestContext authRequestContext) {
        this.mentionApplicationService = mentionApplicationService;
        this.authRequestContext = authRequestContext;
    }

    @GetMapping
    @Operation(summary = "获取提及收件箱", description = "按用户返回提及列表。")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "返回提及列表")})
    public MentionListResponse listMentions(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(name = "unread_only", defaultValue = "false") boolean unreadOnly,
            @RequestParam(name = "cid", required = false) String channelId,
            HttpServletRequest request
    ) {
        AuthenticatedPrincipal principal = authRequestContext.requirePrincipal(request);
        int normalizedLimit = normalizeLimit(limit);
        List<MentionItemResponse> queriedItems = mentionApplicationService.listMentions(new ListMentionsQuery(
                principal.accountId(),
                decodeCursor(cursor),
                normalizedLimit,
                unreadOnly,
                parseOptionalSnowflake(channelId, "cid", false)
        )).stream().map(this::toResponse).toList();
        boolean hasMore = queriedItems.size() > normalizedLimit;
        List<MentionItemResponse> items = hasMore ? queriedItems.subList(0, normalizedLimit) : queriedItems;
        String nextCursor = hasMore ? OpaqueCursorCodec.encode(MENTION_CURSOR_SCOPE, Long.parseLong(items.get(items.size() - 1).mentionId())) : null;
        return new MentionListResponse(items, nextCursor, hasMore);
    }

    /**
     * 将当前用户的一条提及标记为已读。
     *
     * @param mentionId 提及 ID
     * @param request 当前 HTTP 请求
     * @return HTTP 204
     */
    @PutMapping("/{mentionId}/read")
    @Operation(summary = "标记单条提及已读", description = "将当前用户的一条提及标记为已读。")
    @ApiResponses({@ApiResponse(responseCode = "204", description = "已标记为已读")})
    public ResponseEntity<Void> markMentionRead(@PathVariable String mentionId, HttpServletRequest request) {
        AuthenticatedPrincipal principal = authRequestContext.requirePrincipal(request);
        mentionApplicationService.markMentionRead(principal.accountId(), parseRequiredSnowflake(mentionId, "mention_id"));
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/read_state")
    @Operation(summary = "批量标记提及已读", description = "按条件批量标记当前用户提及为已读。")
    @ApiResponses({@ApiResponse(responseCode = "204", description = "已批量标记为已读")})
    public ResponseEntity<Void> markMentionsRead(
            @RequestBody(required = false) UpdateMentionReadStateRequest body,
            HttpServletRequest request
    ) {
        AuthenticatedPrincipal principal = authRequestContext.requirePrincipal(request);
        mentionApplicationService.markMentionsRead(
                principal.accountId(),
                body == null ? null : parseOptionalSnowflake(body.beforeMentionId(), "before_mention_id", false),
                body == null ? null : parseOptionalSnowflake(body.cid(), "cid", false)
        );
        return ResponseEntity.noContent().build();
    }

    private MentionItemResponse toResponse(MentionResult result) {
        return new MentionItemResponse(
                Ids.toString(result.mentionId()),
                Ids.toString(result.channelId()),
                Ids.toString(result.messageId()),
                Ids.toString(result.fromAccountId()),
                new MentionItemResponse.MentionTargetResponse(result.targetType(), Ids.toString(result.targetAccountId())),
                result.createdAt().toEpochMilli(),
                result.read()
        );
    }

    private Long parseOptionalSnowflake(String rawValue, String fieldName, boolean cursorField) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(rawValue.trim());
        } catch (NumberFormatException exception) {
            if (cursorField) {
                throw ProblemException.validationFailed("cursor_invalid", "cursor is invalid");
            }
            throw ProblemException.validationFailed(fieldName + " must be decimal snowflake string");
        }
    }

    private Long decodeCursor(String cursor) {
        return OpaqueCursorCodec.decode(MENTION_CURSOR_SCOPE, cursor);
    }

    private int normalizeLimit(int limit) {
        if (limit <= 0 || limit > 50) {
            throw ProblemException.validationFailed("validation_failed", "limit must be between 1 and 50");
        }
        return limit;
    }

    private long parseRequiredSnowflake(String rawValue, String fieldName) {
        Long parsed = parseOptionalSnowflake(rawValue, fieldName, false);
        if (parsed == null) {
            throw ProblemException.validationFailed(fieldName + " must be decimal snowflake string");
        }
        return parsed;
    }
}
