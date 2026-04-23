package team.carrypigeon.backend.chat.domain.features.message.controller.dto;

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
        String objectKey,
        String filename,
        String mimeType,
        long size
) {
}
