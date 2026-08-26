package team.carrypigeon.backend.chat.domain.features.message.domain.command;

import java.io.InputStream;

/**
 * 上传频道消息附件命令。
 * 职责：聚合消息附件上传用例所需的账号、频道、消息类型、文件元数据与内容流。
 * 边界：不执行参数校验，不控制输入流生命周期，也不包含 HTTP multipart 或对象存储类型。
 *
 * @param accountId 上传账号 ID
 * @param channelId 目标频道 ID
 * @param messageType 附件将要关联的消息类型
 * @param filename 原始文件名
 * @param mimeType 文件 MIME 类型
 * @param size 文件大小，单位字节
 * @param content 文件内容输入流，由调用方负责提供可读取流
 */
public record UploadMessageAttachmentCommand(
        long accountId,
        long channelId,
        String messageType,
        String filename,
        String mimeType,
        long size,
        InputStream content
) {
}
