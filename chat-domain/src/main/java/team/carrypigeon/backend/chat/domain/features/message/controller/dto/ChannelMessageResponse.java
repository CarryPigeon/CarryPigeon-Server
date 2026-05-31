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
 * @param mentions 规范化提及列表 JSON
 * @param forwardedFrom 转发来源 JSON
 * @param status 消息状态
 * @param createdAt 创建时间
 * @param editedAt 编辑时间
 * @param editVersion 编辑版本
 */
public record ChannelMessageResponse(
        @Schema(description = "消息 ID", example = "5001")
        String messageId,
        @Schema(description = "产生该消息的服务端稳定 UUID", example = "550e8400-e29b-41d4-a716-446655440000")
        String serverId,
        @Schema(description = "会话 ID", example = "3001")
        String conversationId,
        @Schema(description = "频道 ID", example = "2001")
        String channelId,
        @Schema(description = "发送者账户 ID", example = "1001")
        String senderId,
        @Schema(description = "消息类型", example = "text")
        String messageType,
        @Schema(description = "消息正文；非文本消息时可为空", example = "Hello CarryPigeon")
        String body,
        @Schema(description = "预览文本", example = "Hello CarryPigeon")
        String previewText,
        @Schema(description = "结构化载荷 JSON 字符串", example = "{\"share_key\":\"shr_7001\",\"download_path\":\"api/files/download/shr_7001\"}")
        String payload,
        @Schema(description = "元数据 JSON 字符串", example = "{\"lang\":\"zh-CN\"}")
        String metadata,
        @Schema(description = "规范化提及列表 JSON 字符串", example = "[{\"type\":\"user\",\"uid\":\"1001\"}]")
        String mentions,
        @Schema(description = "转发来源 JSON 字符串", example = "{\"mid\":\"5000\",\"cid\":\"1\"}")
        String forwardedFrom,
        @Schema(description = "消息状态", example = "NORMAL")
        String status,
        @Schema(description = "创建时间", example = "2026-05-13T08:00:00Z")
        Instant createdAt,
        @Schema(description = "编辑时间", example = "2026-05-13T08:05:00Z")
        Instant editedAt,
        @Schema(description = "编辑版本", example = "2")
        long editVersion
) {

    public ChannelMessageResponse(
            String messageId,
            String serverId,
            String conversationId,
            String channelId,
            String senderId,
            String messageType,
            String body,
            String previewText,
            String payload,
            String metadata,
            String status,
            Instant createdAt
    ) {
        this(messageId, serverId, conversationId, channelId, senderId, messageType, body, previewText, payload, metadata, null, null, status, createdAt, null, 1L);
    }
}
