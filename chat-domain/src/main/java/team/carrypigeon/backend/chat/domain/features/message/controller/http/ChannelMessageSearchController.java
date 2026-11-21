package team.carrypigeon.backend.chat.domain.features.message.controller.http;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import team.carrypigeon.backend.chat.domain.config.http.Result;
import team.carrypigeon.backend.chat.domain.config.http.security.CpPrincipal;
import team.carrypigeon.backend.chat.domain.features.message.controller.http.dto.ChannelMessageResponse;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.ChannelMessageSearchApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.input.SearchChannelMessagesQuery;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.result.ChannelMessagePage;
import team.carrypigeon.backend.infrastructure.basic.page.OpaqueCursorCodec;
import team.carrypigeon.backend.chat.domain.features.message.domain.problem.MessageProblem;
import team.carrypigeon.backend.infrastructure.basic.page.CursorPageResponse;

/**
 * 频道消息搜索 HTTP 入口。
 * 职责：提供频道内按关键字和过滤条件的消息搜索。
 * 边界：不承载时间线方向或锚点上下文。
 */
@RestController
@RequestMapping("/api/channels")
@PreAuthorize("isAuthenticated()")
@Tag(name = "频道消息", description = "频道消息搜索能力。")
public class ChannelMessageSearchController {

    private static final String SEARCH_CURSOR_SCOPE = "channel_message_search_v2_";

    private final ChannelMessageSearchApi channelMessageSearchApi;

    public ChannelMessageSearchController(ChannelMessageSearchApi channelMessageSearchApi) {
        this.channelMessageSearchApi = channelMessageSearchApi;
    }

    /**
     * 在频道内按关键字搜索消息。
     *
     * <p>请求 {@code GET /api/channels/2001/messages/search?q=hello}。无请求体。
     * <p>成功响应 HTTP 200，code 为 0，data 为 {@code CursorPageResponse<ChannelMessageResponse>}。
     * <p>失败示例：游标无效 10013。
     *
     * @param channelId 频道 ID
     * @param query 搜索关键字
     * @param cursor 搜索游标
     * @param senderUid 发送者账号 ID
     * @param domain 消息 domain
     * @param limit 返回条数
     * @param cpPrincipal 当前认证用户
     */
    @GetMapping(value = "/{channelId}/messages/search", produces = "application/json")
    @Operation(summary = "搜索消息", description = "在指定频道内按 MySQL ngram 自然语言全文匹配搜索正文，按频道序号倒序分页，不按相关度排序。默认双字分词，单字及纯符号可能无结果；不搜索回复元数据。")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "返回 items + next_cursor + has_more 分页对象", useReturnTypeSchema = true))
    public Result<CursorPageResponse<ChannelMessageResponse>> searchChannelMessages(
        @Parameter(description = "目标频道 ID", example = "2001")
        @PathVariable
        @Positive(message = "channelId must be greater than 0")
        long channelId,
        @Parameter(description = "正文全文检索关键字，遵循 MySQL ngram 分词语义", example = "hello")
        @RequestParam(name = "q")
        @NotBlank(message = "q must not be blank")
        @Size(min = 1, max = 100, message = "q length must be between 1 and 100")
        String query,
        @Parameter(description = "搜索排他游标")
        @RequestParam(required = false)
        String cursor,
        @Parameter(description = "发送者账号 ID")
        @RequestParam(name = "sender_uid", required = false)
        @Positive(message = "sender_uid must be greater than 0")
        Long senderUid,
        @Parameter(description = "消息 domain 过滤值")
        @RequestParam(required = false)
        String domain,
        @Parameter(description = "返回条数，范围 1..50", example = "20")
        @RequestParam(defaultValue = "20")
        @Min(value = 1, message = "limit must be between 1 and 50")
        @Max(value = 50, message = "limit must be between 1 and 50")
        int limit,
        @AuthenticationPrincipal
        CpPrincipal cpPrincipal
    ) {
        ChannelMessagePage page = channelMessageSearchApi.searchMessages(
            new SearchChannelMessagesQuery(
                cpPrincipal.accountId(),
                channelId,
                query,
                OpaqueCursorCodec.decode(SEARCH_CURSOR_SCOPE + channelId, cursor, MessageProblem.CURSOR_INVALID),
                senderUid,
                domain == null || domain.isBlank() ? null : domain.trim(),
                limit
            )
        );
        return Result.success(CursorPageResponse.of(
            page.items().stream().map(ChannelMessageResponse::from).toList(),
            OpaqueCursorCodec.encode(SEARCH_CURSOR_SCOPE + channelId, page.nextCursor()),
            page.hasMore()
        ));
    }
}
