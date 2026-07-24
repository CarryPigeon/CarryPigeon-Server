package team.carrypigeon.backend.chat.domain.features.message.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

/**
 * HTTP 发送 canonical 频道消息请求。
 * 边界：mentions 只接受提醒用户 ID；所有 domain 专属字段必须位于 data。
 */
public record SendChannelMessageRequest(
        @Schema(description = "消息 domain", example = "Core:Text")
        @NotBlank(message = "domain must not be blank")
        String domain,
        @Schema(description = "domain 版本", example = "1.0.0")
        @NotBlank(message = "domain_version must not be blank")
        String domainVersion,
        @Schema(description = "domain 专属 canonical 数据")
        @NotNull(message = "data must not be null")
        Map<String, Object> data,
        @Schema(description = "需要提醒的用户 ID 数组")
        List<String> mentions,
        @Schema(description = "客户端本地消息 ID", example = "local-msg-001")
        String clientMessageId
) {
}
