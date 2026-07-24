package team.carrypigeon.backend.chat.domain.features.message.controller.http;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import team.carrypigeon.backend.chat.domain.shared.controller.support.RequestAuthenticationContext;
import team.carrypigeon.backend.chat.domain.shared.domain.auth.AuthenticatedAccount;
import team.carrypigeon.backend.chat.domain.features.message.domain.command.RecallChannelMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.domain.command.SendChannelMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.domain.projection.ChannelMessageHistoryResult;
import team.carrypigeon.backend.chat.domain.features.message.domain.projection.ChannelMessageResult;
import team.carrypigeon.backend.chat.domain.features.message.domain.projection.ChannelMessageSearchResult;
import team.carrypigeon.backend.chat.domain.features.message.domain.projection.MessageAttachmentUploadResult;
import team.carrypigeon.backend.chat.domain.features.message.domain.query.GetChannelMessageHistoryQuery;
import team.carrypigeon.backend.chat.domain.features.message.domain.query.SearchChannelMessagesQuery;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.ChannelMessageAttachmentApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.ChannelMessageLifecycleApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.ChannelMessagePublishingApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.ChannelMessageTimelineApi;
import team.carrypigeon.backend.chat.domain.features.message.controller.dto.ChannelMessageV1Response;
import team.carrypigeon.backend.chat.domain.features.message.controller.dto.MessageAttachmentUploadResponse;
import team.carrypigeon.backend.chat.domain.features.message.controller.dto.SendChannelMessageRequest;
import team.carrypigeon.backend.chat.domain.features.message.controller.support.ChannelMessageV1ResponseMapper;
import team.carrypigeon.backend.chat.domain.shared.controller.CursorPageResponse;
import team.carrypigeon.backend.chat.domain.shared.controller.OpaqueCursorCodec;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;

/**
 * 频道消息 HTTP 入口。
 * 职责：提供频道消息历史/搜索查询、发送、撤回与附件上传协议能力。
 * 边界：只承接协议层请求，不承载消息业务规则。
 */
@Validated
@RestController
@RequestMapping("/api/channels")
@Tag(name = "频道消息", description = "频道消息历史查询、搜索与发送能力。")
public class ChannelMessageController {

    private static final String HISTORY_CURSOR_SCOPE = "channel_messages";
    private static final String SEARCH_CURSOR_SCOPE = "channel_message_search";
    private final ChannelMessagePublishingApi channelMessagePublishingApi;
    private final ChannelMessageTimelineApi channelMessageTimelineApi;
    private final ChannelMessageAttachmentApi channelMessageAttachmentDomainApi;
    private final ChannelMessageLifecycleApi channelMessageLifecycleApi;
    private final RequestAuthenticationContext authRequestContext;
    private final ChannelMessageV1ResponseMapper responseMapper;

    public ChannelMessageController(
            ChannelMessagePublishingApi channelMessagePublishingApi,
            ChannelMessageTimelineApi channelMessageTimelineApi,
            ChannelMessageAttachmentApi channelMessageAttachmentDomainApi,
            ChannelMessageLifecycleApi channelMessageLifecycleApi,
            RequestAuthenticationContext authRequestContext,
            ChannelMessageV1ResponseMapper responseMapper
    ) {
        this.channelMessagePublishingApi = channelMessagePublishingApi;
        this.channelMessageTimelineApi = channelMessageTimelineApi;
        this.channelMessageAttachmentDomainApi = channelMessageAttachmentDomainApi;
        this.channelMessageLifecycleApi = channelMessageLifecycleApi;
        this.authRequestContext = authRequestContext;
        this.responseMapper = responseMapper;
    }

