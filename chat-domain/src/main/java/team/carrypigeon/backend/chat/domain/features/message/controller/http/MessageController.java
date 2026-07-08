package team.carrypigeon.backend.chat.domain.features.message.controller.http;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
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
import team.carrypigeon.backend.chat.domain.shared.domain.auth.AuthenticatedAccount;
import team.carrypigeon.backend.chat.domain.features.message.domain.command.DeleteChannelMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.domain.command.EditChannelMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.domain.command.ForwardChannelMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.domain.projection.ChannelMessageResult;
import team.carrypigeon.backend.chat.domain.features.message.controller.dto.ChannelMessageV1Response;
import team.carrypigeon.backend.chat.domain.features.message.controller.dto.EditChannelMessageRequest;
import team.carrypigeon.backend.chat.domain.features.message.controller.dto.ForwardChannelMessageRequest;
import team.carrypigeon.backend.chat.domain.features.message.controller.support.ChannelMessageV1ResponseMapper;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.ChannelMessageLifecycleApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.ChannelMessagePublishingApi;
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
@Tag(name = "消息资源", description = "按消息 ID 执行编辑、删除与转发操作。")
public class MessageController {

    private final ChannelMessagePublishingApi channelMessagePublishingApi;
    private final ChannelMessageLifecycleApi channelMessageLifecycleApi;
    private final RequestAuthenticationContext authRequestContext;
    private final ChannelMessageV1ResponseMapper responseMapper;

    /**
     * 创建消息资源 HTTP 入口。
     *
     * @param channelMessagePublishingApi 频道消息发布领域 API
     * @param channelMessageLifecycleApi 频道消息生命周期领域 API
     * @param authRequestContext 请求认证上下文
     * @param responseMapper v1 消息响应映射器
     */
    public MessageController(
            ChannelMessagePublishingApi channelMessagePublishingApi,
            ChannelMessageLifecycleApi channelMessageLifecycleApi,
            RequestAuthenticationContext authRequestContext,
            ChannelMessageV1ResponseMapper responseMapper
    ) {
        this.channelMessagePublishingApi = channelMessagePublishingApi;
        this.channelMessageLifecycleApi = channelMessageLifecycleApi;
        this.authRequestContext = authRequestContext;
        this.responseMapper = responseMapper;
    }

    /**
     * 删除指定消息。
     * 副作用：通过消息生命周期领域 API 删除目标消息。
     *
     * @param messageId 目标消息 ID
     * @param request 当前 HTTP 请求
     * @return HTTP 204
     */
    @DeleteMapping("/{messageId}")
    public ResponseEntity<Void> deleteMessage(
            @PathVariable @Positive(message = "messageId must be greater than 0") long messageId,
            HttpServletRequest request
    ) {
        AuthenticatedAccount principal = authRequestContext.requirePrincipal(request);
        channelMessageLifecycleApi.deleteChannelMessage(new DeleteChannelMessageCommand(principal.accountId(), messageId));
        return ResponseEntity.noContent().build();
    }

    /**
     * 转发指定消息到目标频道。
     * 输入：目标频道和可选转发评论。
     * 输出：新创建的转发消息 v1 响应。
     *
     * @param messageId 源消息 ID
     * @param body 转发请求
     * @param request 当前 HTTP 请求
     * @return 转发后创建的消息响应
     */
    @PostMapping("/{messageId}/forward")
    public ChannelMessageV1Response forwardMessage(
            @PathVariable @Positive(message = "messageId must be greater than 0") long messageId,
            @Valid @NotNull(message = "request body must not be null") @RequestBody ForwardChannelMessageRequest body,
            HttpServletRequest request
    ) {
        AuthenticatedAccount principal = authRequestContext.requirePrincipal(request);
        ChannelMessageResult result = channelMessagePublishingApi.forwardChannelMessage(new ForwardChannelMessageCommand(
                principal.accountId(),
                messageId,
                parseTargetChannelId(body.targetCid()),
                body.comment()
        ));
        return responseMapper.toResponse(result);
    }

    /**
     * 编辑指定消息。
     * 输入：消息 domain、版本、正文数据、mention 和期望编辑版本。
     * 输出：编辑后的消息 v1 响应。
     *
     * @param messageId 目标消息 ID
     * @param body 编辑请求
     * @param request 当前 HTTP 请求
     * @return 编辑后的消息响应
     */
    @PatchMapping("/{messageId}")
    public ChannelMessageV1Response editMessage(
            @PathVariable @Positive(message = "messageId must be greater than 0") long messageId,
            @Valid @NotNull(message = "request body must not be null") @RequestBody EditChannelMessageRequest body,
            HttpServletRequest request
    ) {
        AuthenticatedAccount principal = authRequestContext.requirePrincipal(request);
        ChannelMessageResult result = channelMessageLifecycleApi.editChannelMessage(new EditChannelMessageCommand(
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
