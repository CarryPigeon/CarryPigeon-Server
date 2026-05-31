package team.carrypigeon.backend.chat.domain.features.file.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * 申请文件上传请求。
 * 职责：承载 `POST /api/files/uploads` 的最小输入。
 */
public record CreateFileUploadRequest(
        @Schema(description = "文件名", example = "image.png")
        @NotBlank(message = "filename must not be blank")
        String filename,
        @Schema(description = "MIME 类型", example = "image/png")
        @NotBlank(message = "mime_type must not be blank")
        String mimeType,
        @Schema(description = "文件大小（字节）", example = "123456")
        @Positive(message = "size_bytes must be greater than 0")
        long sizeBytes,
        @Schema(description = "可选 sha256", example = "")
        String sha256
) {
}
