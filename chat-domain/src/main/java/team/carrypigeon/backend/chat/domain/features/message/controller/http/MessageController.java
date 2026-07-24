package team.carrypigeon.backend.chat.domain.features.message.controller.http;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import team.carrypigeon.backend.chat.domain.shared.controller.support.RequestAuthenticationContext;
import team.carrypigeon.backend.chat.domain.shared.domain.auth.AuthenticatedAccount;
import team.carrypigeon.backend.chat.domain.features.message.domain.command.ForwardChannelMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.domain.projection.ChannelMessageResult;
import team.carrypigeon.backend.chat.domain.features.message.controller.dto.ChannelMessageV1Response;
import team.carrypigeon.backend.chat.domain.features.message.controller.dto.ForwardChannelMessageRequest;
import team.carrypigeon.backend.chat.domain.features.message.controller.support.ChannelMessageV1ResponseMapper;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.ChannelMessagePublishingApi;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * 消息资源 HTTP 入口。
 * 职责：承接按消息 ID 操作的 v1 资源路径。
 * 边界：当前只承载转发入口，不扩展查询与 WS 协议。
 */
@Validated
@RestController
@RequestMapping("/api/messages")
@Tag(name = "消息资源", description = "按消息 ID 执行转发操作。")
public class MessageController {

    private final ChannelMessagePublishingApi channelMessagePublishingApi;
    private final RequestAuthenticationContext authRequestContext;
    private final ChannelMessageV1ResponseMapper responseMapper;

    /**
     * 创建消息资源 HTTP 入口。
     *
     * @param channelMessagePublishingApi 频道消息发布领域 API
     * @param authRequestContext 请求认证上下文
     * @param responseMapper v1 消息响应映射器
     */
    public MessageController(
            ChannelMessagePublishingApi channelMessagePublishingApi,
            RequestAuthenticationContext authRequestContext,
            ChannelMessageV1ResponseMapper responseMapper
    ) {
        this.channelMessagePublishingApi = channelMessagePublishingApi;
        this.authRequestContext = authRequestContext;
        this.responseMapper = responseMapper;
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
    public ResponseEntity<ChannelMessageV1Response> forwardMessage(
            @PathVariable @Positive(message = "messageId must be greater than 0") long messageId,
            @Valid @NotNull(message = "request body must not be null") @RequestBody ForwardChannelMessageRequest body,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKeyHeader,
            HttpServletRequest request
    ) {
        AuthenticatedAccount principal = authRequestContext.requirePrincipal(request);
        ChannelMessageResult result = channelMessagePublishingApi.forwardChannelMessage(new ForwardChannelMessageCommand(
                principal.accountId(),
                messageId,
                parseTargetChannelId(body.targetCid()),
                body.comment(),
                parseMergedMessageIds(body.mergedMids()),
                resolveIdempotencyKey(idempotencyKeyHeader, body.idempotencyKey())
        ));
        return ResponseEntity.status(201).body(responseMapper.toResponse(result));
    }

    /**
     * 解析消息转发目标频道 ID。
     * 失败语义：目标频道 ID 不是十进制雪花 ID 时返回协议校验问题。
     *
     * @param rawValue 原始目标频道 ID
     * @return 目标频道 ID
     */
    private long parseTargetChannelId(String rawValue) {
        return parseSnowflake(rawValue, "target_cid");
    }

    private List<Long> parseMergedMessageIds(List<String> messageIds) {
        if (messageIds == null || messageIds.isEmpty()) {
            return List.of();
        }
        List<Long> parsedIds = new ArrayList<>();
        for (String messageId : messageIds) {
            parsedIds.add(parseSnowflake(messageId, "merged_mids"));
        }
        return parsedIds;
    }

    private String resolveIdempotencyKey(String headerValue, String bodyValue) {
        String header = normalizeOptional(headerValue);
        String body = normalizeOptional(bodyValue);
        if (header != null && body != null && !header.equals(body)) {
            throw ProblemException.validationFailed("Idempotency-Key header must match idempotency_key");
        }
        return header == null ? body : header;
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /**
     * 解析 HTTP 入参中的必填雪花 ID。
     * 失败语义：不能解析为十进制数字时返回字段级校验问题。
     *
     * @param rawValue 原始参数
     * @param fieldName 字段显示名
     * @return 雪花 ID
     */
    private long parseSnowflake(String rawValue, String fieldName) {
        try {
            long value = Long.parseLong(rawValue.trim());
            if (value <= 0L) {
                throw new NumberFormatException();
            }
            return value;
        } catch (RuntimeException exception) {
            throw ProblemException.validationFailed(fieldName + " must be decimal snowflake string");
        }
    }
}
