package team.carrypigeon.backend.chat.domain.features.message.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 消息附件上传响应。
 * 职责：兼容当前仍保留的过渡成功 envelope。
 * 边界：只用于附件上传成功协议。
 */
public record MessageAttachmentUploadResponse(
        @Schema(description = "过渡成功码", example = "100")
        int code,
        @Schema(description = "过渡成功消息", example = "success")
        String message,
        Data data
) {

    public record Data(
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
}
