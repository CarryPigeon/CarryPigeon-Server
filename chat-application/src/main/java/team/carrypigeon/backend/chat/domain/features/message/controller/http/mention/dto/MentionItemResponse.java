package team.carrypigeon.backend.chat.domain.features.message.controller.http.mention.dto;

import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.swagger.v3.oas.annotations.media.Schema;

import team.carrypigeon.backend.chat.domain.features.message.domain.api.mention.result.MentionResult;

/**
 * 提及列表项响应。
 */
public record MentionItemResponse(
        @Schema(type = "string", description = "提及 ID", example = "723155640365318144")
        @JsonSerialize(using = ToStringSerializer.class) long mentionId,
        @Schema(type = "string", description = "频道 ID", example = "12345")
        @JsonSerialize(using = ToStringSerializer.class) long cid,
        @Schema(type = "string", description = "消息 ID", example = "723155640365318144")
        @JsonSerialize(using = ToStringSerializer.class) long mid,
        @Schema(type = "string", description = "来源用户 ID", example = "67890")
        @JsonSerialize(using = ToStringSerializer.class) long fromUid,
        MentionTargetResponse target,
        @Schema(description = "创建时间", example = "1700000000000")
        long createdAt,
        @Schema(description = "是否已读", example = "false")
        boolean read
) {
    /** 将领域 ID 转为十进制字符串，时间采用 Unix 毫秒。 */
    public static MentionItemResponse from(MentionResult mention) {
        return new MentionItemResponse(
            mention.mentionId(),
            mention.channelId(),
            mention.messageId(),
            mention.fromAccountId(),
            new MentionTargetResponse(mention.targetType(), mention.targetAccountId()),
            mention.createdAt().toEpochMilli(),
            mention.read()
        );
    }

    /**
     * 提及列表项中的目标摘要。
     * 职责：表达当前 mention 指向的目标类型与目标账号。
     */
    public record MentionTargetResponse(
            @Schema(description = "目标类型", example = "user") String type,
            @Schema(type = "string", description = "目标用户 ID", example = "123") @JsonSerialize(using = ToStringSerializer.class) long uid
    ) {
    }
}
