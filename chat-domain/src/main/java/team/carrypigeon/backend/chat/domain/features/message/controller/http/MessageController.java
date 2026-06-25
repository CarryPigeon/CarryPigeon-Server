package team.carrypigeon.backend.chat.domain.features.message.controller.http;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Positive;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import team.carrypigeon.backend.chat.domain.shared.controller.support.RequestAuthenticationContext;
import team.carrypigeon.backend.chat.domain.shared.application.auth.AuthenticatedAccount;
import team.carrypigeon.backend.chat.domain.features.message.application.command.DeleteChannelMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.application.command.EditChannelMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.application.command.ForwardChannelMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.application.dto.ChannelMessageResult;
import team.carrypigeon.backend.chat.domain.features.message.controller.dto.ChannelMessageV1Response;
import team.carrypigeon.backend.chat.domain.features.message.controller.dto.EditChannelMessageRequest;
import team.carrypigeon.backend.chat.domain.features.message.controller.dto.ForwardChannelMessageRequest;
import team.carrypigeon.backend.chat.domain.features.message.controller.support.ChannelMessageV1ResponseMapper;
import team.carrypigeon.backend.chat.domain.features.message.application.service.MessageModerationApplicationService;
import team.carrypigeon.backend.chat.domain.features.user.application.service.UserProfileApplicationService;
import team.carrypigeon.backend.infrastructure.basic.json.JsonProvider;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * 消息资源 HTTP 入口。
 * 职责：承接按消息 ID 操作的 v1 资源路径。
 * 边界：当前仅承载硬删除入口，不扩展查询与 WS 协议。
 */
@Validated
@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageModerationApplicationService messageModerationApplicationService;
    private final RequestAuthenticationContext authRequestContext;
    private final ChannelMessageV1ResponseMapper responseMapper;

    public MessageController(
            MessageModerationApplicationService messageModerationApplicationService,
            RequestAuthenticationContext authRequestContext,
            UserProfileApplicationService userProfileApplicationService,
            JsonProvider jsonProvider
    ) {
        this.messageModerationApplicationService = messageModerationApplicationService;
        this.authRequestContext = authRequestContext;
        this.responseMapper = new ChannelMessageV1ResponseMapper(userProfileApplicationService, jsonProvider);
    }

    public MessageController(
            MessageModerationApplicationService messageModerationApplicationService,
            RequestAuthenticationContext authRequestContext
    ) {
        this(
                messageModerationApplicationService,
                authRequestContext,
                null,
                new JsonProvider(new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules())
        );
    }

    @DeleteMapping("/{messageId}")
    public ResponseEntity<Void> deleteMessage(
            @PathVariable @Positive(message = "messageId must be greater than 0") long messageId,
            HttpServletRequest request
    ) {
        AuthenticatedAccount principal = authRequestContext.requirePrincipal(request);
        messageModerationApplicationService.deleteChannelMessage(new DeleteChannelMessageCommand(principal.accountId(), messageId));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{messageId}/forward")
    public ChannelMessageV1Response forwardMessage(
            @PathVariable @Positive(message = "messageId must be greater than 0") long messageId,
            @RequestBody ForwardChannelMessageRequest body,
            HttpServletRequest request
    ) {
        AuthenticatedAccount principal = authRequestContext.requirePrincipal(request);
        ChannelMessageResult result = messageModerationApplicationService.forwardChannelMessage(new ForwardChannelMessageCommand(
                principal.accountId(),
                messageId,
                parseTargetChannelId(body.targetCid()),
                body.comment()
        ));
        return responseMapper.toResponse(result);
    }

    @PatchMapping("/{messageId}")
    public ChannelMessageV1Response editMessage(
            @PathVariable @Positive(message = "messageId must be greater than 0") long messageId,
            @RequestBody EditChannelMessageRequest body,
            HttpServletRequest request
    ) {
        AuthenticatedAccount principal = authRequestContext.requirePrincipal(request);
        ChannelMessageResult result = messageModerationApplicationService.editChannelMessage(new EditChannelMessageCommand(
                principal.accountId(),
                messageId,
                body.domain(),
                body.domainVersion(),
                extractText(body.data()),
                toMentionCommands(body.mentions()),
                body.expectedEditVersion()
        ));
        return responseMapper.toResponse(result);
    }

    private long parseTargetChannelId(String rawValue) {
        try {
            return Long.parseLong(rawValue.trim());
        } catch (RuntimeException exception) {
            throw ProblemException.validationFailed("target_cid must be decimal snowflake string");
        }
    }

    private String extractText(Map<String, Object> data) {
        if (data == null) {
            throw ProblemException.validationFailed("data must not be null");
        }
        Object text = data.get("text");
        if (!(text instanceof String value)) {
            throw ProblemException.validationFailed("text must be provided as string");
        }
        return value;
    }

    private List<EditChannelMessageCommand.MentionTargetCommand> toMentionCommands(
            List<EditChannelMessageRequest.MentionTargetRequest> mentionRequests
    ) {
        if (mentionRequests == null || mentionRequests.isEmpty()) {
            return List.of();
        }
        List<EditChannelMessageCommand.MentionTargetCommand> commands = new ArrayList<>();
        for (EditChannelMessageRequest.MentionTargetRequest mentionRequest : mentionRequests) {
            if (mentionRequest == null || mentionRequest.uid() == null || mentionRequest.uid().isBlank()) {
                throw ProblemException.validationFailed("mentions uid must be decimal snowflake string");
            }
            commands.add(new EditChannelMessageCommand.MentionTargetCommand(
                    mentionRequest.type() == null || mentionRequest.type().isBlank() ? "user" : mentionRequest.type().trim(),
                    parseSnowflake(mentionRequest.uid(), "mentions uid")
            ));
        }
        return commands;
    }

    private long parseSnowflake(String rawValue, String fieldName) {
        try {
            return Long.parseLong(rawValue.trim());
        } catch (RuntimeException exception) {
            throw ProblemException.validationFailed(fieldName + " must be decimal snowflake string");
        }
    }
}
