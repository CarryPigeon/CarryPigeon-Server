package team.carrypigeon.backend.chat.domain.features.message.controller.http;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import team.carrypigeon.backend.chat.domain.config.http.CursorPageResponse;
import team.carrypigeon.backend.chat.domain.config.http.OpaqueCursorCodec;
import team.carrypigeon.backend.chat.domain.config.http.security.CpPrincipal;
import team.carrypigeon.backend.chat.domain.features.message.controller.http.dto.ChannelMessageResponse;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.ChannelMessageTimelineApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.input.SearchChannelMessagesQuery;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.result.ChannelMessageResult;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.result.ChannelMessageSearchResult;
import team.carrypigeon.backend.chat.domain.features.message.domain.problem.MessageProblem;
import team.carrypigeon.backend.infrastructure.basic.exception.ProblemException;

import java.util.List;

/**
 * 频道消息查询的controller
 * 职责：提供频道消息的查询相关能力
 * */
@Validated
@RestController
@RequestMapping("/api/channels")
@PreAuthorize("isAuthenticated()")
@Tag(name = "频道消息", description = "频道消息历史查询、搜索、发送与撤回能力。")
public class ChannelMessageDiscoveryController {

    private static final int MAX_KEYWORD_LENGTH = 100;
    private static final String SEARCH_CURSOR_SCOPE = "channel_message_search";
    private final ChannelMessageTimelineApi channelMessageTimelineApi;

    public ChannelMessageDiscoveryController(ChannelMessageTimelineApi channelMessageTimelineApi) {
        this.channelMessageTimelineApi = channelMessageTimelineApi;
    }


    /**
     * TODO 待审核
     * 在频道内按关键字搜索消息。
     *
     * @param channelId 频道 ID
     * @param q 搜索关键字
     * @param keyword 兼容关键字
     * @param cursor 游标
     * @param senderUid 发送者账号 ID
     * @param domain 消息 domain 过滤值
     * @param beforeMid 前界消息 ID
     * @param afterMid 后界消息 ID
     * @param limit 返回条数
     * @param cpPrincipal 当前认证用户
     * @return 游标分页的搜索结果
     */
    @GetMapping("/{channelId}/messages/search")
    @Operation(summary = "按关键字搜索消息", description = "在指定频道内按关键字搜索消息。")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "返回 items + next_cursor + has_more 分页对象；失败时返回标准 HTTP 错误响应")
    })
    public CursorPageResponse<ChannelMessageResponse> searchChannelMessages(
        @Parameter(description = "目标频道 ID", example = "2001")
        @PathVariable @Positive(message = "channelId must be greater than 0") long channelId,
        @Parameter(description = "搜索关键字", example = "hello")
        @RequestParam(name = "q", required = false) String q,
        @RequestParam(name = "keyword", required = false) String keyword,
        @RequestParam(required = false) String cursor,
        @RequestParam(name = "sender_uid", required = false)
        @Positive(message = "sender_uid must be greater than 0") Long senderUid,
        @RequestParam(required = false) String domain,
        @RequestParam(name = "before_mid", required = false)
        @Positive(message = "before_mid must be greater than 0") Long beforeMid,
        @RequestParam(name = "after_mid", required = false)
        @Positive(message = "after_mid must be greater than 0") Long afterMid,
        @Parameter(description = "返回条数，范围 1..50", example = "20")
        @RequestParam(defaultValue = "20")
        @Min(value = 1, message = "limit must be between 1 and 50")
        @Max(value = 50, message = "limit must be between 1 and 50") int limit,
        @AuthenticationPrincipal CpPrincipal cpPrincipal
    ) {
        String effectiveKeyword = q != null && !q.isBlank() ? q : keyword;
        if (effectiveKeyword == null || effectiveKeyword.isBlank()) {
            throw ProblemException.of(MessageProblem.INVALID_INPUT, "q must not be blank");
        }
        String normalizedKeyword = effectiveKeyword.trim();
        if (normalizedKeyword.length() > MAX_KEYWORD_LENGTH) {
            throw ProblemException.of(MessageProblem.INVALID_INPUT, "q length must be between 1 and 100");
        }
        ChannelMessageSearchResult result = channelMessageTimelineApi.searchChannelMessages(
            new SearchChannelMessagesQuery(
                cpPrincipal.accountId(),
                channelId,
                normalizedKeyword,
                OpaqueCursorCodec.decode(SEARCH_CURSOR_SCOPE, cursor, MessageProblem.CURSOR_INVALID),
                senderUid,
                domain == null || domain.isBlank() ? null : domain.trim(),
                beforeMid,
                afterMid,
                limit
            )
        );
        boolean hasMore = result.messages().size() > limit;
        List<ChannelMessageResult> pageItems = hasMore ? result.messages().subList(0, limit) : result.messages();
        String nextCursor = hasMore
            ? OpaqueCursorCodec.encode(SEARCH_CURSOR_SCOPE, pageItems.getLast().messageId())
            : null;
        return CursorPageResponse.of(pageItems.stream().map(ChannelMessageResponse::from).toList(), nextCursor, hasMore);
    }

}
