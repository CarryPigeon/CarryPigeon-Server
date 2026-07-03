package team.carrypigeon.backend.chat.domain.features.message.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;

/**
 * HTTP 编辑频道消息请求。
 */
public record EditChannelMessageRequest(
        @Schema(description = "消息 domain", example = "Core:Text")
        @NotBlank(message = "domain must not be blank")
        String domain,
        @Schema(description = "domain 版本", example = "1.0.0")
        @NotBlank(message = "domain_version must not be blank")
        String domainVersion,
        @Schema(description = "结构化数据")
        Map<String, Object> data,
        @Schema(description = "候选提及列表")
        List<MentionTargetRequest> mentions,
        @Schema(description = "期望编辑版本", example = "1")
        Long expectedEditVersion
) {

    /**
     * 编辑消息请求中的提及目标。
     * 职责：承载调用方希望写入消息正文的 mention 目标。
     */
    public record MentionTargetRequest(
            @Schema(description = "提及目标类型", example = "user") String type,
            @Schema(description = "提及目标用户 ID", example = "67890") String uid
    ) {
    }
}
