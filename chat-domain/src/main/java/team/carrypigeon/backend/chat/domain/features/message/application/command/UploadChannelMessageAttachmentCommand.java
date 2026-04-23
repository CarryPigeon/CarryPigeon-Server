package team.carrypigeon.backend.chat.domain.features.message.application.command;

import java.io.InputStream;

/**
 * 频道消息附件上传命令。
 * 职责：表达文件/语音消息在发送前所需的最小上传输入。
 * 边界：不暴露 MultipartFile 等协议层类型，只承载应用层所需稳定字段。
 *
 * @param accountId 当前上传账户 ID
 * @param channelId 目标频道 ID
 * @param messageType 消息类型，仅允许 file / voice
 * @param filename 原始文件名
 * @param contentType 内容类型，可为空，留给应用层回退
 * @param size 文件大小（字节）
 * @param content 上传内容流
 */
public record UploadChannelMessageAttachmentCommand(
        long accountId,
        long channelId,
        String messageType,
        String filename,
        String contentType,
        long size,
        InputStream content
) {
}
