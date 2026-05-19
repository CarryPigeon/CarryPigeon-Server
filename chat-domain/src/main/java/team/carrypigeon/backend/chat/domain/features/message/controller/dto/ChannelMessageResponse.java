package team.carrypigeon.backend.chat.domain.features.message.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/**
 * 频道消息协议响应。
 * 职责：向 HTTP 调用方暴露历史消息的稳定字段。
 * 边界：不承载业务规则与统一响应包装逻辑。
 *
 * @param messageId 消息 ID
 * @param serverId 服务端 ID
 * @param conversationId 会话 ID
 * @param channelId 频道 ID
 * @param senderId 发送者账户 ID
 * @param messageType 消息类型
 * @param body 消息正文主体
 * @param previewText 预览文本
 * @param payload 结构化载荷
 * @param metadata 元数据
 * @param status 消息状态
 * @param createdAt 创建时间
 */
public record ChannelMessageResponse(
        @Schema(description = "消息 ID", example = "5001")
        long messageId,
        @Schema(description = "产生该消息的服务端 ID", example = "carrypigeon-local")
        String serverId,
        @Schema(description = "会话 ID", example = "3001")
        long conversationId,
        @Schema(description = "频道 ID", example = "2001")
        long channelId,
        @Schema(description = "发送者账户 ID", example = "1001")
        long senderId,
        @Schema(description = "消息类型", example = "text")
        String messageType,
        @Schema(description = "消息正文；非文本消息时可为空", example = "Hello CarryPigeon")
        String body,
        @Schema(description = "预览文本", example = "Hello CarryPigeon")
        String previewText,
        @Schema(description = "结构化载荷 JSON 字符串", example = "{\"object_key\":\"attachments/5001\"}")
        String payload,
        @Schema(description = "元数据 JSON 字符串", example = "{\"lang\":\"zh-CN\"}")
        String metadata,
        @Schema(description = "消息状态", example = "NORMAL")
        String status,
        @Schema(description = "创建时间", example = "2026-05-13T08:00:00Z")
        Instant createdAt
) {
}
