package team.carrypigeon.backend.chat.domain.features.message.controller.http.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import io.swagger.v3.oas.annotations.media.ArraySchema;

import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.List;
import java.util.Map;

import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.result.ChannelMessageResult;

/**
 * 消息响应。
 * 职责：把领域消息无损转换为统一 HTTP 消息结构。
 * 边界：domain 专属字段位于 data，数值 ID 在 HTTP 响应映射时转成字符串。
 */
public record ChannelMessageResponse(
    @Schema(type = "string") @JsonSerialize(using = ToStringSerializer.class) long mid,
    @Schema(type = "string") @JsonSerialize(using = ToStringSerializer.class) long uid,
    @Schema(type = "string") @JsonSerialize(using = ToStringSerializer.class) long cid,
    String domain,
    Map<String,Object> data,
    String preview,
    @ArraySchema(schema = @Schema(type = "string"))
    @JsonSerialize(contentUsing = ToStringSerializer.class) List<Long> mentions,
    Long sendTime
) {

    /**
     *
     * @param result canonical 消息结果
     * @return v1 消息响应
     */
    public static ChannelMessageResponse from(ChannelMessageResult result) {
        return new ChannelMessageResponse(
            result.messageId(),
            result.senderId(),
            result.channelId(),
            result.domain(),
            result.data(),
            result.preview(),
            result.mentions(),
            result.sendTime().toEpochMilli()
        );
    }
}
