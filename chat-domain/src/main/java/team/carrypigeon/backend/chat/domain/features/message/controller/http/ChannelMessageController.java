package team.carrypigeon.backend.chat.domain.features.message.controller.http;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import team.carrypigeon.backend.chat.domain.features.auth.controller.support.AuthRequestContext;
import team.carrypigeon.backend.chat.domain.features.auth.controller.support.AuthenticatedPrincipal;
import team.carrypigeon.backend.chat.domain.features.message.application.command.DeleteChannelMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.application.command.EditChannelMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.application.command.PinChannelMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.application.command.SendChannelMessageHttpCommand;
import team.carrypigeon.backend.chat.domain.features.message.application.command.UnpinChannelMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.application.dto.ChannelMessageHistoryResult;
import team.carrypigeon.backend.chat.domain.features.message.application.dto.ChannelPinResult;
import team.carrypigeon.backend.chat.domain.features.message.application.dto.ChannelMessageResult;
import team.carrypigeon.backend.chat.domain.features.message.application.dto.ChannelMessageSearchResult;
import team.carrypigeon.backend.chat.domain.features.message.application.dto.MessageAttachmentUploadResult;
import team.carrypigeon.backend.chat.domain.features.message.application.query.GetChannelMessageHistoryQuery;
import team.carrypigeon.backend.chat.domain.features.message.application.query.ListChannelPinsQuery;
import team.carrypigeon.backend.chat.domain.features.message.application.query.SearchChannelMessagesQuery;
import team.carrypigeon.backend.chat.domain.features.message.application.service.MessageApplicationService;
import team.carrypigeon.backend.chat.domain.features.message.controller.dto.ChannelPinItemResponse;
import team.carrypigeon.backend.chat.domain.features.message.controller.dto.ChannelPinListResponse;
import team.carrypigeon.backend.chat.domain.features.message.controller.dto.ChannelMessageV1Response;
import team.carrypigeon.backend.chat.domain.features.message.controller.dto.EditChannelMessageRequest;
import team.carrypigeon.backend.chat.domain.features.message.controller.dto.MessageAttachmentUploadResponse;
import team.carrypigeon.backend.chat.domain.features.message.controller.dto.PinChannelMessageRequest;
import team.carrypigeon.backend.chat.domain.features.message.controller.dto.SendChannelMessageRequest;
import team.carrypigeon.backend.chat.domain.features.message.controller.support.ChannelMessageV1ResponseMapper;
import team.carrypigeon.backend.chat.domain.features.user.application.service.UserProfileApplicationService;
import team.carrypigeon.backend.chat.domain.shared.controller.CursorPageResponse;
import team.carrypigeon.backend.chat.domain.shared.controller.OpaqueCursorCodec;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.id.Ids;
import team.carrypigeon.backend.infrastructure.basic.json.JsonProvider;

/**
 * 频道消息 HTTP 入口。
 * 职责：提供频道消息历史/搜索查询、发送与置顶的协议能力。
 * 边界：只承接协议层请求，不承载消息业务规则。
 */
@Validated
@RestController
@RequestMapping("/api/channels")
@Tag(name = "频道消息", description = "频道消息历史查询、搜索与发送能力。")
public class ChannelMessageController {

    private static final String HISTORY_CURSOR_SCOPE = "channel_messages";
    private static final String SEARCH_CURSOR_SCOPE = "channel_message_search";
    private static final String PIN_CURSOR_SCOPE = "channel_pins";

    private final MessageApplicationService messageApplicationService;
    private final AuthRequestContext authRequestContext;
    private final ChannelMessageV1ResponseMapper responseMapper;

    public ChannelMessageController(
            MessageApplicationService messageApplicationService,
            UserProfileApplicationService userProfileApplicationService,
            AuthRequestContext authRequestContext,
            JsonProvider jsonProvider
    ) {
        this.messageApplicationService = messageApplicationService;
        this.authRequestContext = authRequestContext;
        this.responseMapper = new ChannelMessageV1ResponseMapper(userProfileApplicationService, jsonProvider);
    }

