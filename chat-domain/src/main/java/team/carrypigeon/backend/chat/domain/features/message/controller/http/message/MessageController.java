package team.carrypigeon.backend.chat.domain.features.message.controller.http.message;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import team.carrypigeon.backend.chat.domain.config.http.security.CpPrincipal;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.input.ForwardChannelMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.result.ChannelMessageResult;
import team.carrypigeon.backend.chat.domain.features.message.controller.http.message.dto.ForwardMessageResponse;
import team.carrypigeon.backend.chat.domain.features.message.controller.http.message.dto.ForwardChannelMessageRequest;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.ChannelMessageApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.problem.MessageProblem;
import team.carrypigeon.backend.infrastructure.basic.exception.ProblemException;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * 消息资源 HTTP 入口。
 * 职责：承接按消息 ID 操作的 v1 资源路径。
 * 边界：当前只承载转发入口，不扩展查询与 WS 协议。
 */
@Validated
@RestController
@RequestMapping("/api/messages")
@PreAuthorize("isAuthenticated()")
@Tag(name = "消息资源", description = "按消息 ID 执行转发操作。")
public class MessageController {

    private final ChannelMessageApi channelMessageApi;

    /**
     * 创建消息资源 HTTP 入口。
     *
     * @param channelMessageApi 频道消息发布领域 API
     */
    public MessageController(
            ChannelMessageApi channelMessageApi
    ) {
        this.channelMessageApi = channelMessageApi;
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
            throw ProblemException.of(MessageProblem.INVALID_INPUT, "Idempotency-Key header must match idempotency_key");
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
            throw ProblemException.of(MessageProblem.INVALID_INPUT, fieldName + " must be decimal snowflake string");
        }
    }
}
