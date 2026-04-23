package team.carrypigeon.backend.chat.domain.features.message.application.dto;

/**
 * 频道消息附件上传结果。
 * 职责：向协议层返回可继续用于 file / voice 发送链路的稳定附件引用信息。
 * 边界：不承载协议包装逻辑，也不暴露底层对象存储实现细节。
 *
 * @param objectKey 对象存储键
 * @param filename 文件名
 * @param mimeType 内容类型
 * @param size 文件大小（字节）
 */
public record ChannelMessageAttachmentUploadResult(
        String objectKey,
        String filename,
        String mimeType,
        long size
) {
}
