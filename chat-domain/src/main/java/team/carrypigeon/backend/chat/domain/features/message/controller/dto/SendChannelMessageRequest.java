package team.carrypigeon.backend.chat.domain.features.message.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;

/**
 * HTTP 发送频道消息请求。
 */
public record SendChannelMessageRequest(
        @Schema(description = "消息 domain", example = "Core:Text")
        @NotBlank(message = "domain must not be blank")
        String domain,
        @Schema(description = "domain 版本", example = "1.0.0")
        @NotBlank(message = "domain_version must not be blank")
        String domainVersion,
        @Schema(description = "结构化数据")
        Map<String, Object> data,
        @Schema(description = "回复锚点 mid", example = "0")
        String replyToMid,
        @Schema(description = "候选提及列表")
        List<EditChannelMessageRequest.MentionTargetRequest> mentions,
        @Schema(description = "客户端本地消息 ID", example = "local-msg-001")
        String clientMessageId
) {
}
