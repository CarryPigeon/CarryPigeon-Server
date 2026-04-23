package team.carrypigeon.backend.chat.domain.features.message.controller.http;

import java.io.IOException;
import java.io.InputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import team.carrypigeon.backend.chat.domain.features.auth.controller.support.AuthRequestContext;
import team.carrypigeon.backend.chat.domain.features.auth.controller.support.AuthenticatedPrincipal;
import team.carrypigeon.backend.chat.domain.features.message.application.command.UploadChannelMessageAttachmentCommand;
import team.carrypigeon.backend.chat.domain.features.message.application.dto.ChannelMessageAttachmentUploadResult;
import team.carrypigeon.backend.chat.domain.features.message.application.dto.ChannelMessageHistoryResult;
import team.carrypigeon.backend.chat.domain.features.message.application.dto.ChannelMessageResult;
import team.carrypigeon.backend.chat.domain.features.message.application.dto.ChannelMessageSearchResult;
import team.carrypigeon.backend.chat.domain.features.message.application.query.GetChannelMessageHistoryQuery;
import team.carrypigeon.backend.chat.domain.features.message.application.query.SearchChannelMessagesQuery;
import team.carrypigeon.backend.chat.domain.features.message.application.service.MessageApplicationService;
import team.carrypigeon.backend.chat.domain.features.message.controller.dto.ChannelMessageAttachmentUploadResponse;
import team.carrypigeon.backend.chat.domain.features.message.controller.dto.ChannelMessageHistoryResponse;
import team.carrypigeon.backend.chat.domain.features.message.controller.dto.ChannelMessageResponse;
import team.carrypigeon.backend.chat.domain.features.message.controller.dto.ChannelMessageSearchResponse;
import team.carrypigeon.backend.chat.domain.shared.controller.CPResponse;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;

/**
 * 频道消息 HTTP 入口。
 * 职责：提供频道消息历史/搜索查询与附件上传的协议能力。
 * 边界：只承接协议层请求，不承载消息业务规则。
 */
@Validated
@RestController
@RequestMapping("/api/channels")
public class ChannelMessageController {

    private final MessageApplicationService messageApplicationService;
    private final AuthRequestContext authRequestContext;

    public ChannelMessageController(
            MessageApplicationService messageApplicationService,
            AuthRequestContext authRequestContext
    ) {
        this.messageApplicationService = messageApplicationService;
        this.authRequestContext = authRequestContext;
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
    public CPResponse<ChannelMessageHistoryResponse> getChannelMessages(
            @PathVariable @Positive(message = "channelId must be greater than 0") long channelId,
            @RequestParam(required = false) @Positive(message = "cursor must be greater than 0") Long cursor,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "limit must be between 1 and 100")
            @Max(value = 100, message = "limit must be between 1 and 100") int limit,
            HttpServletRequest request
    ) {
        AuthenticatedPrincipal principal = authRequestContext.requirePrincipal(request);
        ChannelMessageHistoryResult result = messageApplicationService.getChannelMessageHistory(
                new GetChannelMessageHistoryQuery(principal.accountId(), channelId, cursor, limit)
        );
        return CPResponse.success(new ChannelMessageHistoryResponse(
                result.messages().stream().map(this::toResponse).toList(),
                result.nextCursor()
        ));
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
    public CPResponse<ChannelMessageSearchResponse> searchChannelMessages(
            @PathVariable @Positive(message = "channelId must be greater than 0") long channelId,
            @RequestParam String keyword,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "limit must be between 1 and 100")
            @Max(value = 100, message = "limit must be between 1 and 100") int limit,
            HttpServletRequest request
    ) {
        AuthenticatedPrincipal principal = authRequestContext.requirePrincipal(request);
        ChannelMessageSearchResult result = messageApplicationService.searchChannelMessages(
                new SearchChannelMessagesQuery(principal.accountId(), channelId, keyword, limit)
        );
        return CPResponse.success(new ChannelMessageSearchResponse(
                result.messages().stream().map(this::toResponse).toList()
        ));
    }

    /**
     * 上传频道消息附件。
     *
     * @param channelId 频道 ID
     * @param messageType 消息类型，仅允许 file / voice
     * @param file 上传文件
     * @param request 当前 HTTP 请求
     * @return 可继续用于 file / voice 消息发送的附件信息
     */
    @PostMapping(path = "/{channelId}/messages/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CPResponse<ChannelMessageAttachmentUploadResponse> uploadChannelMessageAttachment(
            @PathVariable @Positive(message = "channelId must be greater than 0") long channelId,
            @RequestParam @NotBlank(message = "messageType must not be blank") String messageType,
            @RequestPart("file") MultipartFile file,
            HttpServletRequest request
    ) {
        AuthenticatedPrincipal principal = authRequestContext.requirePrincipal(request);
        ChannelMessageAttachmentUploadResult result = messageApplicationService.uploadChannelMessageAttachment(
                new UploadChannelMessageAttachmentCommand(
                        principal.accountId(),
                        channelId,
                        messageType,
                        resolveFilename(file),
                        file.getContentType(),
                        file.getSize(),
                        openInputStream(file)
                )
        );
        return CPResponse.success(new ChannelMessageAttachmentUploadResponse(
                result.objectKey(),
                result.filename(),
                result.mimeType(),
                result.size()
        ));
    }

    private ChannelMessageResponse toResponse(ChannelMessageResult result) {
        return new ChannelMessageResponse(
                result.messageId(),
                result.serverId(),
                result.conversationId(),
                result.channelId(),
                result.senderId(),
                result.messageType(),
                result.body(),
                result.previewText(),
                result.payload(),
                result.metadata(),
                result.status(),
                result.createdAt()
        );
    }

    private String resolveFilename(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (filename == null || filename.isBlank()) {
            throw ProblemException.validationFailed("filename must not be blank");
        }
        return filename;
    }

    private InputStream openInputStream(MultipartFile file) {
        try {
            return file.getInputStream();
        } catch (IOException exception) {
            throw ProblemException.fail("attachment_read_failed", "failed to read upload content");
        }
    }
}
