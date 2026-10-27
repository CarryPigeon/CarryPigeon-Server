package team.carrypigeon.backend.chat.domain.features.message.controller.http;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import team.carrypigeon.backend.chat.domain.config.http.security.CpPrincipal;
import team.carrypigeon.backend.chat.domain.features.message.controller.http.dto.ChannelMessageResponse;
import team.carrypigeon.backend.chat.domain.features.message.controller.http.dto.SendChannelMessageRequest;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.ChannelMessageApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.ChannelMessageTimelineApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.input.GetChannelMessageHistoryQuery;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.input.RecallChannelMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.input.SearchChannelMessagesQuery;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.input.SendChannelMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.result.ChannelMessageHistoryResult;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.result.ChannelMessageResult;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.result.ChannelMessageSearchResult;
import team.carrypigeon.backend.chat.domain.features.message.domain.problem.MessageProblem;
import team.carrypigeon.backend.chat.domain.shared.controller.CursorPageResponse;
import team.carrypigeon.backend.chat.domain.shared.controller.OpaqueCursorCodec;
import team.carrypigeon.backend.infrastructure.basic.exception.ProblemException;

import java.util.List;

/**
 * 频道消息 HTTP 入口。
 * 职责：提供频道消息历史/搜索查询、发送与撤回协议能力。
 * 边界：只承接协议层请求与请求数据校验，不承载频道权限、domain 支持和消息状态规则。
 */
@Validated
@RestController
@RequestMapping("/api/channels")
@PreAuthorize("isAuthenticated()")
@Tag(name = "频道消息", description = "频道消息历史查询、搜索、发送与撤回能力。")
public class ChannelMessageController {

    private static final String HISTORY_CURSOR_SCOPE = "channel_messages";
    private static final String SEARCH_CURSOR_SCOPE = "channel_message_search";
    private static final int DEFAULT_AROUND_WINDOW = 25;
    private static final int MAX_KEYWORD_LENGTH = 100;

    private final ChannelMessageApi channelMessageApi;
    private final ChannelMessageTimelineApi channelMessageTimelineApi;

    public ChannelMessageController(
            ChannelMessageApi channelMessageApi,
            ChannelMessageTimelineApi channelMessageTimelineApi
    ) {
        this.channelMessageApi = channelMessageApi;
        this.channelMessageTimelineApi = channelMessageTimelineApi;
    }

    /**
     * TODO 待审核
     * 查询频道历史消息。
     * 两种模式：cursor 模式从游标向前查询；around 模式查询目标消息前后的消息。
     * 约束：cursor 与 around_mid 不能同时使用；before/after 为空时使用默认窗口。
     *
     * @param channelId 频道 ID
     * @param cursor 游标
     * @param aroundMid 锚点消息 ID
     * @param before 锚点之前的数量
     * @param after 锚点之后的数量
     * @param limit 查询条数
     * @param cpPrincipal 当前认证用户
     * @return 游标分页的历史消息结果
     */
    @GetMapping("/{channelId}/messages")
    @Operation(summary = "读取历史消息", description = "按游标或锚点消息读取指定频道的历史消息。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "返回 items + next_cursor + has_more 分页对象；失败时返回标准 HTTP 错误响应")
    })
    public CursorPageResponse<ChannelMessageResponse> getChannelMessages(
            @Parameter(description = "目标频道 ID", example = "2001")
            @PathVariable @Positive(message = "channelId must be greater than 0") long channelId,
            @Parameter(description = "历史消息排他游标；为空表示从最新消息开始", example = "Y2hhbm5lbF9tZXNzYWdlczo1MDAw")
            @RequestParam(required = false) String cursor,
            @RequestParam(name = "around_mid", required = false)
            @Positive(message = "around_mid must be greater than 0") Long aroundMid,
            @RequestParam(required = false)
            @Min(value = 0, message = "before must be between 0 and 50")
            @Max(value = 50, message = "before must be between 0 and 50") Integer before,
            @RequestParam(required = false)
            @Min(value = 0, message = "after must be between 0 and 50")
            @Max(value = 50, message = "after must be between 0 and 50") Integer after,
            @Parameter(description = "返回条数，范围 1..50", example = "20")
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "limit must be between 1 and 50")
            @Max(value = 50, message = "limit must be between 1 and 50") int limit,
            @AuthenticationPrincipal CpPrincipal cpPrincipal
    ) {
        Long cursorMessageId = OpaqueCursorCodec.decode(
                HISTORY_CURSOR_SCOPE,
                cursor,
                MessageProblem.CURSOR_INVALID
        );
        if (cursorMessageId != null && aroundMid != null) {
            throw ProblemException.of(MessageProblem.INVALID_INPUT, "cursor and around_mid cannot be used together");
        }
        ChannelMessageHistoryResult result = channelMessageTimelineApi.getChannelMessageHistory(
                new GetChannelMessageHistoryQuery(
                        cpPrincipal.accountId(),
                        channelId,
                        cursorMessageId,
                        aroundMid,
                        before == null ? DEFAULT_AROUND_WINDOW : before,
                        after == null ? DEFAULT_AROUND_WINDOW : after,
                        limit
                )
        );
        return CursorPageResponse.of(
                result.messages().stream().map(ChannelMessageResponse::from).toList(),
                OpaqueCursorCodec.encode(HISTORY_CURSOR_SCOPE, result.nextCursor())
        );
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

    /**
     * 发送消息。
     *
     * @param channelId 目标频道 ID
     * @param requestBody 消息体
     * @param cpPrincipal 当前认证用户
     * @return 创建后的消息响应
     */
    @PostMapping("/{channelId}/messages")
    @Operation(
            summary = "发送消息",
            description = "按 canonical envelope 发送频道消息；domain 格式为主分类:子分类:版本号，data 由对应消息处理器校验。"
    )
    @ApiResponses({@ApiResponse(responseCode = "201", description = "消息发送成功")})
    public ResponseEntity<ChannelMessageResponse> sendChannelMessage(
            @PathVariable @Positive(message = "channelId must be greater than 0") long channelId,
            @Valid @NotNull(message = "request body must not be null") @RequestBody SendChannelMessageRequest requestBody,
            @AuthenticationPrincipal CpPrincipal cpPrincipal
    ) {
        ChannelMessageResult result = channelMessageApi.sendChannelMessage(
                new SendChannelMessageCommand(
                        cpPrincipal.accountId(),
                        channelId,
                        requestBody.domain().trim(),
                        requestBody.data(),
                        requestBody.mentions()
                )
        );
        return ResponseEntity.status(201).body(ChannelMessageResponse.from(result));
    }

    /**
     * 撤回指定频道消息。
     * 输出：成功时返回 204，无响应体。
     *
     * @param channelId 频道 ID
     * @param messageId 消息 ID
     * @param cpPrincipal 当前认证用户
     * @return 无内容响应
     */
    @PostMapping("/{channelId}/messages/{messageId}/recall")
    @Operation(summary = "撤回消息", description = "按频道与消息 ID 撤回指定消息，成功返回 204。")
    @ApiResponses({@ApiResponse(responseCode = "204", description = "消息撤回成功，响应体为空")})
    public ResponseEntity<Void> recallChannelMessage(
            @PathVariable @Positive(message = "channelId must be greater than 0") long channelId,
            @PathVariable @Positive(message = "messageId must be greater than 0") long messageId,
            @AuthenticationPrincipal CpPrincipal cpPrincipal
    ) {
        channelMessageApi.recallChannelMessage(
                new RecallChannelMessageCommand(cpPrincipal.accountId(), channelId, messageId)
        );
        return ResponseEntity.noContent().build();
    }
}
