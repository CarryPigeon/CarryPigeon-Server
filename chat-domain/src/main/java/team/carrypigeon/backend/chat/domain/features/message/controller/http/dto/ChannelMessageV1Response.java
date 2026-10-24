package team.carrypigeon.backend.chat.domain.features.message.controller.http.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.result.ChannelMessageResult;
import team.carrypigeon.backend.infrastructure.basic.id.IdUtil;

/**
 * v1 canonical 消息响应。
 * 职责：把 canonical 领域消息无损转换为统一 HTTP 消息结构。
 * 边界：只包含统一消息字段，domain 专属字段全部位于 data；不解析或重建 data。
 */
public record ChannelMessageV1Response(
        @Schema(description = "消息 ID", example = "1") String mid,
        @Schema(description = "发送者用户 ID", example = "67890") String uid,
        @Schema(description = "频道 ID", example = "12345") String cid,
        @Schema(description = "消息 domain", example = "Core:Text") String domain,
        @Schema(description = "domain 专属 canonical 数据") Map<String, Object> data,
        @Schema(description = "发送时间（epoch 毫秒）", example = "1700000000000") long sendTime,
        @Schema(description = "需要提醒的用户 ID") List<String> mentions,
        @Schema(description = "预览文本", example = "hello") String preview,
        @Schema(description = "消息状态", example = "sent") String status
) {

    /**
     * 把 canonical 消息结果映射为 v1 响应。
     *
     * @param result canonical 消息结果
     * @return v1 消息响应
     */
    public static ChannelMessageV1Response from(ChannelMessageResult result) {
        return new ChannelMessageV1Response(
                IdUtil.toString(result.messageId()),
                IdUtil.toString(result.senderId()),
                IdUtil.toString(result.channelId()),
                result.domain(),
                result.data(),
                result.sendTime().toEpochMilli(),
                result.mentions().stream().map(IdUtil::toString).toList(),
                result.preview(),
                result.status().name().toLowerCase(Locale.ROOT)
        );
    }
}
