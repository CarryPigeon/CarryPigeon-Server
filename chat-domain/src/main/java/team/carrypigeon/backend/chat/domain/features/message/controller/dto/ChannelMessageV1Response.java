package team.carrypigeon.backend.chat.domain.features.message.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;

/**
 * v1 canonical 消息响应。
 * 边界：只包含统一消息字段，domain 专属字段全部位于 data。
 */
public record ChannelMessageV1Response(
        @Schema(description = "消息 ID", example = "1") String mid,
        @Schema(description = "发送者用户 ID", example = "67890") String uid,
        @Schema(description = "频道 ID", example = "12345") String cid,
        @Schema(description = "消息 domain", example = "Core:Text") String domain,
        @Schema(description = "domain 版本", example = "1.0.0") String domainVersion,
        @Schema(description = "domain 专属 canonical 数据") Map<String, Object> data,
        @Schema(description = "发送时间（epoch 毫秒）", example = "1700000000000") long sendTime,
        @Schema(description = "需要提醒的用户 ID") List<String> mentions,
        @Schema(description = "预览文本", example = "hello") String preview,
        @Schema(description = "消息状态", example = "sent") String status
) {
}