    public ChannelMessageController(
            MessageApplicationService messageApplicationService,
            UserProfileApplicationService userProfileApplicationService,
            AuthRequestContext authRequestContext
    ) {
        this(messageApplicationService, userProfileApplicationService, authRequestContext, new JsonProvider(new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules()));
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
        AuthenticatedPrincipal principal = authRequestContext.requirePrincipal(request);
        ChannelMessageHistoryResult result = messageApplicationService.getChannelMessageHistory(
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
        AuthenticatedPrincipal principal = authRequestContext.requirePrincipal(request);
        String effectiveKeyword = q != null && !q.isBlank() ? q : keyword;
        if (effectiveKeyword == null || effectiveKeyword.isBlank()) {
            throw ProblemException.validationFailed("q must not be blank");
        }
        if (effectiveKeyword.trim().length() > 100) {
            throw ProblemException.validationFailed("validation_failed", "q length must be between 1 and 100");
        }
        ChannelMessageSearchResult result = messageApplicationService.searchChannelMessages(
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

    @PostMapping("/{channelId}/pins/{messageId}")
    @Operation(summary = "置顶频道消息", description = "置顶指定消息。")
    public ChannelPinItemResponse pinChannelMessage(
            @PathVariable long channelId,
            @PathVariable long messageId,
            @RequestBody(required = false) PinChannelMessageRequest requestBody,
            HttpServletRequest request
    ) {
        AuthenticatedPrincipal principal = authRequestContext.requirePrincipal(request);
        ChannelPinResult result = messageApplicationService.pinChannelMessage(
                new PinChannelMessageCommand(principal.accountId(), channelId, messageId, requestBody == null ? null : requestBody.note())
        );
        return toPinResponse(result);
    }

    @DeleteMapping("/{channelId}/pins/{messageId}")
    @Operation(summary = "取消置顶频道消息", description = "取消置顶指定消息。")
    public ResponseEntity<Void> unpinChannelMessage(
            @PathVariable long channelId,
            @PathVariable long messageId,
            HttpServletRequest request
    ) {
        AuthenticatedPrincipal principal = authRequestContext.requirePrincipal(request);
        messageApplicationService.unpinChannelMessage(new UnpinChannelMessageCommand(principal.accountId(), channelId, messageId));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{channelId}/pins")
    @Operation(summary = "获取频道置顶列表", description = "按频道返回置顶消息列表。")
    public ChannelPinListResponse listChannelPins(
            @PathVariable long channelId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit,
            HttpServletRequest request
    ) {
        AuthenticatedPrincipal principal = authRequestContext.requirePrincipal(request);
        var items = messageApplicationService.listChannelPins(new ListChannelPinsQuery(
                principal.accountId(),
                channelId,
                decodeCursor(PIN_CURSOR_SCOPE, cursor),
                limit
        ));
        boolean hasMore = items.size() > limit;
        var pageItems = hasMore ? items.subList(0, limit) : items;
        String nextCursor = hasMore ? encodeCursor(PIN_CURSOR_SCOPE, pageItems.get(pageItems.size() - 1).messageId()) : null;
        return new ChannelPinListResponse(pageItems.stream().map(this::toPinResponse).toList(), nextCursor, hasMore);
    }

    @PostMapping("/{channelId}/messages")
    @Operation(summary = "发送消息", description = "按 v1 语义发送频道消息；当前支持 Core:Text / Core:File / Core:Voice。")
    public ResponseEntity<ChannelMessageV1Response> sendChannelMessage(
            @PathVariable @Positive(message = "channelId must be greater than 0") long channelId,
            @Validated @RequestBody SendChannelMessageRequest requestBody,
            HttpServletRequest request
    ) {
        AuthenticatedPrincipal principal = authRequestContext.requirePrincipal(request);
        ChannelMessageResult result = messageApplicationService.sendChannelMessageHttp(
                new SendChannelMessageHttpCommand(
                        principal.accountId(),
                        channelId,
                        requestBody.domain(),
                        requestBody.domainVersion(),
                        requestBody.data(),
                        requestBody.replyToMid(),
                        toMentionCommands(requestBody.mentions()),
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
        AuthenticatedPrincipal principal = authRequestContext.requirePrincipal(request);
        try {
            MessageAttachmentUploadResult result = messageApplicationService.uploadMessageAttachment(
                    principal.accountId(),
                    channelId,
                    messageType,
                    file.getOriginalFilename() == null ? file.getName() : file.getOriginalFilename(),
                    file.getContentType(),
                    file.getSize(),
                    file.getInputStream()
            );
            return new MessageAttachmentUploadResponse(
                    100,
                    "success",
                    new MessageAttachmentUploadResponse.Data(
                            result.objectKey(),
                            result.shareKey(),
                            result.filename(),
                            result.mimeType(),
                            result.size()
                    )
            );
        } catch (IOException exception) {
            throw ProblemException.fail("attachment_upload_read_failed", "failed to read attachment upload content");
        }
    }

    private java.util.List<EditChannelMessageCommand.MentionTargetCommand> toMentionCommands(
            java.util.List<EditChannelMessageRequest.MentionTargetRequest> mentionRequests
    ) {
        if (mentionRequests == null || mentionRequests.isEmpty()) {
            return java.util.List.of();
        }
        java.util.List<EditChannelMessageCommand.MentionTargetCommand> commands = new java.util.ArrayList<>();
        for (EditChannelMessageRequest.MentionTargetRequest mentionRequest : mentionRequests) {
            if (mentionRequest == null || mentionRequest.uid() == null || mentionRequest.uid().isBlank()) {
                throw ProblemException.validationFailed("mentions uid must be decimal snowflake string");
            }
            commands.add(new EditChannelMessageCommand.MentionTargetCommand(
                    mentionRequest.type() == null || mentionRequest.type().isBlank() ? "user" : mentionRequest.type().trim(),
                    parseOptionalSnowflake(mentionRequest.uid(), "mentions uid", false)
            ));
        }
        return commands;
    }

    private ChannelPinItemResponse toPinResponse(ChannelPinResult result) {
        return new ChannelPinItemResponse(
                Ids.toString(result.channelId()),
                Ids.toString(result.messageId()),
                Ids.toString(result.pinnedByAccountId()),
                result.pinnedAt().toEpochMilli(),
                result.note()
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

    private Long decodeCursor(String scope, String cursor) {
        return OpaqueCursorCodec.decode(scope, cursor);
    }

    private String encodeCursor(String scope, Long value) {
        return OpaqueCursorCodec.encode(scope, value);
    }

}
