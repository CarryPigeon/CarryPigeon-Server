package team.carrypigeon.backend.chat.domain.features.message.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 频道消息附件上传响应。
 * 职责：向调用方暴露继续发送 file / voice 消息所需的稳定附件字段。
 * 边界：不承载统一响应包装和业务规则。
 *
 * @param objectKey 对象存储键
 * @param filename 文件名
 * @param mimeType 内容类型
 * @param size 文件大小（字节）
 */
public record ChannelMessageAttachmentUploadResponse(
        @Schema(description = "对象存储键；后续发送 file 或 voice 消息时可放入 payload", example = "attachments/2026/05/5001.bin")
        String objectKey,
        @Schema(description = "原始文件名", example = "voice-message.m4a")
        String filename,
        @Schema(description = "MIME 类型；无法识别时可为空", example = "audio/mp4")
        String mimeType,
        @Schema(description = "文件大小（字节）", example = "20480")
        long size
) {
}