    /**
     * 查询频道历史消息。
     *
     * @param channelId 频道 ID
     * @param cursor 游标消息 ID
     * @param limit 查询条数
     * @param request 当前 HTTP 请求
     * @return 统一响应包装的历史消息结果
     */
    @GetMapping("/{channelId}/messages")
    @Operation(summary = "读取历史消息", description = "按游标和条数读取指定频道的历史消息。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "返回 `items + next_cursor + has_more` 分页对象；失败时返回标准 HTTP 错误响应")
    })
    public CursorPageResponse<ChannelMessageV1Response> getChannelMessages(
            @Parameter(description = "目标频道 ID", example = "2001")
            @PathVariable @Positive(message = "channelId must be greater than 0") long channelId,
            @Parameter(description = "历史消息排他游标；为空表示从最新消息开始", example = "Y2hhbm5lbF9tZXNzYWdlczo1MDAw")
            @RequestParam(required = false) String cursor,
            @RequestParam(name = "around_mid", required = false) String aroundMid,
            @RequestParam(required = false) Integer before,
            @RequestParam(required = false) Integer after,
            @Parameter(description = "返回条数，范围 1..50", example = "20")
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "limit must be between 1 and 50")
            @Max(value = 50, message = "limit must be between 1 and 50") int limit,
            HttpServletRequest request
    ) {
        AuthenticatedAccount principal = authRequestContext.requirePrincipal(request);
        ChannelMessageHistoryResult result = channelMessageTimelineApi.getChannelMessageHistory(
                new GetChannelMessageHistoryQuery(
                        principal.accountId(),
                        channelId,
                        decodeCursor(HISTORY_CURSOR_SCOPE, cursor),
                        parseOptionalSnowflake(aroundMid, "around_mid", false),
                        before,
                        after,
                        limit
                )
        );
        return CursorPageResponse.of(
                result.messages().stream().map(responseMapper::toResponse).toList(),
                encodeCursor(HISTORY_CURSOR_SCOPE, result.nextCursor())
        );
    }

    /**
     * 在频道内按关键字搜索消息。
     *
     * @param channelId 频道 ID
     * @param keyword 搜索关键字
     * @param limit 返回条数
     * @param request 当前 HTTP 请求
     * @return 统一响应包装的搜索结果
     */
    @GetMapping("/{channelId}/messages/search")
    @Operation(summary = "按关键字搜索消息", description = "在指定频道内按关键字搜索消息。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "返回 `items + next_cursor + has_more` 分页对象；失败时返回标准 HTTP 错误响应")
    })
    public CursorPageResponse<ChannelMessageV1Response> searchChannelMessages(
            @Parameter(description = "目标频道 ID", example = "2001")
            @PathVariable @Positive(message = "channelId must be greater than 0") long channelId,
            @Parameter(description = "搜索关键字", example = "hello")
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(required = false) String cursor,
            @RequestParam(name = "sender_uid", required = false) String senderUid,
            @RequestParam(required = false) String domain,
            @RequestParam(name = "before_mid", required = false) String beforeMid,
            @RequestParam(name = "after_mid", required = false) String afterMid,
            @Parameter(description = "返回条数，范围 1..50", example = "20")
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "limit must be between 1 and 50")
            @Max(value = 50, message = "limit must be between 1 and 50") int limit,
            HttpServletRequest request
    ) {
        AuthenticatedAccount principal = authRequestContext.requirePrincipal(request);
        String effectiveKeyword = q != null && !q.isBlank() ? q : keyword;
        if (effectiveKeyword == null || effectiveKeyword.isBlank()) {
            throw ProblemException.validationFailed("q must not be blank");
        }
        if (effectiveKeyword.trim().length() > 100) {
            throw ProblemException.validationFailed("validation_failed", "q length must be between 1 and 100");
        }
        ChannelMessageSearchResult result = channelMessageTimelineApi.searchChannelMessages(
                new SearchChannelMessagesQuery(
                        principal.accountId(),
                        channelId,
                        effectiveKeyword,
                        decodeCursor(SEARCH_CURSOR_SCOPE, cursor),
                        parseOptionalSnowflake(senderUid, "sender_uid", false),
                        domain,
                        parseOptionalSnowflake(beforeMid, "before_mid", false),
                        parseOptionalSnowflake(afterMid, "after_mid", false),
                        limit
                )
        );
        boolean hasMore = result.messages().size() > limit;
        java.util.List<ChannelMessageResult> pageItems = hasMore ? result.messages().subList(0, limit) : result.messages();
        String nextCursor = hasMore ? encodeCursor(SEARCH_CURSOR_SCOPE, pageItems.get(pageItems.size() - 1).messageId()) : null;
        return CursorPageResponse.of(pageItems.stream().map(responseMapper::toResponse).toList(), nextCursor, hasMore);
    }

    public CursorPageResponse<ChannelMessageV1Response> searchChannelMessages(
            long channelId,
            String keyword,
            int limit,
            HttpServletRequest request
    ) {
        return searchChannelMessages(channelId, keyword, null, null, null, null, null, null, limit, request);
    }

    @PostMapping("/{channelId}/messages")
    @Operation(
            summary = "发送消息",
            description = "按 canonical envelope 发送频道消息；domain_version 与 data 由当前运行时注册的 domain 插件校验。"
    )
    public ResponseEntity<ChannelMessageV1Response> sendChannelMessage(
            @PathVariable @Positive(message = "channelId must be greater than 0") long channelId,
            @Valid @NotNull(message = "request body must not be null") @RequestBody SendChannelMessageRequest requestBody,
            HttpServletRequest request
    ) {
        AuthenticatedAccount principal = authRequestContext.requirePrincipal(request);
        ChannelMessageResult result = channelMessagePublishingApi.sendChannelMessage(
                new SendChannelMessageCommand(
                        principal.accountId(),
                        channelId,
                        requestBody.domain(),
                        requestBody.domainVersion(),
                        requestBody.data(),
                        parseMentionIds(requestBody.mentions()),
                        requestBody.clientMessageId()
                )
        );
        return ResponseEntity.status(201).body(responseMapper.toResponse(result));
    }

    @PostMapping(path = "/{channelId}/messages/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传消息附件", description = "上传文件或语音附件并返回后续发送消息可用的稳定引用。")
    public MessageAttachmentUploadResponse uploadMessageAttachment(
            @PathVariable @Positive(message = "channelId must be greater than 0") long channelId,
            @RequestParam(name = "message_type", required = false) String messageType,
            @RequestPart("file") MultipartFile file,
            HttpServletRequest request
    ) {
        AuthenticatedAccount principal = authRequestContext.requirePrincipal(request);
        try {
            MessageAttachmentUploadResult result = channelMessageAttachmentDomainApi.uploadMessageAttachment(
                    principal.accountId(),
                    channelId,
                    messageType,
                    file.getOriginalFilename() == null ? file.getName() : file.getOriginalFilename(),
                    file.getContentType(),
                    file.getSize(),
                    file.getInputStream()
            );
            return new MessageAttachmentUploadResponse(
                    result.objectKey(),
                    result.shareKey(),
                    result.filename(),
                    result.mimeType(),
                    result.size()
            );
        } catch (IOException exception) {
            throw ProblemException.fail("attachment_upload_read_failed", "failed to read attachment upload content");
        }
    }

    /**
     * 撤回指定频道消息。
     * 输入：频道 ID 与消息 ID。
     * 输出：撤回后的消息 v1 响应。
     *
     * @param channelId 频道 ID
     * @param messageId 消息 ID
     * @param request 当前 HTTP 请求
     * @return 撤回后的消息响应
     */
    @PostMapping("/{channelId}/messages/{messageId}/recall")
    @Operation(summary = "撤回消息", description = "按频道与消息 ID 撤回指定消息。")
    public ChannelMessageV1Response recallChannelMessage(
            @PathVariable @Positive(message = "channelId must be greater than 0") long channelId,
            @PathVariable @Positive(message = "messageId must be greater than 0") long messageId,
            HttpServletRequest request
    ) {
        AuthenticatedAccount principal = authRequestContext.requirePrincipal(request);
        ChannelMessageResult result = channelMessageLifecycleApi.recallChannelMessage(
                new RecallChannelMessageCommand(principal.accountId(), channelId, messageId)
        );
        return responseMapper.toResponse(result);
    }

    private java.util.List<Long> parseMentionIds(java.util.List<String> mentionIds) {
        if (mentionIds == null || mentionIds.isEmpty()) {
            return java.util.List.of();
        }
        java.util.List<Long> parsedIds = new java.util.ArrayList<>();
        for (String mentionId : mentionIds) {
            Long parsedId = parseOptionalSnowflake(mentionId, "mentions", false);
            if (parsedId == null || parsedId <= 0L) {
                throw ProblemException.validationFailed("mentions must contain positive decimal snowflake strings");
            }
            parsedIds.add(parsedId);
        }
        return parsedIds;
    }

    /**
     * 解析 HTTP 查询中的可选雪花 ID。
     * 失败语义：cursor 字段固定返回 `cursor_invalid`，其它字段返回字段级十进制雪花 ID 错误。
     *
     * @param rawValue 原始查询参数
     * @param fieldName 字段显示名
     * @param cursorField 是否为 cursor 字段
     * @return 解析后的雪花 ID，缺失时为 null
     */
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

    private Long decodeCursor(String scope, String cursor) {
        return OpaqueCursorCodec.decode(scope, cursor);
    }

    private String encodeCursor(String scope, Long value) {
        return OpaqueCursorCodec.encode(scope, value);
    }

}
