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
import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.ChannelMessageHistoryApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.input.GetChannelMessagesAfterQuery;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.input.GetChannelMessagesBeforeQuery;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.input.GetLatestChannelMessagesQuery;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.result.ChannelMessagePage;
import team.carrypigeon.backend.chat.domain.features.message.domain.problem.MessageProblem;
import team.carrypigeon.backend.infrastructure.basic.page.CursorPageResponse;

/**
 * 频道消息方向查询 HTTP 入口。
 * 职责：分别提供最新消息、更旧消息和更新消息三个游标查询端点。
 * 边界：不承载锚点上下文或搜索。
 */
@Validated
@RestController
@RequestMapping("/api/channels")
@PreAuthorize("isAuthenticated()")
@Tag(name = "频道消息", description = "频道消息方向查询能力。")
public class ChannelMessageHistoryController {

    private static final String BEFORE_CURSOR_SCOPE = "channel_messages_before";
    private static final String AFTER_CURSOR_SCOPE = "channel_messages_after";

    private final ChannelMessageHistoryApi channelMessageHistoryApi;

    public ChannelMessageHistoryController(ChannelMessageHistoryApi channelMessageHistoryApi) {
        this.channelMessageHistoryApi = channelMessageHistoryApi;
    }

    /**
     * 查询频道最新消息。
     *
     * @param channelId 频道 ID
     * @param limit 返回条数
     * @param cpPrincipal 当前认证用户
     * @return 最新消息游标页
     */
    @GetMapping("/{channelId}/messages")
    @Operation(summary = "读取最新消息", description = "返回频道最新一页消息；next_cursor 用于继续读取更旧消息。")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "返回 items + next_cursor + has_more 分页对象")
    })
    public CursorPageResponse<ChannelMessageResponse> getLatestMessages(
        @Parameter(description = "目标频道 ID", example = "2001")
        @PathVariable @Positive(message = "channelId must be greater than 0") long channelId,
        @Parameter(description = "返回条数，范围 1..50", example = "20")
        @RequestParam(defaultValue = "20")
        @Min(value = 1, message = "limit must be between 1 and 50")
        @Max(value = 50, message = "limit must be between 1 and 50") int limit,
        @AuthenticationPrincipal CpPrincipal cpPrincipal
    ) {
        ChannelMessagePage page = channelMessageHistoryApi.getLatestMessages(
            new GetLatestChannelMessagesQuery(cpPrincipal.accountId(), channelId, limit)
        );
        return toResponse(page, BEFORE_CURSOR_SCOPE);
    }

    /**
     * 查询频道内更旧消息。
     *
     * @param channelId 频道 ID
     * @param cursor 排他游标
     * @param limit 返回条数
     * @param cpPrincipal 当前认证用户
     * @return 更旧消息游标页
     */
    @GetMapping("/{channelId}/messages/before")
    @Operation(summary = "读取更旧消息", description = "以 cursor 为排他边界继续向更旧方向读取频道消息。")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "返回 items + next_cursor + has_more 分页对象")
    })
    public CursorPageResponse<ChannelMessageResponse> getMessagesBefore(
        @Parameter(description = "目标频道 ID", example = "2001")
        @PathVariable @Positive(message = "channelId must be greater than 0") long channelId,
        @Parameter(description = "上一页返回的排他游标", example = "Y2hhbm5lbF9tZXNzYWdlc19iZWZvcmU6NTAwMA")
        @RequestParam @NotBlank(message = "cursor must not be blank") String cursor,
        @Parameter(description = "返回条数，范围 1..50", example = "20")
        @RequestParam(defaultValue = "20")
        @Min(value = 1, message = "limit must be between 1 and 50")
        @Max(value = 50, message = "limit must be between 1 and 50") int limit,
        @AuthenticationPrincipal CpPrincipal cpPrincipal
    ) {
        ChannelMessagePage page = channelMessageHistoryApi.getMessagesBefore(
            new GetChannelMessagesBeforeQuery(
                cpPrincipal.accountId(),
                channelId,
                OpaqueCursorCodec.decode(BEFORE_CURSOR_SCOPE, cursor, MessageProblem.CURSOR_INVALID),
                limit
            )
        );
        return toResponse(page, BEFORE_CURSOR_SCOPE);
    }

    /**
     * 查询频道内更新消息。
     *
     * @param channelId 频道 ID
     * @param cursor 排他游标
     * @param limit 返回条数
     * @param cpPrincipal 当前认证用户
     * @return 更新消息游标页
     */
    @GetMapping("/{channelId}/messages/after")
    @Operation(summary = "读取更新消息", description = "以 cursor 为排他边界向更新方向读取频道消息，结果按消息 ID 升序返回。")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "返回 items + next_cursor + has_more 分页对象")
    })
    public CursorPageResponse<ChannelMessageResponse> getMessagesAfter(
        @Parameter(description = "目标频道 ID", example = "2001")
        @PathVariable @Positive(message = "channelId must be greater than 0") long channelId,
        @Parameter(description = "上一页返回的排他游标", example = "Y2hhbm5lbF9tZXNzYWdlc19hZnRlcjo1MDAw")
        @RequestParam @NotBlank(message = "cursor must not be blank") String cursor,
        @Parameter(description = "返回条数，范围 1..50", example = "20")
        @RequestParam(defaultValue = "20")
        @Min(value = 1, message = "limit must be between 1 and 50")
        @Max(value = 50, message = "limit must be between 1 and 50") int limit,
        @AuthenticationPrincipal CpPrincipal cpPrincipal
    ) {
        ChannelMessagePage page = channelMessageHistoryApi.getMessagesAfter(
            new GetChannelMessagesAfterQuery(
                cpPrincipal.accountId(),
                channelId,
                OpaqueCursorCodec.decode(AFTER_CURSOR_SCOPE, cursor, MessageProblem.CURSOR_INVALID),
                limit
            )
        );
        return toResponse(page, AFTER_CURSOR_SCOPE);
    }

    private CursorPageResponse<ChannelMessageResponse> toResponse(
            ChannelMessagePage page,
            String cursorScope
    ) {
        return CursorPageResponse.of(
            page.items().stream().map(ChannelMessageResponse::from).toList(),
            OpaqueCursorCodec.encode(cursorScope, page.nextCursor()),
            page.hasMore()
        );
    }
}
