package team.carrypigeon.backend.chat.domain.features.message.domain.api;

import java.io.InputStream;
import team.carrypigeon.backend.chat.domain.features.message.domain.projection.MessageAttachmentUploadResult;

/**
 * 频道消息附件领域 API。
 * 职责：暴露消息附件上传能力。
 * 边界：不暴露 controller 协议、普通文件传输、频道消息发送和对象存储适配细节。
 * 输入：账号、频道、消息类型、文件元数据和内容流。
 * 输出：后续发送消息可引用的附件上传结果投影。
 * 失败语义：频道权限、文件大小、类型、消息类型和存储写入问题由领域问题异常表达。
 * 调用方：先通过本接口上传附件，再通过消息发布 API 引用返回的稳定对象键或 share key。
 */
public interface ChannelMessageAttachmentApi {

    /**
     * 上传频道消息附件。
     * 输入：当前账号、目标频道、消息类型、文件名、MIME 类型、大小和内容流。
     * 输出：包含对象键、share key、文件名、MIME 类型和大小的附件上传投影。
     * 副作用：将附件内容写入消息附件存储位置。
     *
     * @param accountId 上传账号 ID
     * @param channelId 目标频道 ID
     * @param messageType 附件将要关联的消息类型
     * @param filename 原始文件名
     * @param mimeType 文件 MIME 类型
     * @param size 文件大小，单位字节
     * @param content 文件内容输入流，由调用方负责提供可读取流
     * @return 消息附件上传结果投影
     */
    MessageAttachmentUploadResult uploadMessageAttachment(
            long accountId,
            long channelId,
            String messageType,
            String filename,
            String mimeType,
            long size,
            InputStream content
    );
}
