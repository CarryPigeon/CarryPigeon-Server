package team.carrypigeon.backend.chat.domain.features.message.domain.projection;

/**
 * 消息附件上传结果。
 * 职责：返回可用于后续消息发送的稳定附件引用。
 * 边界：只表达附件对象，不承载消息发送结果。
 */
public record MessageAttachmentUploadResult(
        String objectKey,
        String shareKey,
        String filename,
        String mimeType,
        long size
) {
}
