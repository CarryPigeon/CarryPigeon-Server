package team.carrypigeon.backend.chat.domain.features.message.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 消息附件上传响应。
 * 职责：返回后续发送附件消息所需的稳定引用和文件元数据。
 * 边界：只描述附件上传成功后的资源结果，不携带过渡成功 envelope。
 */
public record MessageAttachmentUploadResponse(
        @Schema(description = "附件对象键", example = "channels/1/messages/file/accounts/1001/5001-demo.pdf")
        String objectKey,
        @Schema(description = "附件分享键", example = "shr_att_xxx")
        String shareKey,
        @Schema(description = "文件名", example = "demo.pdf")
        String filename,
        @Schema(description = "文件 MIME 类型", example = "application/pdf")
        String mimeType,
        @Schema(description = "文件大小（字节）", example = "123")
        long size
) {
}
