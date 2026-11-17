package team.carrypigeon.backend.chat.domain.features.message.controller.http;

import org.hibernate.validator.constraints.Range;
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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import team.carrypigeon.backend.chat.domain.features.message.domain.capability.OpaqueCursorCodec;
import team.carrypigeon.backend.chat.domain.config.http.security.CpPrincipal;
import team.carrypigeon.backend.chat.domain.features.message.controller.http.dto.ChannelMessageResponse;
import team.carrypigeon.backend.chat.domain.features.message.controller.http.dto.ChannelMessageHistoryResponse;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.ChannelMessageHistoryApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.input.GetChannelMessagesAfterQuery;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.input.GetChannelMessagesBeforeQuery;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.input.GetLatestChannelMessagesQuery;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.result.ChannelMessagePage;
import team.carrypigeon.backend.chat.domain.features.message.domain.problem.MessageProblem;

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

    // 版本与频道均进入作用域，旧消息 ID 游标及其它频道的序号不能被静默接受。
    private static final String BEFORE_CURSOR_SCOPE = "channel_messages_before_v2_";
    private static final String AFTER_CURSOR_SCOPE = "channel_messages_after_v2_";

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
    @Operation(summary = "读取最新消息", description = "按频道序号降序返回最新消息；next_cursor 读取更旧消息，sync_cursor 用于 after 增量补拉。旧版游标须重新获取。")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "返回 items + next_cursor + has_more + sync_cursor 分页对象")
    })
    public ChannelMessageHistoryResponse getLatestMessages(
        @Parameter(description = "目标频道 ID", example = "2001")
        @PathVariable
        @Positive(message = "channelId must be greater than 0")
        long channelId,
        @Parameter(description = "返回条数，范围 1..50", example = "20")
        @RequestParam(defaultValue = "20")
        @Range(min = 1,max = 50,message = "limit must be between 1 and 50")
        int limit,
        @AuthenticationPrincipal CpPrincipal cpPrincipal
    ) {
        ChannelMessagePage page = channelMessageHistoryApi.getLatestMessages(
            new GetLatestChannelMessagesQuery(cpPrincipal.accountId(), channelId, limit)
        );
        Long syncSequence = page.items().isEmpty() ? null : page.items().getFirst().sequence();
        return toResponse(page, channelId, BEFORE_CURSOR_SCOPE, syncSequence);
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
        @ApiResponse(responseCode = "200", description = "返回 items + next_cursor + has_more + sync_cursor 分页对象")
    })
    public ChannelMessageHistoryResponse getMessagesBefore(
        @Parameter(description = "目标频道 ID", example = "2001")
        @PathVariable
        @Positive(message = "channelId must be greater than 0")
        long channelId,
        @Parameter(description = "最新页或 before 页返回的 v2 next_cursor，与频道绑定")
        @RequestParam
        @NotBlank(message = "cursor must not be blank")
        String cursor,
        @Parameter(description = "返回条数，范围 1..50", example = "20")
        @RequestParam(defaultValue = "20")
        @Range(min = 1,max = 50,message = "limit must be between 1 and 50")
        int limit,
        @AuthenticationPrincipal CpPrincipal cpPrincipal
    ) {
        ChannelMessagePage page = channelMessageHistoryApi.getMessagesBefore(
            new GetChannelMessagesBeforeQuery(
                cpPrincipal.accountId(),
                channelId,
                OpaqueCursorCodec.decode(BEFORE_CURSOR_SCOPE + channelId, cursor, MessageProblem.CURSOR_INVALID),
                limit
            )
        );
        return toResponse(page, channelId, BEFORE_CURSOR_SCOPE, null);
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
    @Operation(summary = "读取更新消息", description = "使用 sync_cursor 按频道序号升序补拉；省略 cursor 从头开始。has_more=false 时仍保留 sync_cursor 供轮询。推送可能乱序，不能以最大推送序号跳过补拉。")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "返回 items + next_cursor + has_more + sync_cursor 分页对象")
    })
    public ChannelMessageHistoryResponse getMessagesAfter(
        @Parameter(description = "目标频道 ID", example = "2001")
        @PathVariable
        @Positive(message = "channelId must be greater than 0")
        long channelId,
        @Parameter(description = "latest 或 after 页返回的 v2 sync_cursor；省略时从头开始")
        @RequestParam(required = false)
        String cursor,
        @Parameter(description = "返回条数，范围 1..50", example = "20")
        @RequestParam(defaultValue = "20")
        @Range(min = 1,max = 50,message = "limit must be between 1 and 50")
        int limit,
        @AuthenticationPrincipal CpPrincipal cpPrincipal
    ) {
        Long sequence = OpaqueCursorCodec.decode(AFTER_CURSOR_SCOPE + channelId, cursor, MessageProblem.CURSOR_INVALID);
        ChannelMessagePage page = channelMessageHistoryApi.getMessagesAfter(
            new GetChannelMessagesAfterQuery(
                cpPrincipal.accountId(),
                channelId,
                sequence == null ? 0L : sequence,
                limit
            )
        );
        Long syncSequence = page.items().isEmpty() ? sequence : page.items().getLast().sequence();
        return toResponse(page, channelId, AFTER_CURSOR_SCOPE, syncSequence);
    }

    private ChannelMessageHistoryResponse toResponse(
            ChannelMessagePage page,
            long channelId,
            String cursorScope,
            Long syncSequence
    ) {
        return new ChannelMessageHistoryResponse(
            page.items().stream().map(ChannelMessageResponse::from).toList(),
            OpaqueCursorCodec.encode(cursorScope + channelId, page.nextCursor()),
            page.hasMore(),
            OpaqueCursorCodec.encode(AFTER_CURSOR_SCOPE + channelId, syncSequence)
        );
    }
}
