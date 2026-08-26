package team.carrypigeon.backend.chat.domain.features.file.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 用户背景图上传响应。
 */
public record ProfileBackgroundUploadResponse(
        @Schema(description = "背景图下载地址", example = "/api/files/download/share-key") String backgroundUrl
) {
}
