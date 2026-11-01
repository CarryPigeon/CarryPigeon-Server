package team.carrypigeon.backend.chat.domain.features.message.controller.http;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import team.carrypigeon.backend.chat.domain.config.http.OpaqueCursorCodec;
import team.carrypigeon.backend.chat.domain.config.http.security.CpPrincipal;
import team.carrypigeon.backend.chat.domain.features.message.controller.http.dto.ChannelMessageResponse;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.ChannelMessageSearchApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.input.SearchChannelMessagesQuery;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.result.ChannelMessagePage;
import team.carrypigeon.backend.chat.domain.features.message.domain.problem.MessageProblem;
import team.carrypigeon.backend.infrastructure.basic.exception.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.page.CursorPageResponse;

/**
 * 频道消息搜索 HTTP 入口。
 * 职责：提供频道内按关键字和过滤条件的消息搜索。
 * 边界：不承载时间线方向或锚点上下文。
 */
@Validated
@RestController
@RequestMapping("/api/channels")
@PreAuthorize("isAuthenticated()")
@Tag(name = "频道消息", description = "频道消息搜索能力。")
public class ChannelMessageSearchController {

    private static final int MAX_KEYWORD_LENGTH = 100;
    private static final String SEARCH_CURSOR_SCOPE = "channel_message_search";

    private final ChannelMessageSearchApi channelMessageSearchApi;

    public ChannelMessageSearchController(ChannelMessageSearchApi channelMessageSearchApi) {
        this.channelMessageSearchApi = channelMessageSearchApi;
    }

    /**
     * TODO 待迭代优化
     * 在频道内按关键字搜索消息。
     *
     * @param channelId 频道 ID
     * @param query 搜索关键字
     * @param cursor 搜索游标
     * @param senderUid 发送者账号 ID
     * @param domain 消息 domain
     * @param limit 返回条数
     * @param cpPrincipal 当前认证用户
     * @return 搜索结果游标页
     */
    @GetMapping("/{channelId}/messages/search")
    @Operation(summary = "搜索消息", description = "在指定频道内按关键字搜索消息，使用独立搜索游标分页。")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "返回 items + next_cursor + has_more 分页对象")
    })
    public CursorPageResponse<ChannelMessageResponse> searchChannelMessages(
        @Parameter(description = "目标频道 ID", example = "2001")
        @PathVariable @Positive(message = "channelId must be greater than 0") long channelId,
        @Parameter(description = "搜索关键字", example = "hello")
        @RequestParam(name = "q") @NotBlank(message = "q must not be blank") String query,
        @Parameter(description = "搜索排他游标")
        @RequestParam(required = false) String cursor,
        @Parameter(description = "发送者账号 ID")
        @RequestParam(name = "sender_uid", required = false)
        @Positive(message = "sender_uid must be greater than 0") Long senderUid,
        @Parameter(description = "消息 domain 过滤值")
        @RequestParam(required = false) String domain,
        @Parameter(description = "返回条数，范围 1..50", example = "20")
        @RequestParam(defaultValue = "20")
        @Min(value = 1, message = "limit must be between 1 and 50")
        @Max(value = 50, message = "limit must be between 1 and 50") int limit,
        @AuthenticationPrincipal CpPrincipal cpPrincipal
    ) {
        String normalizedKeyword = query.trim();
        if (normalizedKeyword.length() > MAX_KEYWORD_LENGTH) {
            throw ProblemException.of(MessageProblem.INVALID_INPUT, "q length must be between 1 and 100");
        }
        ChannelMessagePage page = channelMessageSearchApi.searchMessages(
            new SearchChannelMessagesQuery(
                cpPrincipal.accountId(),
                channelId,
                normalizedKeyword,
                OpaqueCursorCodec.decode(SEARCH_CURSOR_SCOPE, cursor, MessageProblem.CURSOR_INVALID),
                senderUid,
                domain == null || domain.isBlank() ? null : domain.trim(),
                limit
            )
        );
        return CursorPageResponse.of(
            page.items().stream().map(ChannelMessageResponse::from).toList(),
            OpaqueCursorCodec.encode(SEARCH_CURSOR_SCOPE, page.nextCursor()),
            page.hasMore()
        );
    }
}
