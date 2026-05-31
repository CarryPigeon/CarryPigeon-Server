package team.carrypigeon.backend.chat.domain.features.file.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

/**
 * 文件上传申请响应。
 */
public record FileUploadResponse(
        @Schema(description = "文件 ID", example = "1")
        String fileId,
        @Schema(description = "分享键", example = "shr_01H...")
        String shareKey,
        @Schema(description = "上传信息")
        UploadResponse upload
) {
    public record UploadResponse(
            @Schema(description = "上传方法", example = "PUT")
            String method,
            @Schema(description = "上传 URL", example = "/api/files/uploads/shr_01H...")
            String url,
            @Schema(description = "上传附带请求头")
            Map<String, String> headers,
            @Schema(description = "过期时间（epoch 毫秒）", example = "1700000100000")
            long expiresAt
    ) {
    }
}
