package team.carrypigeon.backend.chat.domain.features.user.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 用户背景图上传响应。
 */
public record UserBackgroundUploadResponse(
        @Schema(description = "背景图下载地址", example = "/api/files/download/share-key")
        String backgroundUrl
) {
}
